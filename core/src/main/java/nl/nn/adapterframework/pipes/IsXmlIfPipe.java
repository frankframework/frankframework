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

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDescription; 
import org.apache.commons.lang.StringUtils;


/** 
 * @author  Peter Leeuwenburgh
 * @since   4.3
 */
@IbisDescription(
	"Selects an exitState, based on if the input is a XML string." + 
	"The input is a XML string if it, after removing leading white-space characters, starts with '<'." 
)

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

	@IbisDoc({"forward returned when 'true'</code>", "then"})
	public void setThenForwardName(String thenForwardName) {
		this.thenForwardName = thenForwardName;
	}

	public String getThenForwardName() {
		return thenForwardName;
	}

	@IbisDoc({"forward returned when 'false'", "else"})
	public void setElseForwardName(String elseForwardName) {
		this.elseForwardName = elseForwardName;
	}

	public String getElseForwardName() {
		return elseForwardName;
	}

	public boolean isElseForwardOnEmptyInput() {
		return elseForwardOnEmptyInput;
	}

	@IbisDoc({"return elseforward when input is empty (or thenforward)", "true"})
	public void setElseForwardOnEmptyInput(boolean b) {
		elseForwardOnEmptyInput = b;
	}
}
