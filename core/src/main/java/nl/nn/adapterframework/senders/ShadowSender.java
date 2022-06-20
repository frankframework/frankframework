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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.SAXException;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Guard;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.SaxDocumentBuilder;
import nl.nn.adapterframework.xml.SaxElementBuilder;

/**
 * Collection of Senders, that are executed all at the same time. Once the results are processed, all results will be sent to the resultSender, while the original sender will return it's result to the pipeline.
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
	private @Getter List<ISender> executableSenders;

	@Override
	public void configure() throws ConfigurationException {
		if(originalSenderName == null || resultSenderName == null) {
			throw new ConfigurationException("no originalSender or resultSender defined");
		}

		executableSenders = validateExecutableSenders();

		if(originalSender == null)
			throw new ConfigurationException("no originalSender found");
		if(resultSender == null)
			throw new ConfigurationException("no resultSender found");

		super.configure();
	}

	public List<ISender> validateExecutableSenders() throws ConfigurationException {
		List<ISender> executableSenderList = new ArrayList<>();
		for (ISender sender: getSenders()) {
			if(originalSenderName.equalsIgnoreCase(sender.getName())) {
				if(originalSender != null) {
					throw new ConfigurationException("originalSender can only be defined once");
				}
				originalSender = sender;
				executableSenderList.add(sender);
			}
			else if(resultSenderName.equalsIgnoreCase(sender.getName())) {
				if(resultSender != null) {
					throw new ConfigurationException("resultSender can only be defined once");
				}
				resultSender = sender;
			}
			else { // ShadowSender
				executableSenderList.add(sender);
			}
		}

		return executableSenderList;
	}

	/**
	 * We override this from the parallel sender as we should only execute the original and shadowsenders here!
	 */
	@Override
	public Message sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		Guard guard = new Guard();
		Map<ISender, ParallelSenderExecutor> executorMap = new HashMap<>();

		// Loop through all senders and execute the message.
		for (ISender sender : getExecutableSenders()) {
			guard.addResource();
			ParallelSenderExecutor pse = new ParallelSenderExecutor(sender, message, session, guard, getStatisticsKeeper(sender));
			executorMap.put(sender, pse);

			getExecutor().execute(pse);
		}

		// Wait till every sender has replied.
		try {
			guard.waitForAllResources();
		} catch (InterruptedException e) {
			throw new SenderException(getLogPrefix()+"was interupted", e);
		}

		ParallelSenderExecutor originalSenderExecutor = executorMap.get(originalSender);
		if(originalSenderExecutor == null) {
			throw new IllegalStateException("unable to find originalSenderExecutor");
		}

		// Collect the results of the (Shadow)Sender and send them to the resultSender.
		try {
			Message result = collectResults(executorMap, message, session);
			resultSender.sendMessage(result, session);
		} catch (IOException | SAXException e) {
			log.error("unable to compose result message", e);
		} catch(TimeoutException | SenderException se) {
			log.error("failed to send ShadowSender result to ["+resultSender.getName()+"]");
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

		String correlationID = session==null ? null : session.getMessageId();
		builder.addAttribute("correlationID", correlationID);

		builder.addElement("originalMessage", XmlUtils.skipXmlDeclaration(message.asString()));

		for (ISender sender : getExecutableSenders()) {
			ParallelSenderExecutor pse = executorMap.get(sender);

			SaxElementBuilder senderResult;
			if(sender == originalSender) {
				senderResult = builder.startElement("originalResult");
			} else {
				senderResult = builder.startElement("shadowResult");
			}

			StatisticsKeeper sk = getStatisticsKeeper(sender);
			senderResult.addAttribute("duration", ""+sk.getLast());

			senderResult.addAttribute("senderClass", ClassUtils.nameOf(sender));
			senderResult.addAttribute("senderName", sender.getName());
			Throwable throwable = pse.getThrowable();
			if (throwable==null) {
				Message result = pse.getReply();
				senderResult.addValue(XmlUtils.skipXmlDeclaration(result.asString()));
			} else {
				senderResult.addValue(throwable.getMessage());
			}
			senderResult.endElement();
		}

		builder.close();

		return Message.asMessage(builder.toString());
	}

	/** The default or original sender name */
	public void setOriginalSender(String senderName) {
		this.originalSenderName = senderName;
	}

	/** The sender name which will process the results */
	public void setResultSender(String senderName) {
		this.resultSenderName = senderName;
	}
}
