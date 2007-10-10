/*
 * $Log: ForEachChildElementPipe.java,v $
 * Revision 1.12.2.2  2007-10-10 14:30:40  europe\L190409
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.14  2007/10/08 12:23:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map where possible
 *
 * Revision 1.13  2007/09/10 11:19:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * remove unused imports
 *
 * Revision 1.12  2007/07/17 11:06:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * switch to new version
 *
 * Revision 1.1  2007/07/10 10:06:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * switch back to original ForEachChildElementPipe
 *
 * Revision 1.10  2007/07/10 07:53:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * new implementation based on IteratingPipe
 *
 *
 */
package nl.nn.adapterframework.pipes;

import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMSource;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Sends a message to a Sender for each child element of the input XML.
 * Alternative implementation, based on IteratingPipe.
 * 
 * <br>
 * The output of each of the processing of each of the elements is returned in XML as follows:
 * <pre>
 *  &lt;results count="num_of_elements"&gt;
 *    &lt;result&gt;result of processing of first item&lt;/result&gt;
 *    &lt;result&gt;result of processing of second item&lt;/result&gt;
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
 * <tr><td><li><code>result[position()='2']</code></td><td>returns result element after second child element has been processed</td></tr>
 * <tr><td><li><code>position()='2'</code></td><td>returns <code>false</code> after second child element has been processed, <code>true</code> for others</td></tr>
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
 * @since 4.6.1
 * 
 * $Id: ForEachChildElementPipe.java,v 1.12.2.2 2007-10-10 14:30:40 europe\L190409 Exp $
 */
public class ForEachChildElementPipe extends IteratingPipe {
	public static final String version="$RCSfile: ForEachChildElementPipe.java,v $ $Revision: 1.12.2.2 $ $Date: 2007-10-10 14:30:40 $";

	private String elementXPathExpression=null;

	private TransformerPool identityTp;
	private TransformerPool extractElementsTp=null;



	public void configure() throws ConfigurationException {
		super.configure();
		try {
			identityTp=new TransformerPool(XmlUtils.IDENTITY_TRANSFORM);
			if (StringUtils.isNotEmpty(getElementXPathExpression())) {
				extractElementsTp=new TransformerPool(makeEncapsulatingXslt("root",getElementXPathExpression()));
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
	
	public class ElementIterator implements IDataIterator {
		private static final boolean elementsOnly=true;

		Node node;
		boolean nextElementReady;

		public ElementIterator(String xmlString) throws SenderException {
			super();
			if (getExtractElementsTp()!=null) {
				log.debug("transforming input to obtain list of elements using xpath ["+getElementXPathExpression()+"]");
				try {
					xmlString=getExtractElementsTp().transform(xmlString, null, isNamespaceAware());
				} catch (Exception e) {
					throw new SenderException("Could not extract list of elements using xpath ["+getElementXPathExpression()+"]");
				}
			}
			Element fullMessage;
			try {
				fullMessage = XmlUtils.buildElement(xmlString, isNamespaceAware());
			} catch (DomBuilderException e) {
				throw new SenderException("Could not build elements",e);
			}
			node=fullMessage.getFirstChild();
			nextElementReady=false;
		}

		private void findNextElement() {
			if (elementsOnly) {
				while (node!=null && !(node instanceof Element)) { 
					node=node.getNextSibling();
				}
			}
		}

		public boolean hasNext() {
			findNextElement();
			return node!=null;
		}

		public Object next() throws SenderException {
			findNextElement();
			if (node==null) {
				return null;
			}
			DOMSource src = new DOMSource(node);
			String result;
			try {
				result = getIdentityTp().transform(src, null);
			} catch (Exception e) {
				throw new SenderException("could not extract element",e);
			}
			if (node!=null) {
				node=node.getNextSibling();
			} 
			return result; 
		}

		public void close() {
		}
	}

	
	protected IDataIterator getIterator(Object input, PipeLineSession session, String correlationID, Map threadContext) throws SenderException {
		return new ElementIterator((String)input);
	}

	protected TransformerPool getExtractElementsTp() {
		return extractElementsTp;
	}
	protected TransformerPool getIdentityTp() {
		return identityTp;
	}



	public void setElementXPathExpression(String string) {
		elementXPathExpression = string;
	}
	public String getElementXPathExpression() {
		return elementXPathExpression;
	}

}
