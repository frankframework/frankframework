/*
 * $Log: ForEachChildElementPipeOrg.java,v $
 * Revision 1.3  2007-07-17 11:06:30  europe\L190409
 * switch to new version
 *
 * Revision 1.11  2007/07/10 10:06:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * switch back to original ForEachChildElementPipe
 *
 * Revision 1.1  2007/07/10 07:53:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * original ForEachChildElementPipe
 *
 * Revision 1.9  2007/04/26 11:58:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * optional xml-version removal
 *
 * Revision 1.8  2006/01/05 14:36:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.7  2005/10/24 09:21:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made namespaceAware an attribute of AbstractPipe
 *
 * Revision 1.6  2005/09/08 08:29:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * debugged stopCondition
 *
 * Revision 1.5  2005/09/08 07:18:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * embedded partial result in XML before evaluating stopcondition
 *
 * Revision 1.4  2005/09/08 07:09:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed end-tags of results
 * debug stopcondition
 *
 * Revision 1.3  2005/09/07 15:29:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * collected all results together
 * added stopConditionXPathExpression attribute
 * added elementXPathExpression attribute
 *
 * Revision 1.2  2005/08/30 15:59:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added configuration hint
 *
 * Revision 1.1  2005/06/20 09:08:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of ForEachChildElementPipe
 *
 */
package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.util.HashMap;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Sends a message to a Sender for each child element of the input XML.
 * 
 * <br>
 * The output of each of the processing of each of the elements is returnen in XML as follows:
 * <pre>
 *  &lt;results count="num_of_elements"&gt;
 *    &lt;result item="1"&gt;result of processing of first item&lt;/result&gt;
 *    &lt;result item="2"&gt;result of processing of second item&lt;/result&gt;
 *       ...
 *  &lt;/results&gt;
 * </pre>
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.ForEachChildElementPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, nl.nn.adapterframework.core.PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setNamespaceAware(boolean) namespaceAware}</td><td>controls namespace-awareness of possible XML parsing in descender-classes</td><td>application default</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setResultOnTimeOut(String) resultOnTimeOut}</td><td>result returned when no return-message was received within the timeout limit</td><td>"receiver timed out"</td></tr>
 * <tr><td>{@link #setLinkMethod(String) linkMethod}</td><td>Indicates wether the server uses the correlationID or the messageID in the correlationID field of the reply</td><td>CORRELATIONID</td></tr>
 * <tr><td>{@link #setStopConditionXPathExpression(String) stopConditionXPathExpression}</td><td>expression evaluated on each result if set. 
 * 		Iteration stops if condition returns anything other than <code>false</code> or an empty result.
 * For example, to stop after the second child element has been processed, one of the following expressions could be used:
 * <table> 
 * <tr><td><li><code>result[@item='2']</code></td><td>returns result element after second child element has been processed</td></tr>
 * <tr><td><li><code>result/@item='2'</code></td><td>returns <code>false</code> after second child element has been processed, <code>true</code> for others</td></tr>
 * </table> 
 * </td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setElementXPathExpression(String) elementXPathExpression}</td><td>expression used to determine the set of elements iterated over, i.e. the set of child elements</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRemoveXmlDeclarationInResults(boolean) removeXmlDeclarationInResults}</td><td>postprocess each partial result, to remove the xml-declaration, as this is not allowed inside an xml-document</td><td>false</td></tr>
 * </table>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ISender sender}</td><td>specification of sender to send messages with</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ICorrelatedPullingListener listener}</td><td>specification of listener to listen to for replies</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the pipe will be handed to the sender, if this is a {@link IParameterizedSender}</td></tr>
 * </table>
 * </p>
 * 
 * For more configuration options, see {@link MessageSendingPipe}.
 * <br>
 * use parameters like:
 * <pre>
 *	&lt;param name="element-name-of-current-item"  xpathExpression="name(/*)" /&gt;
 *	&lt;param name="value-of-current-item"         xpathExpression="/*" /&gt;
 * </pre>
 * 
 * @author Gerrit van Brakel
 * @since 4.3
 * 
 * $Id: ForEachChildElementPipeOrg.java,v 1.3 2007-07-17 11:06:30 europe\L190409 Exp $
 */
public class ForEachChildElementPipeOrg extends MessageSendingPipe {
	public static final String version="$RCSfile: ForEachChildElementPipeOrg.java,v $ $Revision: 1.3 $ $Date: 2007-07-17 11:06:30 $";

	private boolean elementsOnly=true;
	private String stopConditionXPathExpression=null;
	private String elementXPathExpression=null;
	private boolean removeXmlDeclarationInResults=false;

	private TransformerPool identityTp;
	private TransformerPool extractElementsTp=null;
	private TransformerPool stopConditionTp=null;
	private TransformerPool encapsulateResultsTp=null;

	private String makeEncapsulatingXslt(String rootElementname,String xpathExpression) {
		return 
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\" xmlns:xalan=\"http://xml.apache.org/xslt\">" +
		"<xsl:output method=\"xml\" omit-xml-declaration=\"yes\"/>" +
		"<xsl:strip-space elements=\"*\"/>" +
		"<xsl:template match=\"/\">" +
		"<xsl:element name=\"" + rootElementname + "\">" +
		"<xsl:copy-of select=\"" + xpathExpression + "\"/>" +
		"</xsl:element>" +
		"</xsl:template>" +
		"</xsl:stylesheet>";
	}


