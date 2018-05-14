/*
   Copyright 2017-2018 Nationale-Nederlanden

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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Selects an exitState, based on the content-type of a httpServletRequest
 * object as input.
 */

public class IfMultipart extends AbstractPipe {
	private String thenForwardName = "then";
	private String elseForwardName = "else";

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
			throws PipeRunException {
		String forward;
		PipeForward pipeForward = null;

		if (input == null) {
			forward = elseForwardName;
		} else {
			if (!(input instanceof HttpServletRequest)) {
				throw new PipeRunException(this,
						getLogPrefix(null)
								+ "expected HttpServletRequest as input, got ["
								+ ClassUtils.nameOf(input) + "]");
			}

			HttpServletRequest request = (HttpServletRequest) input;
			String contentType = request.getContentType();
			if (StringUtils.isNotEmpty(contentType) && contentType.startsWith("multipart")) {
				forward = thenForwardName;
			} else {
				forward = elseForwardName;
			}
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
}
