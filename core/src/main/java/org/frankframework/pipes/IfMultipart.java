/*
   Copyright 2017-2018, 2020 Nationale-Nederlanden

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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.ElementType;
import org.frankframework.doc.ElementType.ElementTypes;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;

/**
 * Selects an exitState, based on the content-type of a httpServletRequest object as input.
 * 
 * @deprecated The below code block may be used to replace this pipe
 * <pre><code>
 * &lt;XmlIfPipe name="xyz" xpathExpression="contains($my-param, 'multipart')"&gt;
 * 	&lt;param name="my-param" sessionKey="originalMessage" contextKey="Header.content-type" /&gt;
 * 	&lt;forward name="true" path="a" /&gt;
 * 	&lt;forward name="false" path="b" /&gt;
 * &lt;/XmlIfPipe&gt;
 * </code></pre>
 */
@ElementType(ElementTypes.ROUTER)
@Deprecated(forRemoval = true, since = "8.0")
@ConfigurationWarning("The content-type header can be retreived from the MessageContext")
public class IfMultipart extends AbstractPipe {
	private String thenForwardName = "then";
	private String elseForwardName = "else";

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String forward;
		PipeForward pipeForward = null;

		if (message == null || message.asObject() == null) {
			forward = elseForwardName;
		} else {
			if (!(message.asObject() instanceof HttpServletRequest)) {
				throw new PipeRunException(this, "expected HttpServletRequest as input, got [" + ClassUtils.nameOf(message) + "]");
			}

			HttpServletRequest request = (HttpServletRequest) message.asObject();
			String contentType = request.getContentType();
			if (StringUtils.isNotEmpty(contentType) && contentType.startsWith("multipart")) {
				forward = thenForwardName;
			} else {
				forward = elseForwardName;
			}
		}

		log.debug("determined forward [{}]", forward);

		pipeForward = findForward(forward);

		if (pipeForward == null) {
			throw new PipeRunException(this, "cannot find forward or pipe named [" + forward + "]");
		}
		log.debug("resolved forward [{}] to path [{}]", forward, pipeForward.getPath());
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
