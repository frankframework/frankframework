/*
   Copyright 2013 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.pipes;

import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.task.TimeoutGuard;

/**
 * Extension to FixedForwardPipe for interrupting processing when timeout is exceeded.
 *
 * @ff.parameter timeout When a parameter with name timeout is present, it is used instead of the timeout specified by the attribute
 *
 * @author Peter Leeuwenburgh
 */
@Deprecated
public abstract class TimeoutGuardPipe extends FixedForwardPipe {

	private boolean throwException = true;
	private int timeout = 30;

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		ParameterValueList pvl;
		try {
			pvl = getParameterList().getValues(message, session);
		} catch (ParameterException e) {
			throw new PipeRunException(this, "exception on extracting parameters", e);
		}
		String paramValue = getParameterValue(pvl, "timeout");
		int guardTimeout = paramValue == null ? getTimeout() : Integer.valueOf(paramValue);
		log.debug("setting timeout of [{}] s", guardTimeout);

		TimeoutGuard tg = new TimeoutGuard(guardTimeout, getName()) {
			@Override
			protected void abort() {
				// The guard automatically kills the current thread, additional threads maybe 'killed' by implementing killPipe.
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
				return new PipeRunResult(getSuccessForward(), errorMessage);
			}
		} finally {
			if(tg.cancel()) {
				// Throw a TimeOutException
				String msgString = "TimeOutException";
				Exception e = new TimeoutException("exceeds timeout of [" + guardTimeout + "] s, interupting");
				if (isThrowException()) {
					throw new PipeRunException(this, msgString, e);
				} else {
					// This is used for the old console, where a message is displayed
					log.error(msgString, e);
					String msgCdataString = "<![CDATA[" + msgString + ": "+ e.getMessage() + "]]>";
					Message errorMessage = new Message("<error>" + msgCdataString + "</error>");
					return new PipeRunResult(getSuccessForward(), errorMessage);
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

	/**
	 * If {@code true}, a piperunexception is thrown. otherwise the output is only logged as an error (and returned in a xml string with 'error' tags)
	 * @ff.default true
	 */
	public void setThrowException(boolean b) {
		throwException = b;
	}

	public boolean isThrowException() {
		return throwException;
	}

	public int getTimeout() {
		return timeout;
	}

	/**
	 * timeout in seconds of obtaining a result
	 * @ff.default 30
	 */
	public void setTimeout(int i) {
		timeout = i;
	}
}
