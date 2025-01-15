/*
   Copyright 2018 Nationale-Nederlanden, 2020, 2022, 2024 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.senders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Phaser;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.XmlUtils;
import org.frankframework.xml.SaxDocumentBuilder;
import org.frankframework.xml.SaxElementBuilder;

/**
 * Collection of Senders, that are executed all at the same time. Once the results are processed, all results will be sent to the resultSender,
 * while the original sender will return its result to the pipeline.
 *
 * <p>Multiple sub-senders can be configured within the ShadowSender, the minimum amount of senders is 2 (originalSender + resultSender)</p>
 *
 * @author Niels Meijer
 * @since 7.0
 */
public class ShadowSender extends ParallelSenders {

	private @Getter String originalSenderName = null;
	private ISender originalSender = null;
	private @Getter String resultSenderName = null;
	private ISender resultSender = null;
	private @Getter List<ISender> secondarySenders;
	private @Getter boolean waitForShadowsToFinish = false;

	@Override
	public void configure() throws ConfigurationException {
		Iterator<ISender> senderIt = getSenders().iterator();
		if (!senderIt.hasNext()) {
			throw new ConfigurationException("ShadowSender should contain at least 2 Senders, none found");
		}
		ISender sender = senderIt.next();
		if (StringUtils.isEmpty(originalSenderName)) {
			originalSenderName = sender.getName();
		}
		if (!senderIt.hasNext()) {
			throw new ConfigurationException("ShadowSender should contain at least 2 Senders, only one found");
		}
		if (StringUtils.isEmpty(resultSenderName)) {
			while (senderIt.hasNext()) {
				sender = senderIt.next();
			}
			resultSenderName = sender.getName();
		}

		secondarySenders = validateExecutableSenders();

		if (originalSender == null)
			throw new ConfigurationException("no originalSender found");
		if (resultSender == null)
			throw new ConfigurationException("no resultSender found");

		super.configure();
	}

	public List<ISender> validateExecutableSenders() throws ConfigurationException {
		List<ISender> secondarySenderList = new ArrayList<>();
		for (ISender sender : getSenders()) {
			if (originalSenderName.equalsIgnoreCase(sender.getName())) {
				if (originalSender != null) {
					throw new ConfigurationException("originalSender can only be defined once");
				}
				originalSender = sender;
			} else if (resultSenderName.equalsIgnoreCase(sender.getName())) {
				if (resultSender != null) {
					throw new ConfigurationException("resultSender can only be defined once");
				}
				resultSender = sender;
			} else { // ShadowSender
				secondarySenderList.add(sender);
			}
		}

		return secondarySenderList;
	}

	protected void executeGuarded(ISender sender, Message message, PipeLineSession session, Phaser guard, Map<ISender, ParallelSenderExecutor> executorMap) throws SenderException {
		Message messageToSend;
		try {
			messageToSend = isWaitForShadowsToFinish() ? message : message.copyMessage();
		} catch (IOException e) {
			if (guard != null) guard.arrive(); // Sign off the guard, to prevent deadlocks
			throw new SenderException("Cannot create copy of message", e);
		}
		ParallelSenderExecutor pse = new ParallelSenderExecutor(sender, messageToSend, session, getStatisticsKeeper(sender));
		pse.setGuard(guard);
		executorMap.put(sender, pse);
		getExecutor().execute(pse);
	}

	/**
	 * Override this from the parallel sender as it should only execute the original and shadowsenders here!
	 */
	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		try {
			if (!message.isRepeatable()) {
				message.preserve();
			}
		} catch (IOException e) {
			throw new SenderException("could not preserve input message", e);
		}

		Phaser primaryGuard = new Phaser(2); // Itself and the added originalSender
		Phaser shadowGuard = new Phaser(getSecondarySenders().size() + 1); // Itself and all secondary senders
		Map<ISender, ParallelSenderExecutor> executorMap = new ConcurrentHashMap<>();

		executeGuarded(originalSender, message, session, primaryGuard, executorMap);
		// Loop through all senders and execute the message.
		for (ISender sender : getSecondarySenders()) {
			executeGuarded(sender, message, session, shadowGuard, executorMap);
		}

		// Wait till primary sender has replied.
		log.debug("waiting for primary senders to finish. Left: {}", primaryGuard.getUnarrivedParties() - 1);
		primaryGuard.arriveAndAwaitAdvance();


