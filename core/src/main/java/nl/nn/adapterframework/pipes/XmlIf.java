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

import javax.xml.transform.TransformerConfigurationException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.TransformerPool;

import org.apache.commons.lang.StringUtils;

/**
 * Selects an exitState, based on the content of a sessionkey.
 * 
 *
 * @author  Peter Leeuwenburgh
 * @since   4.3
 */

public class XmlIf extends AbstractPipe {

	private String sessionKey = null;
	private String xpathExpression = null;
	private String expressionValue = null;
	private String thenForwardName = "then";
	private String elseForwardName = "else";
	private String regex = null;

	private TransformerPool tp = null;
	
	{
		setNamespaceAware(true);
	}

	protected String makeStylesheet(String xpathExpression, String resultVal) {
	String result = 
		// "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">" +
		"<xsl:output method=\"text\" omit-xml-declaration=\"yes\"/>" +
		"<xsl:strip-space elements=\"*\"/>" +
		"<xsl:template match=\"/\">" +
		"<xsl:choose>" +
		"<xsl:when test=\"" +xpathExpression + 
			(StringUtils.isEmpty(resultVal)?"":"='"+resultVal+"'")+
		"\">" +getThenForwardName()+"</xsl:when>"+
		"<xsl:otherwise>" +getElseForwardName()+"</xsl:otherwise>" +
		"</xsl:choose>" +
		"</xsl:template>" +
		"</xsl:stylesheet>";
		log.debug(getLogPrefix(null)+"created stylesheet ["+result+"]");
		return result;
	}



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

	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		String forward = "";
		PipeForward pipeForward = null;

		String sInput;
		if (StringUtils.isEmpty(getSessionKey())) {
			if (input==null) {
				sInput="";
			} else {
				sInput = input.toString();
			}
		} else {
			log.debug(getLogPrefix(session)+"taking input from sessionKey ["+getSessionKey()+"]");
			sInput=(String) session.get(getSessionKey());
		}

		// log.debug(getLogPrefix(session) + "input value is [" + sInput + "]");
		
		if (tp!=null) {
			try {
				forward = tp.transform(sInput,null, isNamespaceAware());
			} catch (Exception e) {
				throw new PipeRunException(this,getLogPrefix(session)+"cannot evaluate expression",e);
			}
		} else if (StringUtils.isNotEmpty(getRegex())) {
			if (sInput.matches(getRegex())) {
				forward = thenForwardName;
			} else {
				forward = elseForwardName;
			}
		} else {
			if (StringUtils.isEmpty(expressionValue)) {
				if (StringUtils.isEmpty(sInput)) {
					forward = elseForwardName;
				}
				else {
					forward = thenForwardName;
				}
			} else {
				if (sInput.equals(expressionValue)) {
					forward = thenForwardName;
				}
				else {
					forward = elseForwardName;
				}
			}
		}

		log.debug(getLogPrefix(session)+ "determined forward [" + forward + "]");

		pipeForward=findForward(forward);
		
		if (pipeForward == null) {
			  throw new PipeRunException (this, getLogPrefix(null)+"cannot find forward or pipe named [" + forward + "]");
		}
		log.debug(getLogPrefix(session)+ "resolved forward [" + forward + "] to path ["+pipeForward.getPath()+"]");
		return new PipeRunResult(pipeForward, input);
	}
	

	@IbisDoc({"name of the key in the <code>pipelinesession</code> to retrieve the input-message from. if not set, the current input message of the pipe is taken. n.b. same as <code>getinputfromsessionkey</code>", ""})
	public void setSessionKey(String sessionKey){
		this.sessionKey = sessionKey;
	}

	public String getSessionKey(){
		return sessionKey;
	}

	@IbisDoc({"a string to compare the result of the xpathexpression (or the input-message itself) to. if not specified, a non-empty result leads to the 'then'-forward, an empty result to 'else'-forward", ""})
	public void setExpressionValue(String expressionValue){
		this.expressionValue = expressionValue;
	}
	public String getExpressionValue(){
		return expressionValue;
	}

	@IbisDoc({"forward returned when 'true'</code>", "then"})
	public void setThenForwardName(String thenForwardName){
		this.thenForwardName = thenForwardName;
	}
	public String getThenForwardName(){
		return thenForwardName;
	}

	@IbisDoc({"forward returned when 'false'", "else"})
	public void setElseForwardName(String elseForwardName){
		this.elseForwardName = elseForwardName;
	}
	public String getElseForwardName(){
		return elseForwardName;
	}
	

	@IbisDoc({"xpath expression to be applied to the input-message. if not set, no transformation is done", ""})
	public void setXpathExpression(String string) {
		xpathExpression = string;
	}
	public String getXpathExpression() {
		return xpathExpression;
	}

	@IbisDoc({"regular expression to be applied to the input-message (ignored if xpathexpression is specified). the input-message matching the given regular expression leads to the 'then'-forward", ""})
	public void setRegex(String regex){
		this.regex = regex;
	}
	public String getRegex(){
		return regex;
	}
}
