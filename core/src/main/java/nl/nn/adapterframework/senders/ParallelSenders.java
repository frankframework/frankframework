/*
   Copyright 2013, 2017-2018 Nationale-Nederlanden

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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Guard;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Collection of Senders, that are executed all at the same time.
 * 
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link ISender sender}</td><td>one or more specifications of senders. Each will receive the same input message, to be processed in parallel</td></tr>
 * </table>
 * </p>

 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class ParallelSenders extends SenderSeries implements ApplicationContextAware {

	private int maxConcurrentThreads = 0;
	private ApplicationContext applicationContext;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (getParameterList()!=null && getParameterList().size()>0) {
			String paramList=getParameterList().get(0).getName();
			for (int i=1;i<getParameterList().size();i++) {
				paramList+=", "+getParameterList().get(i).getName();
			}
			ConfigurationWarnings.add(this, log, "parameters ["+paramList+"] of ParallelSenders ["+getName()+"] are not available for use by nested Senders");
		}
	}

	@Override
	public Message sendMessage(Message message, IPipeLineSession session) throws SenderException, TimeOutException {
		Guard guard = new Guard();
		Map<ISender, ParallelSenderExecutor> executorMap = new HashMap<ISender, ParallelSenderExecutor>();
		TaskExecutor executor = createTaskExecutor();

		for (Iterator<ISender> it = getSenderIterator(); it.hasNext();) {
			ISender sender = it.next();
			guard.addResource();
			// Create a new ParameterResolutionContext to be thread safe, see
			// documentation on constructor of ParameterResolutionContext
			// (parameter singleThreadOnly).
			// Testing also showed that disabling caching is better for
			// performance. At least when testing with a lot of large messages
			// in parallel. This might be due to the fact that objects can be
			// garbage collected earlier. OutOfMemoryErrors occur much
			// faster when caching is enabled. Testing was done by sending 10
			// messages of 1 MB concurrently to a pipeline which will process
			// the message in parallel with 10 SenderWrappers (containing a
			// XsltSender and IbisLocalSender).
			
			ParallelSenderExecutor pse = new ParallelSenderExecutor(sender, message, session, guard, getStatisticsKeeper(sender));
			executorMap.put(sender, pse);

			executor.execute(pse);
		}
		try {
			guard.waitForAllResources();
		} catch (InterruptedException e) {
			throw new SenderException(getLogPrefix()+"was interupted",e);
		}

		XmlBuilder resultsXml = new XmlBuilder("results");
		for (Iterator<ISender> it = getSenderIterator(); it.hasNext();) {
			ISender sender = it.next();
			ParallelSenderExecutor pse = executorMap.get(sender);
			XmlBuilder resultXml = new XmlBuilder("result");
			resultXml.addAttribute("senderClass", ClassUtils.nameOf(sender));
			resultXml.addAttribute("senderName", sender.getName());
			Throwable throwable = pse.getThrowable();
			if (throwable==null) {
				Message result = pse.getReply();
				if (result==null) {
					resultXml.addAttribute("type", "null");
				} else {
					try {
						resultXml.addAttribute("type", ClassUtils.nameOf(result.asObject()));
						resultXml.setValue(XmlUtils.skipXmlDeclaration(result.asString()),false);
					} catch (IOException e) {
						throw new SenderException(getLogPrefix(),e);
					}
				}
			} else {
				resultXml.addAttribute("type", ClassUtils.nameOf(throwable));
				resultXml.setValue(throwable.getMessage());
			}
			resultsXml.addSubElement(resultXml); 
		}
		return new Message(resultsXml.toXML());
	}

	@Override
	public void setSynchronous(boolean value) {
		if (!isSynchronous()) {
			super.setSynchronous(value); 
		} 
	}

	protected TaskExecutor createTaskExecutor() {
		ThreadPoolTaskExecutor executor = applicationContext.getBean("concurrentTaskExecutor", ThreadPoolTaskExecutor.class);
		executor.setCorePoolSize(getMaxConcurrentThreads());
		return executor;
	}

	@IbisDoc({"Set the upper limit to the amount of concurrent threads that can be run simultaneously. Use 0 to disable.", "0"})
	public void setMaxConcurrentThreads(int maxThreads) {
		if(maxThreads < 1)
			maxThreads = 0;

		this.maxConcurrentThreads = maxThreads;
	}
	public int getMaxConcurrentThreads() {
		return maxConcurrentThreads;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
