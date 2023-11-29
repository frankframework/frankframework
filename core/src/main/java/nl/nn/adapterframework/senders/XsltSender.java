/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package nl.nn.adapterframework.senders;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.doc.SupportsOutputStreaming;
import nl.nn.adapterframework.jta.IThreadConnectableTransactionManager;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.Parameter.ParameterType;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.IThreadCreator;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.PathMessage;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.ThreadConnector;
import nl.nn.adapterframework.stream.ThreadLifeCycleEventListener;
import nl.nn.adapterframework.stream.xml.XmlTap;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.TransformerPool.OutputType;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.PrettyPrintFilter;
import nl.nn.adapterframework.xml.SkipEmptyTagsFilter;
import nl.nn.adapterframework.xml.TransformerFilter;
import nl.nn.adapterframework.xml.XmlWriter;

/**
 * Perform an XSLT transformation with a specified stylesheet or XPath-expression.
 *
 * @ff.parameters any parameters defined on the sender will be applied to the created transformer
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
@SupportsOutputStreaming
public class XsltSender extends SenderWithParametersBase implements IThreadCreator {

	public final OutputType DEFAULT_OUTPUT_METHOD=OutputType.XML;
	public final OutputType DEFAULT_XPATH_OUTPUT_METHOD=OutputType.TEXT;
	public final boolean DEFAULT_INDENT=false; // some existing ibises expect default for indent to be false
	public final boolean DEFAULT_OMIT_XML_DECLARATION=false;

	private @Getter String styleSheetName;
	private @Getter String styleSheetNameSessionKey=null;
	private @Getter String xpathExpression=null;
	private @Getter String namespaceDefs = null;
	private @Getter OutputType outputType=null;
	private @Getter Boolean omitXmlDeclaration;
	private @Getter Boolean indentXml=null;
	private @Getter Boolean disableOutputEscaping=null;
	private @Getter boolean handleLexicalEvents=false;
	private @Getter boolean removeNamespaces=false;
	private @Getter boolean skipEmptyTags=false;
	private @Getter int xsltVersion=0; // set to 0 for auto detect.
	private @Getter boolean debugInput = false;

	private TransformerPool transformerPool;

	private Map<String, TransformerPool> dynamicTransformerPoolMap;
	private int transformerPoolMapSize = 100;

	protected @Setter ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener;
	protected @Setter IThreadConnectableTransactionManager txManager;
	private @Getter Boolean streamingXslt = null;

	/**
	 * The <code>configure()</code> method instantiates a transformer for the specified
	 * XSL. If the stylesheetName cannot be accessed, a ConfigurationException is thrown.
	 */
	@Override
	public void configure() throws ConfigurationException {
		parameterNamesMustBeUnique = true;
		super.configure();

		if(streamingXslt == null) streamingXslt = AppConstants.getInstance(getConfigurationClassLoader()).getBoolean(XmlUtils.XSLT_STREAMING_BY_DEFAULT_KEY, false);
		dynamicTransformerPoolMap = Collections.synchronizedMap(new LRUMap(transformerPoolMapSize));

		if(StringUtils.isNotEmpty(getXpathExpression()) && getOutputType()==null) {
			setOutputType(DEFAULT_XPATH_OUTPUT_METHOD);
		}
		if(StringUtils.isNotEmpty(getStyleSheetName()) || StringUtils.isNotEmpty(getXpathExpression())) {
			Boolean omitXmlDeclaration = getOmitXmlDeclaration();
			if (omitXmlDeclaration==null) {
				omitXmlDeclaration=true;
			}
			transformerPool = TransformerPool.configureTransformer0(this, getNamespaceDefs(), getXpathExpression(), getStyleSheetName(), getOutputType(), !omitXmlDeclaration, getParameterList(), getXsltVersion());
		}
		else if(StringUtils.isEmpty(getStyleSheetNameSessionKey())) {
			throw new ConfigurationException(getLogPrefix()+" one of xpathExpression, styleSheetName or styleSheetNameSessionKey must be specified");
		}

		if (getXsltVersion()>=2) {
			ParameterList parameterList = getParameterList();
			if (parameterList!=null) {
				for (int i=0; i<parameterList.size(); i++) {
					Parameter parameter = parameterList.getParameter(i);
					if (parameter.getType()==ParameterType.NODE) {
						throw new ConfigurationException(getLogPrefix() + "type '"+ParameterType.NODE+" is not permitted in combination with XSLT 2.0, use type '"+ParameterType.DOMDOC+"'");
					}
				}
			}
		}
	}

	@Override
	public void open() throws SenderException {
		super.open();

		if (transformerPool!=null) {
			try {
				transformerPool.open();
			} catch (Exception e) {
				throw new SenderException(getLogPrefix()+"cannot start TransformerPool", e);
			}
		}
	}

	@Override
	public void close() throws SenderException {
		super.close();

		if (transformerPool!=null) {
			transformerPool.close();
		}

		if (dynamicTransformerPoolMap!=null && !dynamicTransformerPoolMap.isEmpty()) {
			for(TransformerPool tp : dynamicTransformerPoolMap.values()) {
				tp.close();
			}
		}
	}


	protected ContentHandler filterInput(ContentHandler input, PipeLineSession session) {
		return input; // TODO might be necessary to do something about namespaceaware
	}


	protected boolean isDisableOutputEscaping(TransformerPool poolToUse) throws TransformerException, IOException {
		Boolean disableOutputEscaping = getDisableOutputEscaping();
		if (log.isTraceEnabled()) log.trace("Configured disableOutputEscaping ["+disableOutputEscaping+"]");
		if (disableOutputEscaping == null) {
			disableOutputEscaping = poolToUse.getDisableOutputEscaping();
			if (log.isTraceEnabled()) log.trace("Detected disableOutputEscaping ["+disableOutputEscaping+"]");
		}
		if (disableOutputEscaping == null) {
			disableOutputEscaping = false;
			if (log.isTraceEnabled()) log.trace("Default disableOutputEscaping ["+disableOutputEscaping+"]");
		}
		return disableOutputEscaping;
	}

	protected TransformerPool getTransformerPoolToUse(PipeLineSession session) throws SenderException, ConfigurationException {
		TransformerPool poolToUse = transformerPool;
		if(StringUtils.isNotEmpty(styleSheetNameSessionKey)) {
			String styleSheetNameToUse = session.getString(styleSheetNameSessionKey);
			if (StringUtils.isNotEmpty(styleSheetNameToUse )) {
				if(!dynamicTransformerPoolMap.containsKey(styleSheetNameToUse)) {
					dynamicTransformerPoolMap.put(styleSheetNameToUse, poolToUse = TransformerPool.configureTransformer(this, null, null, styleSheetNameToUse, null, true, getParameterList()));
					poolToUse.open();
				} else {
					poolToUse = dynamicTransformerPoolMap.get(styleSheetNameToUse);
				}
			}
			if (poolToUse == null) {
				throw new SenderException("no XSLT stylesheet found from styleSheetNameSessionKey ["+styleSheetNameSessionKey+"], and neither one statically configured");
			}
		}
		return poolToUse;
	}

	protected ContentHandler createHandler(Message input, ThreadConnector threadConnector, PipeLineSession session, TransformerPool poolToUse, ContentHandler handler, File tempFile) throws StreamingException {
		try {
			ParameterValueList pvl=null;
			if (paramList!=null) {
				pvl = paramList.getValues(input, session);
			}

			OutputType outputType = getOutputType();
			if (log.isTraceEnabled()) log.trace("Configured outputmethod ["+outputType+"]");
			if (outputType == null) {
				String parsedOutputType = poolToUse.getOutputMethod();
				if (StringUtils.isNotEmpty(parsedOutputType)) {
					outputType = EnumUtils.parse(OutputType.class, parsedOutputType);
				}
				if (log.isTraceEnabled()) log.trace("Detected outputmethod ["+parsedOutputType+"]");
			}
			if (outputType == null) {
				outputType = DEFAULT_OUTPUT_METHOD;
				if (log.isTraceEnabled()) log.trace("Default outputmethod ["+outputType+"]");
			}

			boolean disableOutputEscaping = isDisableOutputEscaping(poolToUse);

			Boolean indentXml = getIndentXml();
			if (log.isTraceEnabled()) log.trace("Configured indentXml ["+indentXml+"]");
			if (indentXml==null) {
				indentXml = poolToUse.getIndent();
				if (log.isTraceEnabled()) log.trace("Detected indentXml ["+indentXml+"]");
			}
			if (indentXml==null) {
				indentXml = DEFAULT_INDENT;
				if (log.isTraceEnabled()) log.trace("Default indentXml ["+indentXml+"]");
			}

			if (handler == null || disableOutputEscaping) {
				XmlWriter xmlWriter = new XmlWriter(Files.newBufferedWriter(tempFile.toPath()));
				xmlWriter.setCloseWriterOnEndDocument(true);
				Boolean omitXmlDeclaration = getOmitXmlDeclaration();
				if (log.isTraceEnabled()) log.trace("Configured omitXmlDeclaration [" + omitXmlDeclaration + "]");
				if (outputType == OutputType.XML) {
					if (omitXmlDeclaration == null) {
						omitXmlDeclaration = poolToUse.getOmitXmlDeclaration();
						if (log.isTraceEnabled()) log.trace("Detected omitXmlDeclaration [" + omitXmlDeclaration + "]");
						if (omitXmlDeclaration == null) {
							omitXmlDeclaration = DEFAULT_OMIT_XML_DECLARATION;
							if (log.isTraceEnabled()) log.trace("Default omitXmlDeclaration [" + omitXmlDeclaration + "]");
						}
					}
					xmlWriter.setIncludeXmlDeclaration(!omitXmlDeclaration);
					if (indentXml) {
						xmlWriter.setNewlineAfterXmlDeclaration(true);
					}
				} else {
					xmlWriter.setTextMode(true);
				}
				handler = xmlWriter;
			}

			if (indentXml) {
				handler = new PrettyPrintFilter(handler);
			}
			if (isSkipEmptyTags()) {
				handler = new SkipEmptyTagsFilter(handler);
			}

			TransformerFilter mainFilter = poolToUse.getTransformerFilter(threadConnector, handler, isRemoveNamespaces(), isHandleLexicalEvents());
			if (pvl!=null) {
				XmlUtils.setTransformerParameters(mainFilter.getTransformer(), pvl.getValueMap());
			}
			handler=filterInput(mainFilter, session);

			return handler;
		} catch (Exception e) {
			throw new StreamingException(getLogPrefix()+"Exception on creating transformerHandler chain", e);
		}
	}


	protected XMLReader getXmlReader(PipeLineSession session, ContentHandler handler) throws ParserConfigurationException, SAXException {
		return XmlUtils.getXMLReader(handler);
	}


	/**
	 * alternative implementation of send message, that should do the same as the original, but reuses the streaming content handler
	 */
	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException {
		if (message == null) {
			throw new SenderException(getLogPrefix() + "got null input");
		}

		try (ThreadConnector threadConnector = streamingXslt ? new ThreadConnector(this, "sendMessage", threadLifeCycleEventListener, txManager, session) : null) {
			TransformerPool poolToUse = getTransformerPoolToUse(session);
			File tempFile = FileUtils.createTempFile();
			ContentHandler handler = createHandler(message, threadConnector, session, poolToUse, null, tempFile);
			if (isDebugInput() && log.isDebugEnabled()) {
				handler = new XmlTap(handler) {
					@Override
					public void endDocument() throws SAXException {
						super.endDocument();
						log.debug(getLogPrefix() + " xml input [" + getWriter() + "]");
					}
				};
			}
			XMLReader reader = getXmlReader(session, handler);
			InputSource source = message.asInputSource();
			reader.parse(source);
			return new SenderResult(PathMessage.asTemporaryMessage(tempFile.toPath()));
		} catch (Exception e) {
			throw new SenderException(getLogPrefix() + "Cannot transform input", e);
		}
	}

	/**
	 * If true, then this sender will process the XSLT while streaming in a different thread. Can be used to switch streaming off for debugging purposes
	 * @ff.default set by appconstant xslt.streaming.default
	 */
	public void setStreamingXslt(Boolean streamingActive) {
		this.streamingXslt = streamingActive;
	}

	@Override
	public boolean isSynchronous() {
		return true;
	}

	/** Location of stylesheet to apply to the input message */
	public void setStyleSheetName(String stylesheetName){
		this.styleSheetName=stylesheetName;
	}

	/** Session key to retrieve stylesheet location. Overrides stylesheetName or xpathExpression attribute */
	public void setStyleSheetNameSessionKey(String newSessionKey) {
		styleSheetNameSessionKey = newSessionKey;
	}

	/**
	 * Size of cache of stylesheets retrieved from styleSheetNameSessionKey
	 * @ff.default 100
	 */
	public void setStyleSheetCacheSize(int size) {
		transformerPoolMapSize = size;
	}

	/** Alternatively: XPath-expression to create stylesheet from */
	public void setXpathExpression(String string) {
		xpathExpression = string;
	}

	/**
	 * Omit the XML declaration on top of the output. If not set, the value specified in the stylesheet is followed
	 * @ff.default false, if not set in stylesheet
	 */
	public void setOmitXmlDeclaration(Boolean b) {
		omitXmlDeclaration = b;
	}

	/**
	 * If set <code>true</code>, any output is reparsed before being handled as XML again. If not set, the stylesheet is searched for <code>@disable-output-escaping='yes'</code> and the value is set accordingly
	 * @ff.default false, if not set in stylesheet
	 */
	public void setDisableOutputEscaping(Boolean b) {
		disableOutputEscaping = b;
	}

	/**
	 * Namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions. For some other use cases (NOT xpathExpression), one entry can be without a prefix, that will define the default namespace.
	 * If left empty, an the xpathExpression will match any namespace
	 */
	public void setNamespaceDefs(String namespaceDefs) {
		this.namespaceDefs = namespaceDefs;
	}

	/**
	 * For xpathExpression only
	 * @ff.default text
	 */
	public void setOutputType(OutputType string) {
		outputType = string;
	}

	/**
	 * If set <code>true</code>, result is pretty-printed. If not set, the value specified in the stylesheet is followed
	 * @ff.default false, if not set in stylesheet
	 */
	public void setIndentXml(Boolean b) {
		indentXml = b;
	}

	/**
	 * If set <code>true</code> namespaces (and prefixes) in the input message are removed before transformation
	 * @ff.default false
	 */
	public void setRemoveNamespaces(boolean b) {
		removeNamespaces = b;
	}

	/**
	 * If set <code>true</code>, the transformer is enabled to handle lexical events, allowing it for example to process comments and to distinghuish CDATA from escaped text.
	 * Beware that this option can cause spurious NullPointerExceptions due to a race condition in streaming XSLT 1.0 processing in Xalan 2.7.2
	 * @ff.default false
	 */
	public void setHandleLexicalEvents(boolean b) {
		handleLexicalEvents = b;
	}

	/**
	 * If set <code>true</code> empty tags in the output are removed after transformation
	 * @ff.default false
	 */
	public void setSkipEmptyTags(boolean b) {
		skipEmptyTags = b;
	}

	/**
	 * If set to <code>2</code> or <code>3</code> a Saxon (net.sf.saxon) xslt processor 2.0 or 3.0 respectively will be used, otherwise xslt processor 1.0 (org.apache.xalan). <code>0</code> will auto detect
	 * @ff.default 0
	 */
	public void setXsltVersion(int xsltVersion) {
		this.xsltVersion=xsltVersion;
	}

	/**
	 * If set <code>true</code> the input is written to the log file, at DEBUG level
	 * @ff.default false
	 */
	public void setDebugInput(boolean debugInput) {
		this.debugInput = debugInput;
	}

	/**
	 * If set <code>true</code> xslt processor 2.0 (net.sf.saxon) will be used, otherwise xslt processor 1.0 (org.apache.xalan)
	 * @ff.default false
	 */
	/**
	 * @deprecated Please remove setting of xslt2, it will be auto detected. Or use xsltVersion.
	 */
	@Deprecated
	@ConfigurationWarning("It's value is now auto detected. If necessary, replace with a setting of xsltVersion")
	public void setXslt2(boolean b) {
		xsltVersion=b?2:1;
	}

}
