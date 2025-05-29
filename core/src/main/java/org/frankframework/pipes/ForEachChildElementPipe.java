/*
   Copyright 2013, 2019 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.pipes;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.SuppressKeys;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.Category;
import org.frankframework.jta.IThreadConnectableTransactionManager;
import org.frankframework.parameters.IParameter;
import org.frankframework.stream.Message;
import org.frankframework.threading.IThreadCreator;
import org.frankframework.threading.ThreadConnector;
import org.frankframework.threading.ThreadLifeCycleEventListener;
import org.frankframework.util.StringUtil;
import org.frankframework.util.TransformerErrorListener;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.XmlEncodingUtils;
import org.frankframework.util.XmlUtils;
import org.frankframework.xml.AbstractExceptionCatchingFilter;
import org.frankframework.xml.FullXmlFilter;
import org.frankframework.xml.IXmlDebugger;
import org.frankframework.xml.NamespaceRemovingFilter;
import org.frankframework.xml.NodeSetFilter;
import org.frankframework.xml.SaxException;
import org.frankframework.xml.TransformerFilter;
import org.frankframework.xml.XmlWriter;

/**
 * Sends a message to a Sender for each child element of the input XML.
 * The input can be a String containing XML, a filename (set processFile true), an InputStream, or a Reader.
 *
 * @ff.parameters All parameters will be applied to the XSLT if an elementXPathExpression is specified.
 *
 * @author Gerrit van Brakel
 * @since 4.6.1
 */
@Category(Category.Type.BASIC)
public class ForEachChildElementPipe extends StringIteratorPipe implements IThreadCreator {

	public static final int DEFAULT_XSLT_VERSION = 1; // currently only Xalan supports XSLT Streaming

	private @Getter String containerElement;
	private @Getter String targetElement;
	private @Getter String elementXPathExpression = null;
	private @Getter int xsltVersion = DEFAULT_XSLT_VERSION;
	private @Getter boolean removeNamespaces = true;

	private TransformerPool extractElementsTp = null;
	private @Setter ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener;
	private @Setter IThreadConnectableTransactionManager<?,?> txManager;
	private @Getter @Setter IXmlDebugger xmlDebugger;

