/*
   Copyright 2018 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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
package nl.nn.adapterframework.senders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Guard;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.SaxDocumentBuilder;
import nl.nn.adapterframework.xml.SaxElementBuilder;

/**
 * Collection of Senders, that are executed all at the same time. Once the results are processed, all results will be sent to the resultSender,
 * while the original sender will return it's result to the pipeline.
 *
 * <p>Multiple sub-senders can be configured within the ShadowSender, the minimum amount of senders is 2 (originalSender + resultSender)</p>

 * @author  Niels Meijer
 * @since   7.0
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
		if(StringUtils.isEmpty(originalSenderName)) {
			originalSenderName = sender.getName();
		}
		if (!senderIt.hasNext()) {
			throw new ConfigurationException("ShadowSender should contain at least 2 Senders, only one found");
		}
		if(StringUtils.isEmpty(resultSenderName)) {
			while(senderIt.hasNext()) {
				sender = senderIt.next();
			}
			resultSenderName = sender.getName();
		}

		secondarySenders = validateExecutableSenders();

		if(originalSender == null)
			throw new ConfigurationException("no originalSender found");
		if(resultSender == null)
			throw new ConfigurationException("no resultSender found");

		super.configure();
	}

	public List<ISender> validateExecutableSenders() throws ConfigurationException {
		List<ISender> secondarySenderList = new ArrayList<>();
		for (ISender sender: getSenders()) {
			if(originalSenderName.equalsIgnoreCase(sender.getName())) {
				if(originalSender != null) {
					throw new ConfigurationException("originalSender can only be defined once");
				}
				originalSender = sender;
			}
			else if(resultSenderName.equalsIgnoreCase(sender.getName())) {
				if(resultSender != null) {
					throw new ConfigurationException("resultSender can only be defined once");
				}
				resultSender = sender;
			}
			else { // ShadowSender
				secondarySenderList.add(sender);
			}
		}

		return secondarySenderList;
	}

	protected void executeGuarded(ISender sender, Message message, PipeLineSession session, Guard guard, Map<ISender, ParallelSenderExecutor> executorMap) {
		guard.addResource();
		ParallelSenderExecutor pse = new ParallelSenderExecutor(sender, message, session, guard, getStatisticsKeeper(sender));
		executorMap.put(sender, pse);
		getExecutor().execute(pse);

	}
	/**
	 * We override this from the parallel sender as we should only execute the original and shadowsenders here!
	 */
	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		Guard primaryGuard = new Guard();
		Guard shadowGuard = new Guard();
		Map<ISender, ParallelSenderExecutor> executorMap = new ConcurrentHashMap<>();

		executeGuarded(originalSender, message, session, primaryGuard, executorMap);
		// Loop through all senders and execute the message.
		for (ISender sender : getSecondarySenders()) {
			executeGuarded(sender, message, session, shadowGuard, executorMap);
		}

		// Wait till primary sender has replied.
		try {
			primaryGuard.waitForAllResources();
		} catch (InterruptedException e) {
			throw new SenderException(getLogPrefix()+"was interupted", e);
		}

		/*
		 * setup action to
		 * - wait for remaining senders to have replied
		 * - collect the results of all senders
		 */
		Runnable collectResults = () -> {
			// Wait till every sender has replied.
			try {
				shadowGuard.waitForAllResources();
				// Collect the results of the (Shadow)Sender and send them to the resultSender.
				try {
					Message result = collectResults(executorMap, message, session);
					resultSender.sendMessageOrThrow(result, session); // Can not close() the message, since results are used later.
				} catch (IOException | SAXException e) {
					log.error("unable to compose result message", e);
				} catch (TimeoutException | SenderException se) {
					log.error("failed to send ShadowSender result to [{}]", resultSender::getName);
				}
			} catch (InterruptedException e) {
				log.warn("{} result collection thread was interrupted", getLogPrefix(), e);
			}
		};

		if (isWaitForShadowsToFinish()) {
			collectResults.run();
		} else {
			getExecutor().execute(collectResults);
		}

		ParallelSenderExecutor originalSenderExecutor = executorMap.get(originalSender);
		if(originalSenderExecutor == null) {
			throw new IllegalStateException("unable to find originalSenderExecutor");
		}
		// If the originalSender contains any exceptions these should be thrown and
		// cause an SenderException regardless of the results of the ShadowSenders.
		if (originalSenderExecutor.getThrowable() != null) {
			throw new SenderException(originalSenderExecutor.getThrowable());
		}
		return originalSenderExecutor.getReply();
	}

	private Message collectResults(Map<ISender, ParallelSenderExecutor> executorMap, Message message, PipeLineSession session) throws SAXException, IOException {
		SaxDocumentBuilder builder = new SaxDocumentBuilder("results");

		String correlationID = session==null ? null : session.getCorrelationId();
		builder.addAttribute("correlationID", correlationID);

		builder.addElement("originalMessage", XmlUtils.skipXmlDeclaration(message.asString()));

		addResult(builder, originalSender, executorMap, "originalResult");

		for (ISender sender : getSecondarySenders()) {
			addResult(builder, sender, executorMap, "shadowResult");
		}

		builder.close();

		return Message.asMessage(builder.toString());
	}

	protected void addResult(SaxDocumentBuilder builder, ISender sender, Map<ISender, ParallelSenderExecutor> executorMap, String tagName) throws SAXException, IOException {
		try (SaxElementBuilder resultXml = builder.startElement(tagName)) {
			ParallelSenderExecutor pse = executorMap.get(sender);

			StatisticsKeeper sk = getStatisticsKeeper(sender);
			resultXml.addAttribute("duration", ""+sk.getLast());

			resultXml.addAttribute("senderClass", ClassUtils.nameOf(sender));
			resultXml.addAttribute("senderName", sender.getName());
			Throwable throwable = pse.getThrowable();
			if (throwable==null) {
				SenderResult senderResult = pse.getReply();
				resultXml.addAttribute("success", Boolean.toString(senderResult.isSuccess()));
				if (senderResult.getForwardName()!=null) {
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
	 * @ff.default the first sender specified
	 */
	public void setOriginalSender(String senderName) {
		this.originalSenderName = senderName;
	}

	/**
	 * The sender name which will process the results
	 * @ff.default the last sender specified
	 */
	public void setResultSender(String senderName) {
		this.resultSenderName = senderName;
	}

	/**
	 * If set <code>true</code> the sender will wait for all shadows to have finished. Otherwise the collection of results will happen in a background thread.
	 * @ff.default false
	 */
	public void setWaitForShadowsToFinish(boolean waitForShadowsToFinish) {
		this.waitForShadowsToFinish = waitForShadowsToFinish;
	}
}
