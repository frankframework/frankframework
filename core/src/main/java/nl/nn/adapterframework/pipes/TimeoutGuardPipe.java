/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.task.TimeoutGuard;

/**
 * Extension to FixedForwardPipe for interrupting processing when timeout is exceeded.
 * 
 * <p>
 * <table border="1">
 * <b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>timeout</td><td>int</td><td>When a parameter with name timeout is present, it is used instead of the timeout specified by the attribute</td></tr>
 * </table>
 * </p>
 * 
 * @author Peter Leeuwenburgh
 */
public abstract class TimeoutGuardPipe extends FixedForwardPipe {

	private boolean throwException = true;
	private int timeout = 30;

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		ParameterValueList pvl = null;
		if (getParameterList() != null) {
			try {
				pvl = getParameterList().getValues(message, session);
			} catch (ParameterException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "exception on extracting parameters", e);
			}
		}
		int timeout_work;
		String timeout_work_str = getParameterValue(pvl, "timeout");
		if (timeout_work_str == null) {
			timeout_work = getTimeout();
		} else {
			timeout_work = Integer.valueOf(timeout_work_str);
		}

		log.debug(getLogPrefix(session) + "setting timeout of [" + timeout_work + "] s");
		TimeoutGuard tg = new TimeoutGuard(timeout_work, getName()) {
			@Override
			protected void abort() {
				//The guard automatically kills the current thread, additional threads maybe 'killed' by implementing killPipe.
				killPipe();
			}
		};

		try {
			return doPipeWithTimeoutGuarded(message, session);
		} catch (Exception e) {
			String msg = e.getClass().getName();

			if (isThrowException()) {
				throw new PipeRunException(this, msg, e);
			} else {
				String msgString = msg + ": " + e.getMessage();
				log.error(msgString, e);
				String msgCdataString = "<![CDATA[" + msgString + "]]>";
				Message errorMessage = new Message("<error>" + msgCdataString + "</error>");
				return new PipeRunResult(getForward(), errorMessage);
			}
		} finally {
			if(tg.cancel()) {
				//Throw a TimeOutException
				String msgString = "TimeOutException";
				Exception e = new TimeOutException("exceeds timeout of [" + timeout_work + "] s, interupting");
				if (isThrowException()) {
					throw new PipeRunException(this, msgString, e);
				} else {
					//This is used for the old console, where a message is displayed
					log.error(msgString, e);
					String msgCdataString = "<![CDATA[" + msgString + ": "+ e.getMessage() + "]]>";
					Message errorMessage = new Message("<error>" + msgCdataString + "</error>");
					return new PipeRunResult(getForward(), errorMessage);
				}
			}
		}
	}

	/**
	 * doPipe wrapped around a TimeoutGuard
	 */
	public abstract PipeRunResult doPipeWithTimeoutGuarded(Message input, PipeLineSession session) throws PipeRunException;

	/**
	 * optional implementation to kill additional threads if the pipe may have created those.
	 */
	protected void killPipe() {
		//kill other threads
	}

	@IbisDoc({"when <code>true</code>, a piperunexception is thrown. otherwise the output is only logged as an error (and returned in a xml string with 'error' tags)", "true"})
	public void setThrowException(boolean b) {
		throwException = b;
	}

	public boolean isThrowException() {
		return throwException;
	}

	public int getTimeout() {
		return timeout;
	}

	@IbisDoc({"timeout in seconds of obtaining a result", "30"})
	public void setTimeout(int i) {
		timeout = i;
	}
}