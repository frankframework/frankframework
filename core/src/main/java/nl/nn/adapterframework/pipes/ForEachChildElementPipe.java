/*
   Copyright 2013, 2019, 2020 Nationale-Nederlanden

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
import java.io.IOException;
import java.util.Map;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.IOutputStreamingSupport;
import nl.nn.adapterframework.stream.IThreadCreator;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.StreamingResult;
import nl.nn.adapterframework.stream.ThreadLifeCycleEventListener;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.TransformerErrorListener;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.FullXmlFilter;
import nl.nn.adapterframework.xml.NamespaceRemovingFilter;
import nl.nn.adapterframework.xml.NodeSetFilter;
import nl.nn.adapterframework.xml.SaxException;
import nl.nn.adapterframework.xml.TransformerFilter;
import nl.nn.adapterframework.xml.XmlWriter;

/**
 * Sends a message to a Sender for each child element of the input XML.
 * Input can be a String containing XML, a filename (set processFile true), an InputStream or a Reader.
 * 
 * @author Gerrit van Brakel
 * @since 4.6.1
 */
public class ForEachChildElementPipe extends IteratingPipe<String> implements IThreadCreator {

	public final int DEFAULT_XSLT_VERSION=1; // currently only Xalan supports XSLT Streaming
	
	private boolean processFile=false;
	private String containerElement;
	private String targetElement;
	private String elementXPathExpression=null;
	private String charset=StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
	private int xsltVersion=DEFAULT_XSLT_VERSION; 
	private boolean removeNamespaces=true;
	private boolean streamingXslt;

	private TransformerPool extractElementsTp=null;
	private ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener;

	{ 
		setNamespaceAware(true);
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		try {
			if (StringUtils.isNotEmpty(getElementXPathExpression())) {
				streamingXslt = AppConstants.getInstance(getConfigurationClassLoader()).getBoolean("xslt.streaming.default", false);
				if (getXsltVersion()==0) {
					setXsltVersion(DEFAULT_XSLT_VERSION);
				}
				if (getXsltVersion()!=DEFAULT_XSLT_VERSION) {
					ConfigurationWarnings.add(this, log, "XsltProcessor xsltVersion ["+getXsltVersion()+"] currently does not support streaming XSLT, might lead to memory problems for large messages");
				}
				extractElementsTp=TransformerPool.getInstance(makeEncapsulatingXslt("root",getElementXPathExpression(), getXsltVersion(), getNamespaceDefs()));
			}
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException(getLogPrefix(null)+"elementXPathExpression ["+getElementXPathExpression()+"]",e);
		}
		if (StringUtils.isNotEmpty(getTargetElement()) && (getTargetElement().contains("/"))) {
			throw new ConfigurationException(getLogPrefix(null)+"targetElement ["+getTargetElement()+"] should not contain '/', only a single element name");
		}
		if (StringUtils.isNotEmpty(getContainerElement()) && (getContainerElement().contains("/"))) {
			throw new ConfigurationException(getLogPrefix(null)+"containerElement ["+getTargetElement()+"] should not contain '/', only a single element name");
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

	protected String makeEncapsulatingXslt(String rootElementname, String xpathExpression, int xsltVersion, String namespaceDefs) throws TransformerConfigurationException {
		String namespaceClause = XmlUtils.getNamespaceClause(namespaceDefs);
		return 
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\""+xsltVersion+".0\" xmlns:xalan=\"http://xml.apache.org/xslt\">" +
		"<xsl:output method=\"xml\" omit-xml-declaration=\"yes\"/>" +
		"<xsl:strip-space elements=\"*\"/>" +
		"<xsl:template match=\"/\">" +
		"<xsl:element "+namespaceClause+" name=\"" + rootElementname + "\">" +
		"<xsl:copy-of select=\"" + XmlUtils.encodeChars(xpathExpression) + "\"/>" +
		"</xsl:element>" +
		"</xsl:template>" +
		"</xsl:stylesheet>";
	}

	
//	@Override
//	protected boolean senderAffectsStreamProvidingCapability() {
//		return false;
//	}
//
//	@Override
//	protected boolean senderAffectsStreamWritingCapability() {
//		return false;
//	}

	@Override
	protected StreamingResult sendMessage(Object input, IPipeLineSession session, String correlationID, ISender sender, Map<String,Object> threadContext, IOutputStreamingSupport nextProvider) throws SenderException, TimeOutException {
		return super.sendMessage(input, session, correlationID, sender, threadContext, null);
	}

	private class ItemCallbackCallingHandler extends NodeSetFilter {
		private ItemCallback callback;
		
		private XmlWriter xmlWriter;
		private int itemCounter=0;
		private Exception rootException=null;
		private boolean stopRequested;
		private TimeOutException timeOutException;


		
		public ItemCallbackCallingHandler(ItemCallback callback) {
			super(null, null, false, false);
			setContentHandler(xmlWriter);
			this.callback=callback;
		}

		@Override
		public void startNode(String uri, String localName, String qName) throws SAXException {
			if (itemCounter==0) {
				xmlWriter= new XmlWriter();
				setContentHandler(xmlWriter);
				xmlWriter.startDocument();
				if (getBlockSize()>0) {
					try {
						xmlWriter.getWriter().append(getBlockPrefix());
					} catch (IOException e) {
						throw new SaxException("cannot write block prefix",e);
					}
				}
			}
		}

		@Override
		public void endNode(String uri, String localName, String qName) throws SAXException {
			checkInterrupt();
			if ((++itemCounter >= getBlockSize())) {
				endBlock();
			}
		}
		
		public void endBlock() throws SaxException {
			try {
				if (getBlockSize()>0) {
					xmlWriter.getWriter().append(getBlockSuffix());
				}
				xmlWriter.endDocument();
				stopRequested = !callback.handleItem(xmlWriter.toString());
				itemCounter=0;
			} catch (Exception e) {
				if (e instanceof TimeOutException) {
					timeOutException = (TimeOutException)e;
				}
				throw new SaxException(e);
				
			}
		}

		@Override
		public void endDocument() throws SAXException {
			if (itemCounter!=0) {
				endBlock();
			}
			super.endDocument();
		}


		private void checkInterrupt() throws SAXException {
			if (Thread.currentThread().isInterrupted()) {
				rootException = new InterruptedException("Thread has been interrupted");
				rootException.fillInStackTrace();
				throw new SAXException("Thread has been interrupted");
			}
		}
		


		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)	throws SAXException {
			checkInterrupt();
			super.startElement(uri, localName, qName, attributes);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			checkInterrupt();
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			checkInterrupt();
			super.characters(ch, start, length);
		}

		@Override
		public void comment(char[] ch, int start, int length) throws SAXException {
			checkInterrupt();
//			super.comment(ch, start, length);
		}

		@Override
		public void startDTD(String arg0, String arg1, String arg2) throws SAXException {
//			System.out.println("startDTD");
		}

		@Override
		public void endDTD() throws SAXException {
//			System.out.println("endDTD");
		}


		@Override
		public void startEntity(String arg0) throws SAXException {
//			System.out.println("startEntity ["+arg0+"]");
		}
		@Override
		public void endEntity(String arg0) throws SAXException {
//			System.out.println("endEntity ["+arg0+"]");
		}

		
		public boolean isStopRequested() {
			return stopRequested;
		}

		public TimeOutException getTimeOutException() {
			return timeOutException;
		}

	}

	private class StopSensor extends FullXmlFilter {
		
		private ItemCallbackCallingHandler itemHandler;
		
		public StopSensor(ItemCallbackCallingHandler itemHandler) {
			this.itemHandler=itemHandler;
		}
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			if (itemHandler.isStopRequested()) {
				throw new SAXException("stop requested");
			}
		}
	}

