/*
   Copyright 2015 Nationale-Nederlanden

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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.NDC;

import nl.nn.adapterframework.parameters.ParameterResolutionContext;

/**
 * Extension to SenderWithParametersBase for interrupting processing when
 * timeout is exceeded.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setThrowException(boolean) throwException}</td><td>when <code>true</code>, a SenderException (or TimeOutException) is thrown. Otherwise the output is only logged as an error (and returned in a XML string with 'error' tags)</td><td>true</td></tr>
 * <tr><td>{@link #setXmlTag(String) xmlTag}</td><td>when not empty, the xml tag to encapsulate the result in</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author Peter Leeuwenburgh
 */
public class TimeoutGuardSenderWithParametersBase extends
		SenderWithParametersBase {

	private boolean throwException = true;
	private int tymeout = 30;
	private String xmlTag;

	public class SendMessage implements Callable<String> {
		private String correlationID;
		private String message;
		private ParameterResolutionContext prc;
		private String threadName;
		private String threadNDC;

		public SendMessage(String correlationID, String message,
				ParameterResolutionContext prc, String threadName,
				String threadNDC) {
			this.correlationID = correlationID;
			this.message = message;
			this.prc = prc;
			this.threadName = threadName;
			this.threadNDC = threadNDC;
		}

		public String call() throws Exception {
			String ctName = Thread.currentThread().getName();
			try {
				Thread.currentThread().setName(threadName + "[" + ctName + "]");
				NDC.push(threadNDC);
				return sendMessageWithTimeoutGuarded(correlationID, message, prc);
			} finally {
				Thread.currentThread().setName(ctName);
			}
		}
	}

	public String sendMessage(String correlationID, String message,
			ParameterResolutionContext prc) throws SenderException,
			TimeOutException {
		SendMessage sendMessage = new SendMessage(correlationID, message, prc,
				Thread.currentThread().getName(), NDC.peek());
		ExecutorService service = Executors.newSingleThreadExecutor();
		Future<String> future = service.submit(sendMessage);
		String result = null;
		try {
			log.debug(getLogPrefix() + "setting timeout of ["
					+ retrieveTymeout() + "] s");
			result = (String) future.get(retrieveTymeout(), TimeUnit.SECONDS);
			if (StringUtils.isNotEmpty(getXmlTag())) {
				result = "<" + getXmlTag() + "><![CDATA[" + result + "]]></"
						+ getXmlTag() + ">";
			}
		} catch (Exception e) {
			boolean timedOut = false;
			Throwable t = e.getCause();
			String msg;
			if (e instanceof TimeoutException) {
				String errorMsg = getLogPrefix() + "exceeds timeout of ["
						+ retrieveTymeout() + "] s, interupting";
				future.cancel(true);
				msg = (t != null ? t.getClass().getName() : e.getClass()
						.getName()) + ": " + errorMsg;
				timedOut = true;
			} else {
				msg = (t != null ? t.getClass().getName() : e.getClass()
						.getName());
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

	public String sendMessageWithTimeoutGuarded(String correlationID,
			String message, ParameterResolutionContext prc)
			throws SenderException, TimeOutException {
		return null;
	}

	public void setThrowException(boolean b) {
		throwException = b;
	}

	public boolean isThrowException() {
		return throwException;
	}

	/**
	 * In the subclass overwrite the retrieveTymeout method with the (already
	 * existing) timeout attribute:
	 * 
	 * public int retrieveTymeout() { return getTimeout() / 1000; }
	 */

	public int retrieveTymeout() {
		return tymeout;
	}

	public void setXmlTag(String string) {
		xmlTag = string;
	}

	public String getXmlTag() {
		return xmlTag;
	}
}