	private boolean createThreadConnectorForXsltStreaming;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		try {
			if (StringUtils.isNotEmpty(getElementXPathExpression())) {
				if (getXsltVersion() == 0) {
					setXsltVersion(DEFAULT_XSLT_VERSION);
				}
				extractElementsTp = TransformerPool.getInstance(makeEncapsulatingXslt("root", getElementXPathExpression(), getXsltVersion(), getNamespaceDefs()), getXsltVersion(), this);

				if (XmlUtils.isXsltStreamingByDefault() && getXsltVersion() != DEFAULT_XSLT_VERSION) {
					ConfigurationWarnings.add(this, log, "XsltProcessor xsltVersion [" + getXsltVersion() + "] currently does not support streaming XSLT, might lead to memory problems for large messages", SuppressKeys.XSLT_STREAMING_SUPRESS_KEY);
				}
			}

			// ThreadConnector should only be created when there is an XPath expression to iterate over elements, and XSLT Streaming is enabled.
			// Otherwise there can be a transaction-deadlock as creating the ThreadConnector suspends the current transaction, and the ForEachChildElementPipe
			// might be delegating work to a sender that does database-work.
			// Cache the value here, so that we don't have to repeat the check on every invocation.
			createThreadConnectorForXsltStreaming = StringUtils.isNotEmpty(getElementXPathExpression()) && XmlUtils.isXsltStreamingByDefault();
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException("elementXPathExpression ["+getElementXPathExpression()+"]",e);
		}
		if (StringUtils.isNotEmpty(getTargetElement()) && (getTargetElement().contains("/"))) {
			throw new ConfigurationException("targetElement ["+getTargetElement()+"] should not contain '/', only a single element name");
		}
		if (StringUtils.isNotEmpty(getContainerElement()) && (getContainerElement().contains("/"))) {
			throw new ConfigurationException("containerElement ["+getTargetElement()+"] should not contain '/', only a single element name");
		}
	}

	@Override
	public void start() {
		if (extractElementsTp != null) {
			extractElementsTp.open();
		}

		super.start();
	}

	@Override
	public void stop()   {
		if (extractElementsTp != null) {
			extractElementsTp.close();
		}
		super.stop();
	}

	protected String makeEncapsulatingXslt(String rootElementName, String xpathExpression, int xsltVersion, String namespaceDefs) {
		String paramsString = getParameterList().stream()
				.map(IParameter::getName)
				.map(paramName -> "<xsl:param name=\"" + paramName + "\"/>")
				.collect(Collectors.joining());
		String namespaceClause = XmlUtils.getNamespaceClause(namespaceDefs);
		return
				"""
						<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="%d.0" xmlns:xalan="http://xml.apache.org/xslt">
							<xsl:output method="xml" omit-xml-declaration="yes"/>
							<xsl:strip-space elements="*"/>
							%s
							<xsl:template match="/">
								<xsl:element %s name="%s">
									<xsl:copy-of select="%s"/>
								</xsl:element>
							</xsl:template>
						</xsl:stylesheet>
						""".formatted(xsltVersion, paramsString, namespaceClause, rootElementName, XmlEncodingUtils.encodeChars(xpathExpression));
	}

	private static class ItemCallbackCallingHandler extends NodeSetFilter {
		private final ItemCallback callback;

		private XmlWriter xmlWriter;

		private StopReason stopReason=null;

		public ItemCallbackCallingHandler(ItemCallback callback) {
			super(null, null, false, false, null);
			setContentHandler(xmlWriter);
			this.callback=callback;
		}

		@Override
		public void startDocument() throws SAXException {
			try {
				callback.startIterating();
			} catch (SenderException | TimeoutException | IOException e) {
				throw new SaxException(e);
			}
			super.startDocument();

		}

		@Override
		public void endDocument() throws SAXException {
			super.endDocument();
			try {
				callback.endIterating();
			} catch (SenderException | IOException | TimeoutException e) {
				throw new SaxException(e);
			}
		}


		/*
		 * Nodes are the elements that are iterated over.
		 */
		@Override
		public void startNode(String uri, String localName, String qName) throws SAXException {
			xmlWriter= new XmlWriter();
			setContentHandler(xmlWriter);
			xmlWriter.startDocument();
		}

		@Override
		public void endNode(String uri, String localName, String qName) throws SAXException {
			xmlWriter.endDocument();
			try {
				stopReason = callback.handleItem(xmlWriter.toString());
			} catch (TimeoutException e) {
				throw new SaxTimeoutException(e);
			} catch (Exception e) {
				throw new SaxException(e);
			}
			checkInterrupt();
		}

		private void checkInterrupt() throws SAXException {
			if (Thread.currentThread().isInterrupted()) {
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
		public void startDTD(String arg0, String arg1, String arg2) {
//			System.out.println("startDTD");
		}

		@Override
		public void endDTD() throws SAXException {
//			System.out.println("endDTD");
		}


		@Override
		public void startEntity(String arg0) {
//			System.out.println("startEntity ["+arg0+"]");
		}
		@Override
		public void endEntity(String arg0) {
//			System.out.println("endEntity ["+arg0+"]");
		}

		public boolean isStopRequested() {
			return stopReason != null;
		}

	}

	private static class StopSensor extends FullXmlFilter {

		private final ItemCallbackCallingHandler itemHandler;

		public StopSensor(ItemCallbackCallingHandler itemHandler, ContentHandler handler) {
			super(handler);
			this.itemHandler=itemHandler;
		}
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			if (itemHandler.isStopRequested()) {
				throw new SaxAbortException("stop requested");
			}
		}
	}

	private static class HandlerRecord {
		private ItemCallbackCallingHandler itemHandler;
		private ContentHandler inputHandler;
		private String errorMessage="Could not parse input";
		private TransformerErrorListener transformerErrorListener=null;
	}

	private void createHandler(HandlerRecord result, ThreadConnector<?> threadConnector, Message input, PipeLineSession session, ItemCallback callback) throws TransformerConfigurationException {
		result.itemHandler = new ItemCallbackCallingHandler(callback);
		result.inputHandler=result.itemHandler;

		if (getXmlDebugger()!=null && (StringUtils.isNotEmpty(getContainerElement()) || StringUtils.isNotEmpty(getTargetElement()) || getExtractElementsTp()!=null)) {
			String containerElementString = StringUtils.isNotEmpty(getContainerElement()) ? "filter to containerElement '"+getContainerElement()+"'" : null;
			String targetElementString = StringUtils.isNotEmpty(getTargetElement()) ? "filter to targetElement '"+getTargetElement()+"'" :null;
			String xpathString = getExtractElementsTp()!=null ? "filter XPath (with temporary root element) '"+getElementXPathExpression()+"'": null;
			String label = "XML after preprocessing: " + StringUtil.concat(", ",containerElementString, targetElementString, xpathString);
			result.inputHandler=getXmlDebugger().inspectXml(session, label, result.inputHandler);
		}

		if (isRemoveNamespaces()) {
			result.inputHandler = new NamespaceRemovingFilter(result.inputHandler);
		}

		if (getExtractElementsTp()!=null) {
			log.debug("transforming input to obtain list of elements using xpath [{}]", getElementXPathExpression());
			TransformerFilter transformerFilter = getTransformerFilter(result, threadConnector);
			if (!getParameterList().isEmpty()) {
				try {
					XmlUtils.setTransformerParameters(transformerFilter.getTransformer(), getParameterList().getValues(input, session).getValueMap());
				} catch (ParameterException | IOException e) {
					throw new TransformerConfigurationException("Cannot apply parameters", e);
				}
			}
			result.inputHandler=transformerFilter;
			result.transformerErrorListener=(TransformerErrorListener)transformerFilter.getErrorListener();
			result.errorMessage="Could not process list of elements using xpath ["+getElementXPathExpression()+"]";
		}
		if (StringUtils.isNotEmpty(getTargetElement())) {
			result.inputHandler = new NodeSetFilter(XmlUtils.getNamespaceMap(getNamespaceDefs()), getTargetElement(), true, true, result.inputHandler);
		}
		if (StringUtils.isNotEmpty(getContainerElement())) {
			result.inputHandler = new NodeSetFilter(XmlUtils.getNamespaceMap(getNamespaceDefs()), getContainerElement(), false, true, result.inputHandler);
		}

		result.inputHandler = new StopSensor(result.itemHandler, result.inputHandler);

		result.inputHandler = new AbstractExceptionCatchingFilter(result.inputHandler) {
			@Override
			protected void handleException(Exception e) throws SAXException {
				if (e instanceof SaxTimeoutException exception) {
					throw exception;
				}
				if (result.itemHandler.isStopRequested()) {
					if (e instanceof SaxAbortException exception) {
						throw exception;
					}
					throw new SaxAbortException(e);
				}
				// Xalan rethrows any caught exception with the message, but without the cause.
				// For improved diagnosability of error situations, rethrow the original exception, where applicable.
				if (result.transformerErrorListener!=null) {
					TransformerException tex = result.transformerErrorListener.getFatalTransformerException();
					if (tex!=null) {
						throw new SaxException(result.errorMessage,tex);
					}
					IOException iox = result.transformerErrorListener.getFatalIOException();
					if (iox!=null) {
						throw new SaxException(result.errorMessage,iox);
					}
				}
				throw new SaxException(result.errorMessage,e);
			}
		};
	}

	private TransformerFilter getTransformerFilter(HandlerRecord result, ThreadConnector<?> threadConnector) throws TransformerConfigurationException {
		if (threadConnector != null) {
			return getExtractElementsTp().getTransformerFilter(threadConnector, result.inputHandler);
		} else {
			return getExtractElementsTp().getTransformerFilter(result.inputHandler);
		}
	}

	@Override
	protected StopReason iterateOverInput(Message input, PipeLineSession session, Map<String,Object> threadContext, ItemCallback callback) throws SenderException, TimeoutException {
		InputSource src;

		try {
			src = input.asInputSource();
		} catch (IOException e) {
			throw new SenderException("could not get InputSource",e);
		}

		HandlerRecord handlerRecord = new HandlerRecord();
		try (ThreadConnector<?> threadConnector = createThreadConnectorForXsltStreaming ? new ThreadConnector<>(this, "iterateOverInput", threadLifeCycleEventListener, txManager, session) : null) {
			try {
				createHandler(handlerRecord, threadConnector, input, session, callback);
			} catch (TransformerException e) {
				throw new SenderException(handlerRecord.errorMessage, e);
			}

			try {
				XmlUtils.parseXml(src, handlerRecord.inputHandler);
			} catch (Exception e) {
				try {
					if (e instanceof SaxTimeoutException) {
						if (e.getCause() instanceof TimeoutException timeoutException) {
							throw timeoutException;
						}
						throw new TimeoutException(e);
					}
					if (!(e instanceof SaxAbortException)) {
						throw new SenderException(e);
					}
				} finally {
					try {
						handlerRecord.inputHandler.endDocument();
					} catch (Exception e2) {
						log.warn("Exception in endDocument()", e2);
					}
				}
			}
		}
		return handlerRecord.itemHandler.stopReason;
	}

	protected TransformerPool getExtractElementsTp() {
		return extractElementsTp;
	}

	/**
	 * Element name (not an XPath expression), qualified via attribute <code>namespaceDefs</code>, used to determine the 'root' of elements to be iterated over, i.e., the root of the set of child elements.
	 * When empty, the pipe will iterate over each direct child element of the root.
	 */
	public void setContainerElement(String containerElement) {
		this.containerElement = containerElement;
	}

	/**
	 * Element name (not an XPath expression), qualified via attribute <code>namespaceDefs</code>, used to determine the type of elements to be iterated over, i.e. the element name of each of the child elements.
	 * When empty, the pipe will iterate over any direct child element of the root or specified <code>containerElement</code>.
	 */
	public void setTargetElement(String targetElement) {
		this.targetElement = targetElement;
	}

	/**
	 * XPath expression used to determine the set of elements to be iterated over, i.e., the set of child elements. When empty, the effective value is \/*\/*, i.e., the pipe will iterate over each direct child element of the root.
	 * Be aware that memory consumption appears to increase with file size when this attribute is used. When possible, use containerElement and/or targetElement instead.
	 */
	public void setElementXPathExpression(String string) {
		elementXPathExpression = string;
	}

	/**
	 * If set to <code>2</code> or <code>3</code>, a Saxon (net.sf.saxon) XSLT processor 2.0 or 3.0 will be used, supporting XPath 2.0 or 3.0 respectively. Otherwise, an XSLT processor 1.0 (org.apache.xalan), supporting XPath 1.0, will be used. N.B. Be aware that setting this to a value other than 1 might cause the input file to be read in its entirety into memory, as XSLT Streaming is currently only supported by the XSLT Processor that is used for xsltVersion=1.
	 * @ff.default 1
	 */
	public void setXsltVersion(int xsltVersion) {
		this.xsltVersion=xsltVersion;
	}

	/** If {@code true}, namespaces (and prefixes) are removed from the items just before forwarding them to the sender. N.B. This takes place <strong>after</strong> the transformation for <code>elementXPathExpression</code> if that is specified. */
	public void setRemoveNamespaces(boolean b) {
		removeNamespaces = b;
	}

	/**
	 * SAXException thrown to signal that the consumer of a stream does not want to receive more of it.
	 */
	public static class SaxAbortException extends SaxException {
		public SaxAbortException(String message) {
			super(message);
		}
		public SaxAbortException(Exception cause) {
			super(cause);
		}
	}

	/**
	 * SAXException thrown to signal that a timeout occurred in consuming the stream.
	 */
	public static class SaxTimeoutException extends SaxException {
		public SaxTimeoutException(Exception cause) {
			super(cause);
		}
	}
}
