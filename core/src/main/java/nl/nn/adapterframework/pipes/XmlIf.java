/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021, 2022 WeAreFrank!

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

import java.io.IOException;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Selects an exitState, based on xpath evaluation
 * 
 * @ff.forward then The configured condition is met
 * @ff.forward else The configured condition is not met
 *
 * @author  Peter Leeuwenburgh
 * @since   4.3
 */

public class XmlIf extends AbstractPipe {

	private @Getter String namespaceDefs = null;
	private @Getter String sessionKey = null;
	private @Getter String xpathExpression = null;
	private @Getter String expressionValue = null;
	private @Getter String thenForwardName = "then";
	private @Getter String elseForwardName = "else";
	private @Getter String regex = null;
	private @Getter int xsltVersion = XmlUtils.DEFAULT_XSLT_VERSION;

	private TransformerPool tp = null;

	{
		setNamespaceAware(true);
	}

	protected String makeStylesheet(String xpathExpression, String resultVal) {
		String nameSpaceClause = XmlUtils.getNamespaceClause(getNamespaceDefs());
		StringBuilder parameters = new StringBuilder("");
		for(Parameter parameter : getParameterList()) {
				parameters.append("<xsl:param name=\"" + parameter.getName() + "\"/>");
		}

		String result = 
			// "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
			"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\""+getXsltVersion()+".0\">" +
			"<xsl:output method=\"text\" omit-xml-declaration=\"yes\"/>" +
			"<xsl:strip-space elements=\"*\"/>" +
			parameters.toString()+
			"<xsl:template match=\"/\">" +
			"<xsl:choose>" +
			"<xsl:when "+nameSpaceClause+" test=\"" +xpathExpression + 
				(StringUtils.isEmpty(resultVal)?"":"='"+resultVal+"'")+
			"\">" +getThenForwardName()+"</xsl:when>"+
			"<xsl:otherwise>" +getElseForwardName()+"</xsl:otherwise>" +
			"</xsl:choose>" +
			"</xsl:template>" +
			"</xsl:stylesheet>";
			log.debug(getLogPrefix(null)+"created stylesheet ["+result+"]");
			return result;
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isNotEmpty(getXpathExpression())) {
			try {
				tp = TransformerPool.getInstance(makeStylesheet(getXpathExpression(), getExpressionValue()));
			} catch (TransformerConfigurationException e) {
				throw new ConfigurationException(getLogPrefix(null)+"could not create transformer from xpathExpression ["+getXpathExpression()+"], target expressionValue ["+getExpressionValue()+"]",e);
			}
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String forward = "";
		PipeForward pipeForward = null;

		String sInput = null;
		if (StringUtils.isEmpty(getSessionKey())) {
			if (Message.isEmpty(message)) {
				sInput="";
			} else {
				try {
					sInput = message.asString();
				} catch (IOException e) {
					throw new PipeRunException(this, getLogPrefix(session)+"cannot open stream", e);
				}
			}
		} else {
			log.debug(getLogPrefix(session)+"taking input from sessionKey ["+getSessionKey()+"]");
			try {
				sInput=session.getMessage(getSessionKey()).asString();
			} catch (IOException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "unable to resolve session key ["+getSessionKey()+"]", e);
			}
		}

		if (tp!=null) {
			try {
				Map<String,Object> parametervalues = null;
				ParameterList parameterList = getParameterList();
				if (!parameterList.isEmpty()) {
					parametervalues = parameterList.getValues(message, session, isNamespaceAware()).getValueMap();
				}
				forward = tp.transform(sInput, parametervalues, isNamespaceAware());
			} catch (Exception e) {
				throw new PipeRunException(this,getLogPrefix(session)+"cannot evaluate expression",e);
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

		log.debug(getLogPrefix(session)+ "determined forward [" + forward + "]");

		pipeForward=findForward(forward);
		
		if (pipeForward == null) {
			  throw new PipeRunException (this, getLogPrefix(null)+"cannot find forward or pipe named [" + forward + "]");
		}
		log.debug(getLogPrefix(session)+ "resolved forward [" + forward + "] to path ["+pipeForward.getPath()+"]");
		return new PipeRunResult(pipeForward, message);
	}

	@Override
	public boolean consumesSessionVariable(String sessionKey) {
		return super.consumesSessionVariable(sessionKey) || sessionKey.equals(getSessionKey());
	}

	@Deprecated
	@ConfigurationWarning("Please use getInputFromSessionKey instead.")
	@IbisDoc({"name of the key in the <code>pipelinesession</code> to retrieve the input-message from. if not set, the current input message of the pipe is taken. n.b. same as <code>getinputfromsessionkey</code>", ""})
	public void setSessionKey(String sessionKey){
		this.sessionKey = sessionKey;
	}

	@IbisDoc({"a string to compare the result of the xpathexpression (or the input-message itself) to. if not specified, a non-empty result leads to the 'then'-forward, an empty result to 'else'-forward", ""})
	public void setExpressionValue(String expressionValue){
		this.expressionValue = expressionValue;
	}

	@IbisDoc({"forward returned when <code>'true'</code>", "then"})
	public void setThenForwardName(String thenForwardName){
		this.thenForwardName = thenForwardName;
	}

	@IbisDoc({"forward returned when 'false'", "else"})
	public void setElseForwardName(String elseForwardName){
		this.elseForwardName = elseForwardName;
	}

	@IbisDoc({"xpath expression to be applied to the input-message. if not set, no transformation is done", ""})
	public void setXpathExpression(String string) {
		xpathExpression = string;
	}

	@IbisDoc({"regular expression to be applied to the input-message (ignored if xpathexpression is specified). the input-message matching the given regular expression leads to the 'then'-forward", ""})
	public void setRegex(String regex){
		this.regex = regex;
	}

	@IbisDoc({"specifies the version of xslt to use", "2"})
	public void setXsltVersion(int xsltVersion) {
		this.xsltVersion = xsltVersion;
	}

	@IbisDoc({"namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions.", ""})
	public void setNamespaceDefs(String namespaceDefs) {
		this.namespaceDefs = namespaceDefs;
	}
}
