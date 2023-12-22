/*
   Copyright 2013, 2017-2018 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.util.Guard;
import org.frankframework.util.XmlBuilder;
import org.frankframework.util.XmlUtils;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.ConcurrencyThrottleSupport;

import lombok.Getter;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.Category;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.SpringUtils;

/**
 * Collection of Senders, that are executed all at the same time.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
@Category("Advanced")
public class ParallelSenders extends SenderSeries {

	private @Getter int maxConcurrentThreads = 0;
	private @Getter TaskExecutor executor;

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
		executor = createTaskExecutor();
	}

	@Override
	public SenderResult doSendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		Guard guard = new Guard();
		Map<ISender, ParallelSenderExecutor> executorMap = new LinkedHashMap<>();
		boolean success=true;
		String errorMessage=null;

		for (ISender sender: getSenders()) {
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
		for (ISender sender: getSenders()) {
			ParallelSenderExecutor pse = executorMap.get(sender);
			XmlBuilder resultXml = new XmlBuilder("result");
			resultXml.addAttribute("senderClass", org.springframework.util.ClassUtils.getUserClass(sender).getSimpleName());
			resultXml.addAttribute("senderName", sender.getName());
			Throwable throwable = pse.getThrowable();
			if (throwable==null) {
				SenderResult senderResult = pse.getReply();
				success &= senderResult.isSuccess();
				resultXml.addAttribute("success", senderResult.isSuccess());
				if (senderResult.getForwardName()!=null) {
					resultXml.addAttribute("forwardName", senderResult.getForwardName());
				}
				if (StringUtils.isNotEmpty(senderResult.getErrorMessage())) {
					resultXml.addAttribute("errorMessage", senderResult.getErrorMessage());
					if (errorMessage==null) {
						errorMessage=senderResult.getErrorMessage();
					}
				}
				Message result = senderResult.getResult();
				if (result==null) {
					resultXml.addAttribute("type", "null");
				} else {
					try {
						resultXml.addAttribute("type", result.getRequestClass());
						resultXml.setValue(XmlUtils.skipXmlDeclaration(result.asString()),false);
					} catch (IOException e) {
						throw new SenderException(getLogPrefix(),e);
					}
				}
			} else {
				success=false;
				resultXml.addAttribute("type", ClassUtils.nameOf(throwable));
				resultXml.addAttribute("success", false);
				resultXml.setValue(throwable.getMessage());
			}
			resultsXml.addSubElement(resultXml);
		}
		return new SenderResult(success, new Message(resultsXml.toXML()), errorMessage, null);
	}

	@Override
	public void setSynchronous(boolean value) {
		if (!isSynchronous()) {
			super.setSynchronous(value);
		}
	}

	protected TaskExecutor createTaskExecutor() {
		SimpleAsyncTaskExecutor executor = SpringUtils.createBean(getApplicationContext(), SimpleAsyncTaskExecutor.class);

		if(getMaxConcurrentThreads() > 0) { //ConcurrencyLimit defaults to NONE so only this technically limits it!
			executor.setConcurrencyLimit(getMaxConcurrentThreads());
		} else {
			executor.setConcurrencyLimit(ConcurrencyThrottleSupport.UNBOUNDED_CONCURRENCY);
		}

		return executor;
	}

	/** one or more specifications of senders. Each will receive the same input message, to be processed in parallel */
	@Override
	public void registerSender(ISender sender) {
		super.registerSender(sender);
	}

	/**
	 * Set the upper limit to the amount of concurrent threads that can be run simultaneously. Use 0 to disable.
	 * @ff.default 0
	 */
	public void setMaxConcurrentThreads(int maxThreads) {
		if(maxThreads < 1)
			maxThreads = 0;

		this.maxConcurrentThreads = maxThreads;
	}
}
