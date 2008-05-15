/*
 * $Log: ForEachChildElementPipe.java,v $
 * Revision 1.17  2008-05-15 15:32:31  europe\L190409
 * set root cause of SAX exception
 *
 * Revision 1.16  2008/02/22 14:32:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fix bug for nested elements
 *
 * Revision 1.15  2008/02/21 12:48:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added option for pushing iteration
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Sends a message to a Sender for each child element of the input XML.
 * Input can be a String containing XML, a filename (set processFile true), an InputStream or a Reader.
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
 * <tr><td>{@link #setProcessFile(boolean) processFile}</td><td>when set <code>true</code>, the input is assumed to be the name of a file to be processed. Otherwise, the input itself is transformed</td><td>application default</td></tr>
 * <tr><td>{@link #setCharset(String) charset}</td><td>characterset used for reading file or inputstream, only used when {@link #setProcessFile(boolean) processFile} is <code>true</code>, or the input is of type InputStream</td><td>UTF-8</td></tr>
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
 * $Id: ForEachChildElementPipe.java,v 1.17 2008-05-15 15:32:31 europe\L190409 Exp $
 */
public class ForEachChildElementPipe extends IteratingPipe {
	public static final String version="$RCSfile: ForEachChildElementPipe.java,v $ $Revision: 1.17 $ $Date: 2008-05-15 15:32:31 $";

	private String elementXPathExpression=null;
	private boolean processFile=false;
	private String charset=StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;

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

	private class ItemCallbackCallingHandler extends DefaultHandler {
		
		ItemCallback callback;
		
		StringBuffer elementbuffer=new StringBuffer();
		int elementLevel=0;
		Exception rootException=null;
		int startLength;		
		boolean contentSeen;
		boolean stopRequested;
		
		public ItemCallbackCallingHandler(ItemCallback callback) {
			this.callback=callback;
			elementbuffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			startLength=elementbuffer.length();
		}
		
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (elementLevel>1) {
				if (!contentSeen) {
					contentSeen=true;
					elementbuffer.append(">");
				}
				elementbuffer.append(ch, start, length);
			}
		}

		public void endElement(String uri, String localName, String qname) throws SAXException {
			if (elementLevel>1) {
				if (!contentSeen) {
					contentSeen=true;
					elementbuffer.append("/>");
				} else {
					elementbuffer.append("</"+localName+">");
				}
			}
			if (--elementLevel==1) {
				try {
					stopRequested = !callback.handleItem(elementbuffer.toString());
					elementbuffer.setLength(startLength);
				} catch (Exception e) {
					rootException =e;
					Throwable rootCause = e;
					while (rootCause.getCause()!=null) {
						rootCause=rootCause.getCause();
					}
					SAXException se = new SAXException(e);
					se.setStackTrace(rootCause.getStackTrace());
					throw se;
					
				}
				if (stopRequested) {
					throw new SAXException("stop maar");
				}
			}
		}


		public void startElement(String uri, String localName, String qName, Attributes attributes)	throws SAXException {
			if (elementLevel>1 && !contentSeen) {
				elementbuffer.append(">");
			}
			if (++elementLevel>1) {
				elementbuffer.append("<"+localName);
				for (int i=0; i<attributes.getLength(); i++) {
					elementbuffer.append(" "+attributes.getLocalName(i)+"=\""+attributes.getValue(i)+"\"");
				}
				contentSeen=false;
			}
		}

