/*
 * $Log: XmlIf.java,v $
 * Revision 1.10  2011-11-30 13:51:50  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.8  2008/01/30 14:47:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid NPE
 *
 * Revision 1.7  2007/08/29 15:08:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.6  2006/01/05 14:36:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.5  2005/12/29 15:18:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.4  2005/10/24 09:20:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made namespaceAware an attribute of AbstractPipe
 *
 * Revision 1.3  2005/08/30 16:03:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * decreased amount of logging
 *
 * Revision 1.2  2005/08/25 15:49:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging
 *
 * Revision 1.1  2005/08/24 15:54:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of XmlIf
 *
 */

package nl.nn.adapterframework.pipes;

import javax.xml.transform.TransformerConfigurationException;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.TransformerPool;

/**
 * Selects an exitState, based on the content of a sessionkey.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.XmlIf</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNamespaceAware(boolean) namespaceAware}</td><td>controls namespace-awareness of transformation</td><td>application default</td></tr>
 * <tr><td>{@link #setSessionKey(String) sessionKey}</td><td>name of the key in the <code>PipeLineSession</code> to retrieve the input-message from. If not set, the current input message of the Pipe is taken. N.B. same as <code>getInputFromSessionKey</code></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setXpathExpression(String) xpathExpression}</td><td>XPath expression to be applied to the input-message. If not set, no transformation is done</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setExpressionValue(String) expressionValue}</td><td>a string to compare the result of the xpathExpression (or the input-message itself) to. If not specified, a non-empty result leads to the 'then'-forward, an empty result to 'else'-forward</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setThenForwardName(String) thenForwardName}</td><td>forward returned when 'true'</code></td><td>then</td></tr>
 * <tr><td>{@link #setElseForwardName(String) elseForwardName}</td><td>forward returned when 'false'</td><td>else</td></tr>
 * </table>
 * </p>
 *
 * @version Id
 * @author  Peter Leeuwenburgh
 * @since   4.3
 */

public class XmlIf extends AbstractPipe {
	public static final String version="$RCSfile: XmlIf.java,v $ $Revision: 1.10 $ $Date: 2011-11-30 13:51:50 $";

	private String sessionKey = null;
	private String xpathExpression = null;
	private String expressionValue = null;
	private String thenForwardName = "then";
	private String elseForwardName = "else";

	private TransformerPool tp = null;

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
				tp = new TransformerPool(makeStylesheet(getXpathExpression(), getExpressionValue()));
			} catch (TransformerConfigurationException e) {
				throw new ConfigurationException(getLogPrefix(null)+"could not create transformer from xpathExpression ["+getXpathExpression()+"], target expressionValue ["+getExpressionValue()+"]",e);
			}
		}
	}

	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
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
		} else {
			if (sInput.equals(expressionValue)) {
				forward = thenForwardName;
			}
			else {
				forward = elseForwardName;
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
	

	public void setSessionKey(String sessionKey){
		this.sessionKey = sessionKey;
	}

	public String getSessionKey(){
		return sessionKey;
	}

	public void setExpressionValue(String expressionValue){
		this.expressionValue = expressionValue;
	}
	public String getExpressionValue(){
		return expressionValue;
	}

	public void setThenForwardName(String thenForwardName){
		this.thenForwardName = thenForwardName;
	}
	public String getThenForwardName(){
		return thenForwardName;
	}

	public void setElseForwardName(String elseForwardName){
		this.elseForwardName = elseForwardName;
	}
	public String getElseForwardName(){
		return elseForwardName;
	}
	

	public void setXpathExpression(String string) {
		xpathExpression = string;
	}
	public String getXpathExpression() {
		return xpathExpression;
	}

}
