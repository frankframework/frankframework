/*
   Copyright 2018 Nationale-Nederlanden, 2020 WeAreFrank!

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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.core.task.TaskExecutor;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Guard;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Collection of Senders, that are executed all at the same time. Once the results are processed, all results will be sent to the resultSender, while the original sender will return it's result to the pipeline.
 * 
 * <p>Multiple sub-senders can be configured within the ShadowSender, the minimum amount of senders is 2 (originalSender + resultSender)</p>

 * @author  Niels Meijer
 * @since   7.0
 */
public class ShadowSender extends ParallelSenders {

	private String originalSender = null;
	private String resultSender = null;
	private ISender resultISender = null;
	private List<ISender> senderList = null;

	@Override
	public void configure() throws ConfigurationException {
		boolean hasShadowSender = false;
		boolean hasResultSender = false;
		boolean hasOriginalSender = false;

		if(originalSender == null)
			throw new ConfigurationException("no originalSender defined");
		if(resultSender == null)
			throw new ConfigurationException("no resultSender defined");

		for (ISender sender: getSenders()) {
			if(sender.getName() != null && sender.getName().equalsIgnoreCase(getOriginalSender())) {
				if(hasOriginalSender)
					throw new ConfigurationException("originalSender can only be defined once");
				hasOriginalSender = true;
			}
			else if(sender.getName() != null && sender.getName().equalsIgnoreCase(getResultSender())) {
				if(hasResultSender)
					throw new ConfigurationException("resultSender can only be defined once");
				hasResultSender = true;
				resultISender = sender;
			}
			else
				hasShadowSender = true;
		}

		if(!hasOriginalSender)
			throw new ConfigurationException("no originalSender found");
		if(!hasResultSender)
			throw new ConfigurationException("no resultSender found");
		if(!hasShadowSender)
			throw new ConfigurationException("no shadowSender found");

		if(getSenderList().size()==0)
			throw new ConfigurationException("this is not supposed to happen, like ever");

		super.configure();
	}

	/**
	 * We override this from the parallel sender as we should only execute the original and shadowsenders here!
	 */
	@Override
	public Message doSendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		Guard guard = new Guard();
		Map<ISender, ParallelSenderExecutor> executorMap = new HashMap<ISender, ParallelSenderExecutor>();
		TaskExecutor executor = createTaskExecutor();

		// Create a new ParameterResolutionContext to be thread safe, see
		// documentation on constructor of ParameterResolutionContext
		// (parameter cacheXmlSource).
		// Testing also showed that disabling caching is better for
		// performance. At least when testing with a lot of large messages
		// in parallel. This might be due to the fact that objects can be
		// garbage collected earlier. OutOfMemoryErrors occur much
		// faster when caching is enabled. Testing was done by sending 10
		// messages of 1 MB concurrently to a pipeline which will process
		// the message in parallel with 10 SenderWrappers (containing a
		// XsltSender and IbisLocalSender).
		for (Iterator<ISender> it = getExecutableSenders(); it.hasNext();) {
			ISender sender = it.next();
			guard.addResource();
			ParallelSenderExecutor pse = new ParallelSenderExecutor(sender, message, session, guard, getStatisticsKeeper(sender));
			executorMap.put(sender, pse);
			executor.execute(pse);
		}
		try {
			guard.waitForAllResources();
		} catch (InterruptedException e) {
			throw new SenderException(getLogPrefix()+"was interupted",e);
		}

		ParallelSenderExecutor originalSender = null;
		XmlBuilder resultsXml = new XmlBuilder("results");
		String correlationID = session==null ? null : session.getMessageId();
		resultsXml.addAttribute("correlationID", correlationID);

		XmlBuilder originalMessageXml = new XmlBuilder("originalMessage");
		try {
			originalMessageXml.setValue(XmlUtils.skipXmlDeclaration(message.asString()),false);
		} catch (IOException e) {
			throw new SenderException(getLogPrefix(),e);
		}
		resultsXml.addSubElement(originalMessageXml);

		// First loop through all (Shadow)Senders and handle their results
		for (Iterator<ISender> it = getExecutableSenders(); it.hasNext();) {
			ISender sender = it.next();
			ParallelSenderExecutor pse = executorMap.get(sender);

			XmlBuilder resultXml;
			if(sender.getName() != null && sender.getName().equalsIgnoreCase(getOriginalSender())) {
				originalSender = pse;
				resultXml = new XmlBuilder("originalResult");
			}
			else {
				resultXml = new XmlBuilder("shadowResult");
			}

			StatisticsKeeper sk = getStatisticsKeeper(sender);
			resultXml.addAttribute("duration", sk.getLast() + sk.getUnits());
			resultXml.addAttribute("count", sk.getCount());

			resultXml.addAttribute("senderClass", ClassUtils.nameOf(sender));
			resultXml.addAttribute("senderName", sender.getName());
			Throwable throwable = pse.getThrowable();
			if (throwable==null) {
				Object result = pse.getReply();
				if (result==null) {
					resultXml.addAttribute("type", "null");
				} else {
					resultXml.addAttribute("type", ClassUtils.nameOf(result));
					resultXml.setValue(XmlUtils.skipXmlDeclaration(result.toString()), false);
				}
			} else {
				resultXml.addAttribute("type", ClassUtils.nameOf(throwable));
				resultXml.setValue(throwable.getMessage());
			}
			resultsXml.addSubElement(resultXml); 
		}

		// If the originalSender contains any exceptions these should be thrown and
		// cause an SenderException regardless of the results of the ShadowSenders.
		if(originalSender == null) {
			//In theory this should never happen!
			throw new SenderException("no originalSender found");
		}

		//The messages have been processed, now the results need to be stored somewhere.
		try {
			resultISender.sendMessage(new Message(resultsXml.toXML()), session);
		} catch(SenderException se) {
			log.warn("failed to send ShadowSender result to ["+resultISender.getName()+"]");
		}

		if (originalSender.getThrowable() != null) {
			throw new SenderException(originalSender.getThrowable());
		}
		return originalSender.getReply();
	}

	protected Iterator<ISender> getExecutableSenders() {
		return getSenderList().iterator();
	}
	private List<ISender> getSenderList() {
		if(senderList == null) {
			senderList = new ArrayList<ISender>();
			for (ISender sender: getSenders()) {
				if(sender.getName() == null || (!sender.getName().equals(getResultSender())))
					senderList.add(sender);
			}
		}
		return senderList;
	}


	@IbisDoc({"the default or original sender", ""})
	public void setOriginalSender(String sender) {
		this.originalSender = sender;
	}
	public String getOriginalSender() {
		return originalSender;
	}

	@IbisDoc({"the sender which will process all results", ""})
	public void setResultSender(String sender) {
		this.resultSender = sender;
	}
	public String getResultSender() {
		return resultSender;
	}
}