		 // Wait for remaining senders to have replied & collect the results of all senders
		Message originalMessage;
		try {
			originalMessage = isWaitForShadowsToFinish() ? message : message.copyMessage();
		} catch (IOException e) {
			throw new SenderException("Cannot copy input message", e);
		}
		String correlationId = session.getCorrelationId();
		Runnable collectResults = () -> {
			// Wait till every sender has replied.
			log.debug("waiting for shadow senders to finish. Left: {}", shadowGuard.getUnarrivedParties() - 1);
			shadowGuard.arriveAndAwaitAdvance();

			// Collect the results of the (Shadow)Sender and send them to the resultSender.
			try {
				Message result = collectResults(executorMap, originalMessage, correlationId);
				resultSender.sendMessageOrThrow(result, session); // Can not close() the message, since results are used later.
			} catch (IOException | SAXException e) {
				log.error("unable to compose result message", e);
			} catch (TimeoutException | SenderException se) {
				log.error("failed to send ShadowSender result to [{}]", resultSender::getName);
			}
		};

		if (isWaitForShadowsToFinish()) {
			collectResults.run();
		} else {
			getExecutor().execute(collectResults);
		}

		ParallelSenderExecutor originalSenderExecutor = executorMap.get(originalSender);
		if (originalSenderExecutor == null) {
			throw new IllegalStateException("unable to find originalSenderExecutor");
		}
		// If the originalSender contains any exceptions these should be thrown and
		// cause an SenderException regardless of the results of the ShadowSenders.
		if (originalSenderExecutor.getThrowable() != null) {
			throw new SenderException(originalSenderExecutor.getThrowable());
		}
		return originalSenderExecutor.getReply();
	}

	private Message collectResults(Map<ISender, ParallelSenderExecutor> executorMap, Message originalMessage, String correlationID) throws SAXException, IOException {
		MessageBuilder messageBuilder = new MessageBuilder();
		try (SaxDocumentBuilder builder = new SaxDocumentBuilder("results", messageBuilder.asXmlWriter(), true)) {
			builder.addAttribute("correlationID", correlationID);
			builder.addElement("originalMessage", XmlUtils.skipXmlDeclaration(originalMessage.asString()));
			addResult(builder, originalSender, executorMap, "originalResult");

			for (ISender sender : getSecondarySenders()) {
				addResult(builder, sender, executorMap, "shadowResult");
			}
		}

		return messageBuilder.build();
	}

	protected void addResult(SaxDocumentBuilder builder, ISender sender, Map<ISender, ParallelSenderExecutor> executorMap, String tagName) throws SAXException, IOException {
		try (SaxElementBuilder resultXml = builder.startElement(tagName)) {
			ParallelSenderExecutor pse = executorMap.get(sender);

			resultXml.addAttribute("duration", ""+pse.getDuration());

			resultXml.addAttribute("senderClass", ClassUtils.nameOf(sender));
			resultXml.addAttribute("senderName", sender.getName());
			Throwable throwable = pse.getThrowable();
			if (throwable == null) {
				SenderResult senderResult = pse.getReply();
				resultXml.addAttribute("success", Boolean.toString(senderResult.isSuccess()));
				if (senderResult.getForwardName() != null) {
					resultXml.addAttribute("forwardName", senderResult.getForwardName());
				}
				Message result = senderResult.getResult();
				resultXml.addValue(XmlUtils.skipXmlDeclaration(result.asString()));
			} else {
				resultXml.addAttribute("success", "false");
				resultXml.addValue(throwable.getMessage());
			}
		}
	}

	/**
	 * Name of the sender that is considered that is considered to be the golden standard, i.e. the source of truth.
	 *
	 * @ff.default the first sender specified
	 */
	public void setOriginalSender(String senderName) {
		this.originalSenderName = senderName;
	}

	/**
	 * The sender name which will process the results
	 *
	 * @ff.default the last sender specified
	 */
	public void setResultSender(String senderName) {
		this.resultSenderName = senderName;
	}

	/**
	 * If set <code>true</code> the sender will wait for all shadows to have finished. Otherwise the collection of results will happen in a background thread.
	 *
	 * @ff.default false
	 */
	public void setWaitForShadowsToFinish(boolean waitForShadowsToFinish) {
		this.waitForShadowsToFinish = waitForShadowsToFinish;
	}
}
