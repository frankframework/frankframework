/*
   Copyright 2018 Nationale-Nederlanden

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


import nl.nn.adapterframework.core.AllowAllSecurityHandler;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;

/**
 * Selects an exitState, based on the input equals an AllowAllSecurityHandler object.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.XmlIf</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setThenForwardName(String) thenForwardName}</td><td>forward returned when 'true'</code></td><td>then</td></tr>
 * <tr><td>{@link #setElseForwardName(String) elseForwardName}</td><td>forward returned when 'false'</td><td>else</td></tr>
 * </table>
 * </p>
 *
 * @author  Peter Leeuwenburgh
 */

public class IsAllSecurityHandlerAllowed extends AbstractPipe {

	private String thenForwardName = "then";
	private String elseForwardName = "else";


	@Override
	public PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {
		String forward = "";
		PipeForward pipeForward = null;

		if (message.asObject() instanceof AllowAllSecurityHandler) {
			forward = thenForwardName;
		} else {
			forward = elseForwardName;
		}

		log.debug(
				getLogPrefix(session) + "determined forward [" + forward + "]");

		pipeForward = findForward(forward);

		if (pipeForward == null) {
			throw new PipeRunException(this, getLogPrefix(null)
					+ "cannot find forward or pipe named [" + forward + "]");
		}
		log.debug(getLogPrefix(session) + "resolved forward [" + forward
				+ "] to path [" + pipeForward.getPath() + "]");
		return new PipeRunResult(pipeForward, message);
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
}
