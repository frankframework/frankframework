/*
   Copyright 2015, 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.stream.IThreadCreator;
import nl.nn.adapterframework.stream.ThreadConnector;
import nl.nn.adapterframework.stream.ThreadLifeCycleEventListener;

/**
 * Extension to SenderWithParametersBase for interrupting processing when
 * timeout is exceeded.
 * 
 * 
 * @author Peter Leeuwenburgh
 */
public abstract class TimeoutGuardSenderWithParametersBase extends SenderWithParametersBase implements IThreadCreator  {

	private boolean throwException = true;
	private int tymeout = 30;
	private String xmlTag;
	protected ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener;

	public class TaskWrapper<V> implements IAbortableTask<V>{
		private V message;
		private IAbortableTask<V> task;
		private ThreadConnector threadConnector;

		public TaskWrapper(Object owner, String correlationID, V message, IAbortableTask<V> task) {
			this.message=message;
			this.task = task;
			threadConnector=new ThreadConnector(owner, threadLifeCycleEventListener, correlationID);
		}

		@Override
		public V call() throws Exception {
			try {
				threadConnector.startThread(message);
				V response=task.call();
				threadConnector.endThread(response);
				return response;
			} catch (Exception e) {
				throw (Exception)threadConnector.abortThread(e);
			}
		}

		@Override
		public void abort() {
			task.abort();
		}
	}

	@Override
	// we cannot make this method final, createSendMessageTask is not a good candidate to use for debugging.
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		TaskWrapper<String> task = new TaskWrapper<>(this, correlationID, message, createSendMessageTask(correlationID, message, prc));
		ExecutorService service = Executors.newSingleThreadExecutor();
		Future<String> future = service.submit(task);
		String result = null;
		try {
			log.debug(getLogPrefix() + "setting timeout of [" + retrieveTymeout() + "] s");
			result = future.get(retrieveTymeout(), TimeUnit.SECONDS);
			if (StringUtils.isNotEmpty(getXmlTag())) {
				result = "<" + getXmlTag() + "><![CDATA[" + result + "]]></" + getXmlTag() + ">";
			}
		} catch (Exception e) {
			boolean timedOut = false;
			Throwable t = e.getCause();
			String msg;
			if (e instanceof TimeoutException) {
				String errorMsg = getLogPrefix() + "exceeds timeout of [" + retrieveTymeout() + "] s, interupting";
				task.abort();
				if (!future.isDone()) {
					future.cancel(true);
				}
				msg = (t != null ? t.getClass().getName() : e.getClass().getName()) + ": " + errorMsg;
				timedOut = true;
			} else {
				msg = (t != null ? t.getClass().getName() : e.getClass().getName());
				if (t != null && t instanceof TimeOutException) {
					timedOut = true;
				}
			}

			if (isThrowException()) {
				if (timedOut) {
					throw new TimeOutException(msg, (t != null ? t : e));
				} else {
					throw new SenderException(msg, (t != null ? t : e));
				}
			} else {
				String msgString = msg + ": " + e.getMessage();
				log.error(msgString);
				String msgCdataString = "<![CDATA[" + msgString + "]]>";
				result = "<error>" + msgCdataString + "</error>";
			}
		} finally {
			service.shutdown();
		}
		return result;
	}

	public abstract IAbortableTask<String> createSendMessageTask(String correlationID, String message, ParameterResolutionContext prc) throws SenderException;

	/**
	 * In the subclass overwrite the retrieveTymeout method with the (already
	 * existing) timeout attribute:
	 * 
	 * public int retrieveTymeout() { return getTimeout() / 1000; }
	 */
	public int retrieveTymeout() {
		return tymeout;
	}

	@IbisDoc({"when <code>true</code>, a senderexception (or timeoutexception) is thrown. otherwise the output is only logged as an error (and returned in a xml string with 'error' tags)", "true"})
	public void setThrowException(boolean b) {
		throwException = b;
	}
	public boolean isThrowException() {
		return throwException;
	}

	@IbisDoc({"when not empty, the xml tag to encapsulate the result in", ""})
	public void setXmlTag(String string) {
		xmlTag = string;
	}
	public String getXmlTag() {
		return xmlTag;
	}
	
	@Override
	public void setThreadLifeCycleEventListener(ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener) {
		this.threadLifeCycleEventListener=threadLifeCycleEventListener;
	}

}
