/*
   Copyright 2013 Nationale-Nederlanden

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

import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDescription; 
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.NDC;

import nl.nn.adapterframework.parameters.ParameterResolutionContext;


/** 
 * @author Peter Leeuwenburgh
 */
@IbisDescription(
	"Extension to SenderWithParametersBase for interrupting processing when \n" + 
	"timeout is exceeded. \n" 
)
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

		@Override
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

	@Override
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		SendMessage sendMessage = new SendMessage(correlationID, message, prc,
				Thread.currentThread().getName(), NDC.peek());
		ExecutorService service = Executors.newSingleThreadExecutor();
		Future<String> future = service.submit(sendMessage);
		String result = null;
		try {
			log.debug(getLogPrefix() + "setting timeout of ["
					+ retrieveTymeout() + "] s");
			result = future.get(retrieveTymeout(), TimeUnit.SECONDS);
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

	public String sendMessageWithTimeoutGuarded(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		return null;
	}

	@IbisDoc({"when <code>true</code>, a senderexception (or timeoutexception) is thrown. otherwise the output is only logged as an error (and returned in a xml string with 'error' tags)", "true"})
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

	@IbisDoc({"when not empty, the xml tag to encapsulate the result in", ""})
	public void setXmlTag(String string) {
		xmlTag = string;
	}

	public String getXmlTag() {
		return xmlTag;
	}
}
