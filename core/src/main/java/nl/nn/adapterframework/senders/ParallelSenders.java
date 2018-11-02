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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Guard;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Collection of Senders, that are executed all at the same time.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.senders.ParallelSenders</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setGetInputFromFixedValue(String) getInputFromFixedValue}</td><td>when set, this fixed value is taken as input, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPreserveInput(boolean) preserveInput}</td><td>when set <code>true</code>, the input of a pipe is restored before processing the next one</td><td>false</td></tr>
 * <tr><td>{@link #setMaxConcurrentThreads(int) maxThreads}</td><td>sets and upper limit to the amount of concurrent threads that can be run simultaneously. Use 0 to disable.</td><td>0</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ISender sender}</td><td>one or more specifications of senders. Each will receive the same input message, to be processed in parallel</td></tr>
 * </table>
 * </p>

 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class ParallelSenders extends SenderSeries {

	private int maxConcurrentThreads = 0;

	@Override
	public String doSendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
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
			
			ParameterResolutionContext newPrc = new ParameterResolutionContext(
					prc.getInput(), prc.getSession());
			ParallelSenderExecutor pse = new ParallelSenderExecutor(sender,
					correlationID, message, newPrc, guard,
					getStatisticsKeeper(sender));
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
				Object result = pse.getReply();
				if (result==null) {
					resultXml.addAttribute("type", "null");
				} else {
					resultXml.addAttribute("type", ClassUtils.nameOf(result));
					resultXml.setValue(XmlUtils.skipXmlDeclaration(result.toString()),false);
				}
			} else {
				resultXml.addAttribute("type", ClassUtils.nameOf(throwable));
				resultXml.setValue(throwable.getMessage());
			}
			resultsXml.addSubElement(resultXml); 
		}
		return resultsXml.toXML();
	}

	@Override
	public void setSynchronous(boolean value) {
		if (!isSynchronous()) {
			super.setSynchronous(value); 
		} 
	}

	protected TaskExecutor createTaskExecutor() {
		ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) getConfiguration().getIbisManager().getIbisContext().getBean("concurrentTaskExecutor");
		executor.setCorePoolSize(getMaxConcurrentThreads());
		return executor;
	}

	public void setMaxConcurrentThreads(int maxThreads) {
		if(maxThreads < 1)
			maxThreads = 0;

		this.maxConcurrentThreads = maxThreads;
	}
	public int getMaxConcurrentThreads() {
		return maxConcurrentThreads;
	}
}
