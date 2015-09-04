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
package nl.nn.adapterframework.pipes;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.NDC;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;

/**
 * Extension to FixedForwardPipe for interrupting processing when timeout is exceeded.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setThrowException(boolean) throwException}</td><td>when <code>true</code>, a PipeRunException is thrown. Otherwise the output is only logged as an error (and returned in a XML string with 'error' tags)</td><td>true</td></tr>
 * <tr><td>{@link #setTimeout(int) timeout}</td><td>timeout in seconds of obtaining a result</td><td>30</td></tr>
 * </table>
 * </p>
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

public class TimeoutGuardPipe extends FixedForwardPipe {

	private boolean throwException = true;
	private int timeout = 30;

	public class DoPipe implements Callable<String> {
		private Object input;
		private IPipeLineSession session;
		private String threadName;
		private String threadNDC;

		public DoPipe(Object input, IPipeLineSession session, String threadName, String threadNDC) {
			this.input = input;
			this.session = session;
			this.threadName = threadName;
			this.threadNDC = threadNDC;
		}

		public String call() throws Exception {
			String ctName = Thread.currentThread().getName();
			Thread.currentThread().setName(threadName+"["+ctName+"]");
			NDC.push(threadNDC);
			return doPipeWithTimeoutGuarded(input, session);
		}
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
			throws PipeRunException {
		ParameterValueList pvl = null;
		if (getParameterList() != null) {
			ParameterResolutionContext prc = new ParameterResolutionContext(
					(String) input, session);
			try {
				pvl = prc.getValues(getParameterList());
			} catch (ParameterException e) {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "exception on extracting parameters", e);
			}
		}
		int timeout_work;
		String timeout_work_str = getParameterValue(pvl, "timeout");
		if (timeout_work_str == null) {
			timeout_work = getTimeout();
		} else {
			timeout_work = Integer.valueOf(timeout_work_str);
		}

		DoPipe doPipe = new DoPipe(input, session, Thread.currentThread().getName(), NDC.peek());
		ExecutorService service = Executors.newSingleThreadExecutor();
		Future future = service.submit(doPipe);
		String result = null;
		try {
			log.debug(getLogPrefix(session) + "setting timeout of ["
					+ timeout_work + "] s");
			result = (String) future.get(timeout_work, TimeUnit.SECONDS);
		} catch (Exception e) {
			String msg;
			if (e instanceof TimeoutException) {
				String errorMsg = getLogPrefix(session)
						+ "exceeds timeout of [" + timeout_work
						+ "] s, interupting";
				future.cancel(true);
				msg = e.getClass().getName() + ": " + errorMsg;
			} else {
				msg = e.getClass().getName();
			}

			if (isThrowException()) {
				throw new PipeRunException(this, msg, e);
			} else {
				String msgString = msg + ": " + e.getMessage();
				log.error(msgString);
				String msgCdataString = "<![CDATA[" + msgString + "]]>";
				result = "<error>" + msgCdataString + "</error>";
			}
		} finally {
			service.shutdown();
		}
		return new PipeRunResult(getForward(), result);
	}

	public String doPipeWithTimeoutGuarded(Object input,
			IPipeLineSession session) throws PipeRunException {
		return input.toString();
	}

	private String getParameterValue(ParameterValueList pvl,
			String parameterName) {
		ParameterList parameterList = getParameterList();
		if (pvl != null && parameterList != null) {
			for (int i = 0; i < parameterList.size(); i++) {
				Parameter parameter = parameterList.getParameter(i);
				if (parameter.getName().equalsIgnoreCase(parameterName)) {
					return pvl.getParameterValue(i).asStringValue(null);
				}
			}
		}
		return null;
	}

	public void setThrowException(boolean b) {
		throwException = b;
	}

	public boolean isThrowException() {
		return throwException;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int i) {
		timeout = i;
	}
}