	@Override
	protected void iterateOverInput(Object input, IPipeLineSession session, String correlationID, Map<String,Object> threadContext, ItemCallback callback) throws SenderException, TimeOutException {
		InputSource src;
		if (isProcessFile()) {
			try {
				src = new InputSource(new FileInputStream((String)input));
			} catch (FileNotFoundException e) {
				throw new SenderException("could not find file ["+input+"]",e);
			}
		} else {
			src = new Message(input).asInputSource();
		}
		ItemCallbackCallingHandler itemHandler;
		ContentHandler inputHandler;
		String errorMessage="Could not parse input";
		TransformerErrorListener transformerErrorListener=null;
		try {
			itemHandler = new ItemCallbackCallingHandler(callback);
			inputHandler=itemHandler;
			
			if (isRemoveNamespaces()) {
				NamespaceRemovingFilter namespaceRemovingFilter = new NamespaceRemovingFilter();
				namespaceRemovingFilter.setContentHandler(itemHandler);
				inputHandler = namespaceRemovingFilter;
			}
			
			if (getExtractElementsTp()!=null) {
				if (log.isDebugEnabled()) log.debug("transforming input to obtain list of elements using xpath ["+getElementXPathExpression()+"]");
				TransformerFilter transformerFilter = getExtractElementsTp().getTransformerFilter(this, threadLifeCycleEventListener, correlationID, streamingXslt);
				transformerFilter.setContentHandler(inputHandler);
				inputHandler=transformerFilter;
				ErrorListener errorListener = transformerFilter.getTransformer().getErrorListener();
				if (errorListener!=null && errorListener instanceof TransformerErrorListener) {
					transformerErrorListener = (TransformerErrorListener)errorListener;
				}
				transformerErrorListener=(TransformerErrorListener)transformerFilter.getTransformer().getErrorListener();
				errorMessage="Could not process list of elements using xpath ["+getElementXPathExpression()+"]";
			} 
			if (StringUtils.isNotEmpty(getTargetElement())) {
				NodeSetFilter targetElementFilter = new NodeSetFilter(XmlUtils.getNamespaceMap(getNamespaceDefs()), getTargetElement(),true,true);
				targetElementFilter.setContentHandler(inputHandler);
				inputHandler=targetElementFilter;
			}
			if (StringUtils.isNotEmpty(getContainerElement())) {
				NodeSetFilter containerElementFilter = new NodeSetFilter(XmlUtils.getNamespaceMap(getNamespaceDefs()), getContainerElement(),false,true);
				containerElementFilter.setContentHandler(inputHandler);
				inputHandler=containerElementFilter;
			}
		} catch (TransformerException e) {
			throw new SenderException(errorMessage, e);
		}
		
		StopSensor stopSensor = new StopSensor(itemHandler);
		stopSensor.setContentHandler(inputHandler);
		inputHandler=stopSensor;

		try {
			XmlUtils.parseXml(inputHandler,src);
		} catch (Exception e) {
			if (itemHandler.getTimeOutException()!=null) {
				throw itemHandler.getTimeOutException();
			}
			if (!itemHandler.isStopRequested()) {
				throw new SenderException(errorMessage,e);
			}
		}
		
		if (transformerErrorListener!=null) {
			TransformerException tex = transformerErrorListener.getFatalTransformerException();
			if (tex!=null) {
				throw new SenderException(errorMessage,tex);
			}
			IOException iox = transformerErrorListener.getFatalIOException();
			if (iox!=null) {
				throw new SenderException(errorMessage,iox);
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

	@IbisDoc({"2", "Element name (not an XPath-expression), qualified via attribute <code>namespaceDefs</code>, used to determine the 'root' of elements to be iterated over, i.e. the root of the set of child elements. "
			+ "When empty, the pipe will iterate over each direct child element of the root", ""})
	public void setContainerElement(String containerElement) {
		this.containerElement = containerElement;
	}
	public String getContainerElement() {
		return containerElement;
	}

	@IbisDoc({"3", "Element name (not an XPath-expression), qualified via attribute <code>namespaceDefs</code>, used to determine the type of elements to be iterated over, i.e. the element name of each of the child elements. "
			+ "When empty, the pipe will iterate over any direct child element of the root or specified containerElement", ""})
	public void setTargetElement(String targetElement) {
		this.targetElement = targetElement;
	}
	public String getTargetElement() {
		return targetElement;
	}

	@IbisDoc({"4", "XPath-expression used to determine the set of elements to be iterated over, i.e. the set of child elements. When empty, the effective value is /*/*, i.e. the pipe will iterate over each direct child element of the root. "
		+"Be aware that memory consumption appears to increase with file size when this attribute is used. When possible, use containerElement and/or targetElement instead.", ""})
	public void setElementXPathExpression(String string) {
		elementXPathExpression = string;
	}
	public String getElementXPathExpression() {
		return elementXPathExpression;
	}

	@IbisDoc({"5", "Characterset used for reading file or inputstream, only used when {@link #setProcessFile(boolean) processFile} is <code>true</code>, or the input is of type InputStream", "utf-8"})
	public void setCharset(String string) {
		charset = string;
	}
	public String getCharset() {
		return charset;
	}

	@IbisDoc({"6", "When set to <code>2</code> xslt processor 2.0 (net.sf.saxon) will be used, supporting XPath 2.0, otherwise xslt processor 1.0 (org.apache.xalan), supporting XPath 1.0. N.B. Be aware that setting this other than 1 might cause the input file being read as a whole in to memory, as Xslt Streaming is currently only supported by the XsltProcessor that is used for xsltVersion=1", "1"})
	public void setXsltVersion(int xsltVersion) {
		this.xsltVersion=xsltVersion;
	}
	public int getXsltVersion() {
		return xsltVersion;
	}

	@IbisDoc({"7", "Deprecated: when set <code>true</code> xslt processor 2.0 (net.sf.saxon) will be used, otherwise xslt processor 1.0 (org.apache.xalan)", "false"})
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
	
	@IbisDoc({"8", "When set <code>true</code> namespaces (and prefixes) in the input message are removed before transformation", "true"})
	public void setRemoveNamespaces(boolean b) {
		removeNamespaces = b;
	}
	public boolean isRemoveNamespaces() {
		return removeNamespaces;
	}

	@Override
	public void setThreadLifeCycleEventListener(ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener) {
		this.threadLifeCycleEventListener=threadLifeCycleEventListener;
	}

}
