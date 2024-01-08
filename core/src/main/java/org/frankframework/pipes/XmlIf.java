/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021-2023 WeAreFrank!

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
import org.frankframework.doc.ElementType;
import org.frankframework.doc.ElementType.ElementTypes;
import org.frankframework.parameters.ParameterList;
import org.frankframework.stream.Message;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.TransformerPool.OutputType;
import org.frankframework.util.XmlEncodingUtils;
import org.frankframework.util.XmlUtils;

/**
 * Selects an forward, based on XPath evaluation
 *
 * @ff.forward then The configured condition is met
 * @ff.forward else The configured condition is not met
 *
 * @author  Peter Leeuwenburgh
 * @since   4.3
 */
@ElementType(ElementTypes.ROUTER)
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

	protected String makeStylesheet(String xpathExpression, String resultVal) {
		String namespaceClause = XmlUtils.getNamespaceClause(getNamespaceDefs());
		return XmlUtils.createXPathEvaluatorSource(x -> "<xsl:choose>" +
															"<xsl:when "+namespaceClause+" test=\"" + XmlEncodingUtils.encodeChars(x) + "\">" +getThenForwardName()+"</xsl:when>"+
															"<xsl:otherwise>" +getElseForwardName()+"</xsl:otherwise>" +
														"</xsl:choose>",
													xpathExpression + (StringUtils.isEmpty(resultVal)?"":"='"+resultVal+"'"),
													OutputType.TEXT, false, getParameterList(), true, !isNamespaceAware(), xsltVersion);
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isNotEmpty(getXpathExpression())) {
			try {
				tp = TransformerPool.getInstance(makeStylesheet(getXpathExpression(), getExpressionValue()));
			} catch (TransformerConfigurationException e) {
				throw new ConfigurationException("could not create transformer from xpathExpression ["+getXpathExpression()+"], target expressionValue ["+getExpressionValue()+"]",e);
			}
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String forward;
		PipeForward pipeForward;

		String sInput;
		if (StringUtils.isEmpty(getSessionKey())) {
			if (Message.isEmpty(message)) {
				sInput="";
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
			sInput=session.getString(getSessionKey());
			if (sInput == null) {
				throw new PipeRunException(this, "unable to resolve session key ["+getSessionKey()+"]");
			}
		}

		if (tp != null) {
			try {
				Map<String,Object> parametervalues = null;
				ParameterList parameterList = getParameterList();
				if (!parameterList.isEmpty()) {
					parametervalues = parameterList.getValues(message, session, isNamespaceAware()).getValueMap();
				}
				forward = tp.transform(sInput, parametervalues, isNamespaceAware());
			} catch (Exception e) {
				throw new PipeRunException(this,"cannot evaluate expression",e);
			}
		} else if (StringUtils.isNotEmpty(getRegex())) {
			forward = sInput.matches(getRegex()) ? thenForwardName : elseForwardName;
		} else {
			if (StringUtils.isEmpty(getExpressionValue())) {
				forward = StringUtils.isEmpty(sInput) ? elseForwardName : thenForwardName;
			} else {
				forward = sInput.equals(expressionValue) ? thenForwardName : elseForwardName;
			}
		}

		log.debug("determined forward [{}]", forward);

		pipeForward=findForward(forward);

		if (pipeForward == null) {
			throw new PipeRunException (this, "cannot find forward or pipe named [" + forward + "]");
		}
		log.debug("resolved forward [{}] to path [{}]", forward, pipeForward.getPath());
		return new PipeRunResult(pipeForward, message);
	}

	@Override
	public boolean consumesSessionVariable(String sessionKey) {
		return super.consumesSessionVariable(sessionKey) || sessionKey.equals(getSessionKey());
	}

	@Deprecated
	@ConfigurationWarning("Please use getInputFromSessionKey instead.")
	/** name of the key in the <code>pipelinesession</code> to retrieve the input-message from. if not set, the current input message of the pipe is taken. n.b. same as <code>getinputfromsessionkey</code> */
	public void setSessionKey(String sessionKey){
		this.sessionKey = sessionKey;
	}

	/** a string to compare the result of the xpathexpression (or the input-message itself) to. if not specified, a non-empty result leads to the 'then'-forward, an empty result to 'else'-forward */
	public void setExpressionValue(String expressionValue){
		this.expressionValue = expressionValue;
	}

	/**
	 * forward returned when <code>'true'</code>
	 * @ff.default then
	 */
	public void setThenForwardName(String thenForwardName){
		this.thenForwardName = thenForwardName;
	}

	/**
	 * forward returned when 'false'
	 * @ff.default else
	 */
	public void setElseForwardName(String elseForwardName){
		this.elseForwardName = elseForwardName;
	}

	/** xpath expression to be applied to the input-message. if not set, no transformation is done */
	public void setXpathExpression(String string) {
		xpathExpression = string;
	}

	/** regular expression to be applied to the input-message (ignored if xpathexpression is specified). the input-message matching the given regular expression leads to the 'then'-forward */
	public void setRegex(String regex){
		this.regex = regex;
	}

	/**
	 * If set to <code>2</code> or <code>3</code> a Saxon (net.sf.saxon) xslt processor 2.0 or 3.0 respectively will be used, otherwise xslt processor 1.0 (org.apache.xalan)
	 * @ff.default 2
	 */
	public void setXsltVersion(int xsltVersion) {
		this.xsltVersion = xsltVersion;
	}

	/** namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions. */
	public void setNamespaceDefs(String namespaceDefs) {
		this.namespaceDefs = namespaceDefs;
	}


	/**
	 * controls namespace-awareness of XSLT transformation
	 * @ff.default true
	 */
	public void setNamespaceAware(boolean b) {
		namespaceAware = b;
	}
}
