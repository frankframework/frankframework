/*
   Copyright 2013,2019 Nationale-Nederlanden

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
public class ForEachChildElementPipe extends IteratingPipe<String> {

	public final int DEFAULT_XSLT_VERSION=1; // currently only Xalan supports XSLT Streaming
	
	private boolean processFile=false;
	private String elementXPathExpression=null;
	private String charset=StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
	private int xsltVersion=DEFAULT_XSLT_VERSION; 

	private TransformerPool extractElementsTp=null;

	{ 
		setNamespaceAware(true);
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		try {
			if (StringUtils.isNotEmpty(getElementXPathExpression())) {
				if (getXsltVersion()==0) {
					setXsltVersion(DEFAULT_XSLT_VERSION);
				}
				if (getXsltVersion()!=DEFAULT_XSLT_VERSION) {
					ConfigurationWarnings.add(this, log, "XsltProcessor xsltVersion ["+getXsltVersion()+"] currently does not support streaming XSLT, might lead to memory problems for large messages");
				}
				extractElementsTp=TransformerPool.getInstance(makeEncapsulatingXslt("root",getElementXPathExpression(), getXsltVersion()), getXsltVersion());
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

	protected String makeEncapsulatingXslt(String rootElementname,String xpathExpression, int xsltVersion) {
		return 
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\""+xsltVersion+".0\" xmlns:xalan=\"http://xml.apache.org/xslt\">" +
		"<xsl:output method=\"xml\" omit-xml-declaration=\"yes\"/>" +
		"<xsl:strip-space elements=\"*\"/>" +
		"<xsl:template match=\"/\">" +
		"<xsl:element name=\"" + rootElementname + "\">" +
		"<xsl:copy-of select=\"" + XmlUtils.encodeChars(xpathExpression) + "\"/>" +
		"</xsl:element>" +
		"</xsl:template>" +
		"</xsl:stylesheet>";
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
	protected void iterateOverInput(Object input, IPipeLineSession session, String correlationID, Map<String,Object> threadContext, ItemCallback callback) throws SenderException, TimeOutException {
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

	

	protected TransformerPool getExtractElementsTp() {
		return extractElementsTp;
	}



	@IbisDoc({"1", "When set <code>true</code>, the input is assumed to be the name of a file to be processed. otherwise, the input itself is transformed", "false"})
	public void setProcessFile(boolean b) {
		processFile = b;
	}
	public boolean isProcessFile() {
		return processFile;
	}

	@IbisDoc({"2", "expression used to determine the set of elements to be iterated over, i.e. the set of child elements.", ""})
	public void setElementXPathExpression(String string) {
		elementXPathExpression = string;
	}
	public String getElementXPathExpression() {
		return elementXPathExpression;
	}

	@IbisDoc({"3", "characterset used for reading file or inputstream, only used when {@link #setProcessFile(boolean) processFile} is <code>true</code>, or the input is of type InputStream", "utf-8"})
	public void setCharset(String string) {
		charset = string;
	}
	public String getCharset() {
		return charset;
	}

	@IbisDoc({"4", "when set to <code>2</code> xslt processor 2.0 (net.sf.saxon) will be used, supporting XPath 2.0, otherwise xslt processor 1.0 (org.apache.xalan), supporting XPath 1.0. N.B. Be aware that setting this other than 1 might cause the input file being read as a whole in to memory, as Xslt Streaming is currently only supported by the XsltProcessor that is used for xsltVersion=1", "1"})
	public void setXsltVersion(int xsltVersion) {
		this.xsltVersion=xsltVersion;
	}
	public int getXsltVersion() {
		return xsltVersion;
	}

	@IbisDoc({"5", "Deprecated: when set <code>true</code> xslt processor 2.0 (net.sf.saxon) will be used, otherwise xslt processor 1.0 (org.apache.xalan)", "false"})
	/**
	 * @deprecated Please remove setting of xslt2, it will be auto detected. Or use xsltVersion.
	 */
	@Deprecated
	public void setXslt2(boolean b) {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = ClassUtils.nameOf(this) +"["+getName()+"]: the attribute 'xslt2' has been deprecated. If necessary, replace with a setting of xsltVersion";
		configWarnings.add(log, msg);
		xsltVersion=b?2:1;
	}
}
