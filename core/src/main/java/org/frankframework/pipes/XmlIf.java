/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021-2024 WeAreFrank!

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

import java.io.IOException;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.EnterpriseIntegrationPattern.Type;
import org.frankframework.doc.Forward;
import org.frankframework.parameters.ParameterList;
import org.frankframework.stream.Message;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.TransformerPool.OutputType;
import org.frankframework.util.XmlEncodingUtils;
import org.frankframework.util.XmlUtils;

/**
 * Selects a forward, based on XPath evaluation
 *
 * @author Peter Leeuwenburgh
 * @since 4.3
 */
@Forward(name = "*", description = "when {@literal thenForwardName} or {@literal elseForwardName} are used")
@Forward(name = "then", description = "the configured condition is met")
@Forward(name = "else", description = "the configured condition is not met")
@EnterpriseIntegrationPattern(Type.ROUTER)
public class XmlIf extends AbstractPipe {

	private @Getter String namespaceDefs = null;
	private @Getter String sessionKey = null;
	private @Getter String xpathExpression = null;
	private @Getter String expressionValue = null;
	private @Getter String thenForwardName = "then";
	private @Getter String elseForwardName = "else";
	private @Getter String regex = null;
	private @Getter int xsltVersion = XmlUtils.DEFAULT_XSLT_VERSION;
	private @Getter boolean namespaceAware = XmlUtils.isNamespaceAwareByDefault();

	private TransformerPool tp = null;

	protected String makeStylesheet(String xpathExpression, String expressionValue) {
		String namespaceClause = XmlUtils.getNamespaceClause(getNamespaceDefs());
		return XmlUtils.createXPathEvaluatorSource(x -> "<xsl:choose>" +
						"<xsl:when " + namespaceClause + " test=\"" + XmlEncodingUtils.encodeChars(x) + "\">" + getThenForwardName() + "</xsl:when>" +
						"<xsl:otherwise>" + getElseForwardName() + "</xsl:otherwise>" +
						"</xsl:choose>",
				xpathExpression + (StringUtils.isEmpty(expressionValue) ? "" : "='" + expressionValue + "'"),
				OutputType.TEXT, false, getParameterList(), true, !isNamespaceAware(), xsltVersion
		);
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isNotEmpty(getXpathExpression())) {
			try {
				tp = TransformerPool.getInstance(makeStylesheet(getXpathExpression(), getExpressionValue()), xsltVersion, this);
			} catch (TransformerConfigurationException e) {
				throw new ConfigurationException("could not create transformer from xpathExpression [" + getXpathExpression() + "], target expressionValue [" + getExpressionValue() + "]", e);
			}
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String sInput;
		if (StringUtils.isEmpty(getSessionKey())) {
			if (Message.isEmpty(message)) {
				sInput = "";
			} else {
				try {
					sInput = message.asString();
				} catch (IOException e) {
					throw new PipeRunException(this, "cannot open stream", e);
				}
				if (sInput == null) {
					throw new PipeRunException(this, "Input message is empty");
				}
			}
		} else {
			log.debug("taking input from sessionKey [{}]", getSessionKey());
			sInput = session.getString(getSessionKey());
			if (sInput == null) {
				throw new PipeRunException(this, "unable to resolve session key [" + getSessionKey() + "]");
			}
		}

		String forward;
		if (tp != null) {
			try {
				Map<String, Object> parameterValues = null;
				ParameterList parameterList = getParameterList();
				if (!parameterList.isEmpty()) {
					parameterValues = parameterList.getValues(message, session, isNamespaceAware()).getValueMap();
				}
				forward = tp.transform(sInput, parameterValues, isNamespaceAware());
			} catch (Exception e) {
				throw new PipeRunException(this, "cannot evaluate expression", e);
			}
		} else {
			forward = getForward(sInput);
		}

		log.debug("determined forward [{}]", forward);

		PipeForward pipeForward = findForward(forward);
		if (pipeForward == null) {
			throw new PipeRunException(this, "cannot find forward or pipe named [" + forward + "]");
		}

		log.debug("resolved forward [{}] to path [{}]", forward, pipeForward.getPath());
		return new PipeRunResult(pipeForward, message);
	}

	private String getForward(String sInput) {
		if (StringUtils.isNotEmpty(getRegex())) {
			return sInput.matches(getRegex()) ? thenForwardName : elseForwardName;
		} else if (StringUtils.isNotEmpty(getExpressionValue())) {
			return sInput.equals(expressionValue) ? thenForwardName : elseForwardName;
		}

		// If the input is empty, use the else forward.
		return StringUtils.isEmpty(sInput) ? elseForwardName : thenForwardName;
	}

	@Override
	public boolean consumesSessionVariable(String sessionKey) {
		return super.consumesSessionVariable(sessionKey) || sessionKey.equals(getSessionKey());
	}

	@Deprecated(forRemoval = true, since = "7.7.0")
	@ConfigurationWarning("Please use getInputFromSessionKey instead.")
	/** name of the key in the <code>pipelinesession</code> to retrieve the input-message from. if not set, the current input message of the pipe is taken. n.b. same as <code>getinputfromsessionkey</code> */
	public void setSessionKey(String sessionKey) {
		this.sessionKey = sessionKey;
	}

	/** a string to compare the result of the xpathExpression (or the input-message itself) to. If not specified, a non-empty result leads to the 'then'-forward, an empty result to 'else'-forward */
	public void setExpressionValue(String expressionValue) {
		this.expressionValue = expressionValue;
	}

	/**
	 * forward returned when output is <code>true</code>
	 *
	 * @ff.default then
	 */
	public void setThenForwardName(String thenForwardName) {
		this.thenForwardName = thenForwardName;
	}

	/**
	 * forward returned when output is <code>false</code>
	 *
	 * @ff.default else
	 */
	public void setElseForwardName(String elseForwardName) {
		this.elseForwardName = elseForwardName;
	}

	/** xpath expression to be applied to the input-message. if not set, no transformation is done */
	public void setXpathExpression(String string) {
		xpathExpression = string;
	}

	/**
	 * Regular expression to be applied to the input-message (ignored if <code>xpathExpression</code> is specified).
	 * The input-message <b>fully</b> matching the given regular expression leads to the 'then'-forward
	 */
	@Deprecated(forRemoval = true, since = "9.0")
	@ConfigurationWarning(value = "Use RegExPipe instead")
	public void setRegex(String regex) {
		this.regex = regex;
	}

	/**
	 * If set to <code>2</code> or <code>3</code> a Saxon (net.sf.saxon) xslt processor 2.0 or 3.0 respectively will be used, otherwise xslt processor 1.0 (org.apache.xalan)
	 *
	 * @ff.default 2
	 */
	public void setXsltVersion(int xsltVersion) {
		this.xsltVersion = xsltVersion;
	}

	/** namespace definitions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code> definitions. */
	public void setNamespaceDefs(String namespaceDefs) {
		this.namespaceDefs = namespaceDefs;
	}

	/**
	 * controls namespace-awareness of XSLT transformation
	 *
	 * @ff.default true
	 */
	public void setNamespaceAware(boolean b) {
		namespaceAware = b;
	}
}
