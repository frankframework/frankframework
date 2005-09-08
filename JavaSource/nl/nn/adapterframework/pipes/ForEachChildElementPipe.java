/*
 * $Log: ForEachChildElementPipe.java,v $
 * Revision 1.4  2005-09-08 07:09:46  europe\L190409
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
 * <tr><td>{@link #setStopConditionXPathExpression(String) stopConditionXPathExpression}</td><td>expression evaluated on each result if set. Iteration stops if condition returns an empty result</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setElementXPathExpression(String) elementXPathExpression}</td><td>expression used to determine the set of elements iterated over, i.e. the set of child elements</td><td>&nbsp;</td></tr>
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
 * $Id: ForEachChildElementPipe.java,v 1.4 2005-09-08 07:09:46 europe\L190409 Exp $
 */
public class ForEachChildElementPipe extends MessageSendingPipe {
	public static final String version="$RCSfile: ForEachChildElementPipe.java,v $ $Revision: 1.4 $ $Date: 2005-09-08 07:09:46 $";

	private boolean elementsOnly=true;
	private String stopConditionXPathExpression=null;
	private String elementXPathExpression=null;

	private TransformerPool identityTp;
	private TransformerPool extractElementsTp=null;
	private TransformerPool stopConditionTp=null;

	private String makeElementExtractionXslt(String xpathExpression) {
		return 
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\" xmlns:xalan=\"http://xml.apache.org/xslt\">" +
		"<xsl:output method=\"xml\" omit-xml-declaration=\"yes\"/>" +
		"<xsl:strip-space elements=\"*\"/>" +
		"<xsl:template match=\"/\">" +
		"<xsl:element name=\"root\">" +
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
				stopConditionTp=new TransformerPool(XmlUtils.createXPathEvaluatorSource(getStopConditionXPathExpression()));
			}
			if (StringUtils.isNotEmpty(getElementXPathExpression())) {
				extractElementsTp=new TransformerPool(makeElementExtractionXslt(getElementXPathExpression()));
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
				inputString=extractElementsTp.transform((String)input,null);
			}
			Element fullMessage = XmlUtils.buildElement(inputString);
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
						result = psender.sendMessage(correlationID, item, new ParameterResolutionContext(item, session));
					} else {
						result = sender.sendMessage(correlationID, item);
					}
					resultsXml += "<result item=\""+count+"\">\n"+result+"\n</result>\n";
					node=node.getNextSibling();
				}
				if (stopConditionTp!=null) {
					String stopConditionResult = stopConditionTp.transform(result,null);
					log.debug(getLogPrefix(session)+"stopcondition result ["+stopConditionResult+"]");
					if (StringUtils.isEmpty(stopConditionResult)) {
						keepGoing=false;
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

}