		public Exception getRootException() {
			return rootException;	
		}
		public boolean isStopRequested() {
			return stopRequested;
		}

	}


	protected void iterateInput(Object input, PipeLineSession session, String correlationID, Map threadContext, ItemCallback callback) throws SenderException {
		Reader reader=null;
		try {
			if (input instanceof Reader) {
				reader = (Reader)input;
			} else 	if (input instanceof InputStream) {
				reader=new InputStreamReader((InputStream)input,getCharset());
			} else 	if (isProcessFile()) {
				// TODO: arrange for non-namespace aware processing of files
				reader=new InputStreamReader(new FileInputStream((String)input),getCharset());
			}
		} catch (FileNotFoundException e) {
			throw new SenderException("could not find file ["+input+"]",e);
		} catch (UnsupportedEncodingException e) {
			throw new SenderException("could not use charset ["+getCharset()+"]",e);
		}
		ItemCallbackCallingHandler handler = new ItemCallbackCallingHandler(callback);
		
		if (getExtractElementsTp()!=null) {
			log.debug("transforming input to obtain list of elements using xpath ["+getElementXPathExpression()+"]");
			try {
				SAXResult transformedStream = new SAXResult();
				Source src;
				if (reader!=null) {
					src=new StreamSource(reader);
				} else {
					src = XmlUtils.stringToSourceForSingleUse((String)input, isNamespaceAware());
				}
				transformedStream.setHandler(handler);
				getExtractElementsTp().transform(src, transformedStream, null);
			} catch (Exception e) {
				if (!handler.isStopRequested()) {
					throw new SenderException("Could not extract list of elements using xpath ["+getElementXPathExpression()+"]");
				}
			}
		} else {
			
			try {
				if (reader!=null) {
					XmlUtils.parseXml(handler,new InputSource(reader));
				} else {
					XmlUtils.parseXml(handler,(String)input);
				}
			} catch (Exception e) {
				if (!handler.isStopRequested()) {
					throw new SenderException("Could not parse input",e);
				}
			}
		}
		
	}

	
//	public class ElementIterator implements IDataIterator {
//		private static final boolean elementsOnly=true;
//
//		Node node;
//		boolean nextElementReady;
//
//		public ElementIterator(String inputString) throws SenderException {
//			super();
//
//			Reader reader=null;
//			if (isProcessFile()) {
//				try {
//					// TODO: arrange for non-namespace aware processing of files
//					reader=new InputStreamReader(new FileInputStream(inputString));
//				} catch (FileNotFoundException e) {
//					throw new SenderException("could not find file ["+inputString+"]",e);
//				}
//			}
//
//			if (getExtractElementsTp()!=null) {
//				log.debug("transforming input to obtain list of elements using xpath ["+getElementXPathExpression()+"]");
//				try {
//					DOMResult fullMessage = new DOMResult();
//					Source src;
//					if (reader!=null) {
//						src=new StreamSource(reader);
//					} else {
//						src = XmlUtils.stringToSourceForSingleUse(inputString, isNamespaceAware());
//					}
//					getExtractElementsTp().transform(src, fullMessage, null);
//					node=fullMessage.getNode().getFirstChild();
//				} catch (Exception e) {
//					throw new SenderException("Could not extract list of elements using xpath ["+getElementXPathExpression()+"]");
//				}
//			} else {
//				Document fullMessage;
//				try {
//					if (reader!=null) {
//						fullMessage=XmlUtils.buildDomDocument(reader, isNamespaceAware());
//					} else {
//						fullMessage=XmlUtils.buildDomDocument(inputString, isNamespaceAware());
//					}
//					node=fullMessage.getDocumentElement().getFirstChild();
//				} catch (DomBuilderException e) {
//					throw new SenderException("Could not build elements",e);
//				}
//			}
//			nextElementReady=false;
//		}
//
//		private void findNextElement() {
//			if (elementsOnly) {
//				while (node!=null && !(node instanceof Element)) { 
//					node=node.getNextSibling();
//				}
//			}
//		}
//
//		public boolean hasNext() {
//			findNextElement();
//			return node!=null;
//		}
//
//		public Object next() throws SenderException {
//			findNextElement();
//			if (node==null) {
//				return null;
//			}
//			DOMSource src = new DOMSource(node);
//			String result;
//			try {
//				result = getIdentityTp().transform(src, null);
//			} catch (Exception e) {
//				throw new SenderException("could not extract element",e);
//			}
//			if (node!=null) {
//				node=node.getNextSibling();
//			} 
//			return result; 
//		}
//
//		public void close() {
//		}
//	}

	
//	protected IDataIterator getIterator(Object input, PipeLineSession session, String correlationID, Map threadContext) throws SenderException {
//		return new ElementIterator((String)input);
//	}

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

	public void setProcessFile(boolean b) {
		processFile = b;
	}
	public boolean isProcessFile() {
		return processFile;
	}

	public void setCharset(String string) {
		charset = string;
	}
	public String getCharset() {
		return charset;
	}

}
