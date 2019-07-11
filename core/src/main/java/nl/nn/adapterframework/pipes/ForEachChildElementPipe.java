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

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Sends a message to a Sender for each child element of the input XML.
 * Input can be a String containing XML, a filename (set processFile true), an InputStream or a Reader.
 * 
 * @author Gerrit van Brakel
 * @since 4.6.1
 */
public class ForEachChildElementPipe extends IteratingPipe {

	private String elementXPathExpression=null;
	private boolean processFile=false;
	private String charset=StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;

	private TransformerPool extractElementsTp=null;
	private int xsltVersion=0; // set to 0 for auto detect.

	{ 
		setNamespaceAware(true);
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		try {
			if (StringUtils.isNotEmpty(getElementXPathExpression())) {
				extractElementsTp=TransformerPool.getInstance(makeEncapsulatingXslt("root",getElementXPathExpression()), getXsltVersion());
			}
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException(getLogPrefix(null)+"elementXPathExpression ["+getElementXPathExpression()+"]",e);
		}
	}

	@Override
	public void start() throws PipeStartException  {
		try {
			if (extractElementsTp!=null) {
				extractElementsTp.open();
			}
		} catch (Exception e) {
			throw new PipeStartException(e);
		}
		super.start();
	}

	@Override
	public void stop()   {
		if (extractElementsTp!=null) {
			extractElementsTp.close();
		}
		super.stop();
	}

	private class ItemCallbackCallingHandler extends DefaultHandler {
		
		ItemCallback callback;
		
		StringBuffer elementbuffer=new StringBuffer();
		int elementLevel=0;
		int itemCounter=0;
		Exception rootException=null;
		int startLength;		
		boolean contentSeen;
		boolean stopRequested;
		TimeOutException timeOutException;
		
		public ItemCallbackCallingHandler(ItemCallback callback) {
			this.callback=callback;
			elementbuffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			if (getBlockSize()>0) {
				elementbuffer.append(getBlockPrefix());
			}
			startLength=elementbuffer.length();
		}

		private void checkInterrupt() throws SAXException {
			if (Thread.currentThread().isInterrupted()) {
				rootException = new InterruptedException("Thread has been interrupted");
				rootException.fillInStackTrace();
				throw new SAXException("Thread has been interrupted");
			}
		}
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			checkInterrupt();
			if (elementLevel>1) {
				if (!contentSeen) {
					contentSeen=true;
					elementbuffer.append(">");
				}
				elementbuffer.append(XmlUtils.encodeChars(ch, start, length));
			}
		}

		@Override
		public void endElement(String uri, String localName, String qname) throws SAXException {
			checkInterrupt();
			if (elementLevel>1) {
				if (!contentSeen) {
					contentSeen=true;
					elementbuffer.append("/>");
				} else {
					elementbuffer.append("</"+localName+">");
				}
			}
			elementLevel--;
			if (elementLevel == 1) {
				itemCounter++;
			}
			if ((elementLevel == 1 && itemCounter >= getBlockSize())
					|| (elementLevel == 0 && itemCounter > 0)) {
				try {
					if (getBlockSize()>0) {
						elementbuffer.append(getBlockSuffix());
					}
					stopRequested = !callback.handleItem(elementbuffer.toString());
					elementbuffer.setLength(startLength);
					itemCounter=0;
				} catch (Exception e) {
					if (e instanceof TimeOutException) {
						timeOutException = (TimeOutException)e;
					}
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
					throw new SAXException("stop requested");
				}
			}
		}


		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)	throws SAXException {
			checkInterrupt();
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

		public boolean isStopRequested() {
			return stopRequested;
		}

		public TimeOutException getTimeOutException() {
			return timeOutException;
		}
	}


	@Override
	protected void iterateOverInput(Object input, IPipeLineSession session, String correlationID, Map threadContext, ItemCallback callback) throws SenderException, TimeOutException {
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
				if (handler.getTimeOutException()!=null) {
					throw handler.getTimeOutException();
				}
				if (!handler.isStopRequested()) {
					throw new SenderException("Could not extract list of elements using xpath ["+getElementXPathExpression()+"]",e);
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
				if (handler.getTimeOutException()!=null) {
					throw handler.getTimeOutException();
				}
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



	@IbisDoc({"expression used to determine the set of elements iterated over, i.e. the set of child elements", ""})
	public void setElementXPathExpression(String string) {
		elementXPathExpression = string;
	}
	public String getElementXPathExpression() {
		return elementXPathExpression;
	}

	@IbisDoc({"when set <code>true</code>, the input is assumed to be the name of a file to be processed. otherwise, the input itself is transformed", "application default"})
	public void setProcessFile(boolean b) {
		processFile = b;
	}
	public boolean isProcessFile() {
		return processFile;
	}

	@IbisDoc({"characterset used for reading file or inputstream, only used when {@link #setProcessFile(boolean) processFile} is <code>true</code>, or the input is of type InputStream", "utf-8"})
	public void setCharset(String string) {
		charset = string;
	}
	public String getCharset() {
		return charset;
	}

	@IbisDoc({"when set to <code>2</code> xslt processor 2.0 (net.sf.saxon) will be used, otherwise xslt processor 1.0 (org.apache.xalan). <code>0</code> will auto detect", "0"})
	public void setXsltVersion(int xsltVersion) {
		this.xsltVersion=xsltVersion;
	}
	public int getXsltVersion() {
		return xsltVersion;
	}

	@IbisDoc({"Deprecated: when set <code>true</code> xslt processor 2.0 (net.sf.saxon) will be used, otherwise xslt processor 1.0 (org.apache.xalan)", "false"})
	/**
	 * @deprecated Please remove setting of xslt2, it will be auto detected. Or use xsltVersion.
	 */
	@Deprecated
	public void setXslt2(boolean b) {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = ClassUtils.nameOf(this) +"["+getName()+"]: the attribute 'xslt2' has been deprecated. Its value is now auto detected. If necessary, replace with a setting of xsltVersion";
		configWarnings.add(log, msg);
		xsltVersion=b?2:1;
	}
}