	public void configure() throws ConfigurationException {
		super.configure();
		try {
			identityTp=new TransformerPool(XmlUtils.IDENTITY_TRANSFORM);
			if (StringUtils.isNotEmpty(getStopConditionXPathExpression())) {
				stopConditionTp=new TransformerPool(XmlUtils.createXPathEvaluatorSource(null,getStopConditionXPathExpression(),"xml",false));
			}
			if (StringUtils.isNotEmpty(getElementXPathExpression())) {
				extractElementsTp=new TransformerPool(makeEncapsulatingXslt("root",getElementXPathExpression()));
			}
			if (isRemoveXmlDeclarationInResults()) {
				encapsulateResultsTp=new TransformerPool( makeEncapsulatingXslt("result","*"));
			}
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException(e);
		}
	}

	public void start() throws PipeStartException  {
		try {
			identityTp.open();
		} catch (Exception e) {
			throw new PipeStartException(e);
		}
		super.start();
	}

	public void stop()   {
		identityTp.close();
		super.stop();
	}
	

	protected String sendMessage(Object input, PipeLineSession session, String correlationID, ISender sender, HashMap threadContext) throws SenderException, TimeOutException {
		// sendResult has a messageID for async senders, the result for sync senders
		String result=null;
		ISenderWithParameters psender=null;
		if (sender instanceof ISenderWithParameters && getParameterList()!=null) {
			psender = (ISenderWithParameters) sender;
		}		
		String resultsXml = "";
		boolean keepGoing = true;
		try {
			int count=0;
			String inputString=(String)input;
			if (extractElementsTp!=null) {
				log.debug("transforming input to obtain list of elements using xpath ["+getElementXPathExpression()+"]");
				inputString=extractElementsTp.transform((String)input,null, isNamespaceAware());
			}
			Element fullMessage = XmlUtils.buildElement(inputString, isNamespaceAware());
			Node node=fullMessage.getFirstChild();
			while (keepGoing && node!=null) { 
				if (elementsOnly) {
					while (node!=null && !(node instanceof Element)) { 
						node=node.getNextSibling();
					}
				}
				if (node!=null) {
					count++;
					DOMSource src = new DOMSource(node);
					String item=identityTp.transform(src,null);
					if (log.isDebugEnabled()) {
						//log.debug(getLogPrefix(session)+"set current item to ["+item+"]");
						log.debug(getLogPrefix(session)+"sending item no ["+count+"] element name ["+node.getNodeName()+"]");
					} 
					if (psender!=null) {
						//result = psender.sendMessage(correlationID, item, new ParameterResolutionContext(src, session));
						//TODO find out why ParameterResolutionContext cannot be constructed using dom-source
						ParameterResolutionContext prc = new ParameterResolutionContext(item, session, isNamespaceAware());
						result = psender.sendMessage(correlationID, item, prc);
					} else {
						result = sender.sendMessage(correlationID, item);
					}
					if (isRemoveXmlDeclarationInResults()) {
						log.debug(getLogPrefix(session)+"post processing partial result ["+result+"]");
						result = encapsulateResultsTp.transform(result,null);
					} else {
						log.debug(getLogPrefix(session)+"partial result ["+result+"]");
						result = "<result>\n"+result+"\n</result>";
					}
					resultsXml += result+"\n";
					node=node.getNextSibling();
				}
				if (stopConditionTp!=null) {
					String stopConditionResult = stopConditionTp.transform(result,null);
					if (StringUtils.isNotEmpty(stopConditionResult) && !stopConditionResult.equalsIgnoreCase("false")) {
						log.debug(getLogPrefix(session)+"stopcondition result ["+stopConditionResult+"], stopping loop");
						keepGoing=false;
					} else {
						log.debug(getLogPrefix(session)+"stopcondition result ["+stopConditionResult+"], continueing loop");
					}
				}
			}
			resultsXml = "<results count=\""+count+"\">\n"+resultsXml+"</results>";
			return resultsXml;
		} catch (DomBuilderException e) {
			throw new SenderException(getLogPrefix(session)+"cannot parse input",e);
		} catch (TransformerException e) {
			throw new SenderException(getLogPrefix(session)+"cannot serialize item",e);
		} catch (IOException e) {
			throw new SenderException(getLogPrefix(session)+"cannot serialize item",e);
		}
	}


	public void setSender(ISender sender) {
		super.setSender(sender);
	}


	public void setStopConditionXPathExpression(String string) {
		stopConditionXPathExpression = string;
	}
	public String getStopConditionXPathExpression() {
		return stopConditionXPathExpression;
	}


	public void setElementXPathExpression(String string) {
		elementXPathExpression = string;
	}
	public String getElementXPathExpression() {
		return elementXPathExpression;
	}

	public void setRemoveXmlDeclarationInResults(boolean b) {
		removeXmlDeclarationInResults = b;
	}
	public boolean isRemoveXmlDeclarationInResults() {
		return removeXmlDeclarationInResults;
	}
}
