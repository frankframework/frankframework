/*
   Copyright 2016 Nationale-Nederlanden

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

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import org.apache.commons.lang.StringUtils;

/**
 * Selects an exitState, based on if the input is a XML string.
 * 
 * The input is a XML string if it, after removing leading white-space characters, starts with '<'.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.IsXmlIfPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setThenForwardName(String) thenForwardName}</td><td>forward returned when 'true'</code></td><td>then</td></tr>
 * <tr><td>{@link #setElseForwardName(String) elseForwardName}</td><td>forward returned when 'false'</td><td>else</td></tr>
 * <tr><td>{@link #setElseForwardOnEmptyInput(boolean) elseForwardOnEmptyInput}</td><td>return elseForward when input is empty (or thenForward)</td><td>true</td></tr>
 * </table>
 * </p>
 *
 * @author  Peter Leeuwenburgh
 * @since   4.3
 */

public class IsXmlIfPipe extends AbstractPipe {

	private String thenForwardName = "then";
	private String elseForwardName = "else";
	private boolean elseForwardOnEmptyInput = true;

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
			throws PipeRunException {
		String forward = "";
		if (input==null) {
			if (isElseForwardOnEmptyInput()) {
				forward = elseForwardName;
			} else {
				forward = thenForwardName;
			}
		} else {
			String sInput = input.toString();
			if (StringUtils.isEmpty(sInput)) {
				if (isElseForwardOnEmptyInput()) {
					forward = elseForwardName;
				} else {
					forward = thenForwardName;
				}
			} else {
				String firstChar = sInput.replaceAll("^\\s+", "").substring(0, 1);
				if (firstChar.equals("<")) {
					forward = thenForwardName;
				} else {
					forward = elseForwardName;
				}
			}
		}

		log.debug(getLogPrefix(session) + "determined forward [" + forward
				+ "]");

		PipeForward pipeForward = findForward(forward);

		if (pipeForward == null) {
			throw new PipeRunException(this, getLogPrefix(null)
					+ "cannot find forward or pipe named [" + forward + "]");
		}
		log.debug(getLogPrefix(session) + "resolved forward [" + forward
				+ "] to path [" + pipeForward.getPath() + "]");
		return new PipeRunResult(pipeForward, input);
	}

	public void setThenForwardName(String thenForwardName) {
		this.thenForwardName = thenForwardName;
	}

	public String getThenForwardName() {
		return thenForwardName;
	}

	public void setElseForwardName(String elseForwardName) {
		this.elseForwardName = elseForwardName;
	}

	public String getElseForwardName() {
		return elseForwardName;
	}

	public boolean isElseForwardOnEmptyInput() {
		return elseForwardOnEmptyInput;
	}

	public void setElseForwardOnEmptyInput(boolean b) {
		elseForwardOnEmptyInput = b;
	}
}
