/*
   Copyright 2013, 2016 Nationale-Nederlanden

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

import java.util.Collections;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.stream.IOutputStreamingSupport;
import nl.nn.adapterframework.stream.IThreadCreator;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.MessageOutputStreamCap;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.StreamingSenderBase;
import nl.nn.adapterframework.stream.ThreadLifeCycleEventListener;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.NamespaceRemovingFilter;
import nl.nn.adapterframework.xml.PrettyPrintFilter;
import nl.nn.adapterframework.xml.SkipEmptyTagsFilter;
import nl.nn.adapterframework.xml.TransformerFilter;
import nl.nn.adapterframework.xml.XmlWriter;

/**
 * Perform an XSLT transformation with a specified stylesheet or XPath-expression.
 *
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link Parameter param}</td><td>any parameters defined on the sender will be applied to the created transformer</td></tr>
 * </table>
 * </p>
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class XsltSender extends StreamingSenderBase implements IThreadCreator {
	private static final String ENVIRONMENT = "environment";

	public final String DEFAULT_OUTPUT_METHOD="xml";
	public final boolean DEFAULT_INDENT=false; // some existing ibises expect default for indent to be false 
	public final boolean DEFAULT_OMIT_XML_DECLARATION=false; 
	
	private String styleSheetName;
	private String styleSheetNameSessionKey=null;
	private String xpathExpression=null;
	private String namespaceDefs = null; 
	private String outputType=null;
	private Boolean omitXmlDeclaration;
	private Boolean indentXml=null; 
	private boolean removeNamespaces=false;
	private boolean skipEmptyTags=false;
	private int xsltVersion=0; // set to 0 for auto detect.
	private boolean namespaceAware=XmlUtils.isNamespaceAwareByDefault();
	private boolean addEnvironmentParameter=false;
	
	private TransformerPool transformerPool;
	
	private Map<String, TransformerPool> dynamicTransformerPoolMap;
	private int transformerPoolMapSize = 100;

	protected ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener;
	private boolean streamingXslt;


	/**
	 * The <code>configure()</code> method instantiates a transformer for the specified
	 * XSL. If the stylesheetname cannot be accessed, a ConfigurationException is thrown.
	 */
	@Override
	public void configure() throws ConfigurationException {
		ParameterList parameterList = getParameterList();
		if (isAddEnvironmentParameter() && (parameterList == null || parameterList.findParameter(ENVIRONMENT) == null)) {
			Parameter p = new Parameter();
			p.setName(ENVIRONMENT);
			p.setValue(AppConstants.getInstance().getResolvedProperty("otap.stage"));
			addParameter(p);
		}
		super.configure();
		
		streamingXslt = AppConstants.getInstance(getConfigurationClassLoader()).getBoolean("xslt.streaming.default", false);
		dynamicTransformerPoolMap = Collections.synchronizedMap(new LRUMap(transformerPoolMapSize));
		
		if(StringUtils.isNotEmpty(getXpathExpression()) && getOutputType()==null) {
			setOutputType("text");
		}
		if(StringUtils.isNotEmpty(getStyleSheetName()) || StringUtils.isNotEmpty(getXpathExpression())) {
			Boolean omitXmlDeclaration = getOmitXmlDeclaration();
			if (omitXmlDeclaration==null) {
				omitXmlDeclaration=true;
			}
			transformerPool = TransformerPool.configureTransformer0(getLogPrefix(), getConfigurationClassLoader(), getNamespaceDefs(), getXpathExpression(), getStyleSheetName(), getOutputType(), !omitXmlDeclaration, getParameterList(), getXsltVersion());
		}
		else if(StringUtils.isEmpty(getStyleSheetNameSessionKey())) {
			throw new ConfigurationException(getLogPrefix()+" one of xpathExpression, styleSheetName or styleSheetNameSessionKey must be specified");
		}

		if (getXsltVersion()>=2) {
			if (parameterList!=null) {
				for (int i=0; i<parameterList.size(); i++) {
					Parameter parameter = parameterList.getParameter(i);
					if (StringUtils.isNotEmpty(parameter.getType()) && "node".equalsIgnoreCase(parameter.getType())) {
						throw new ConfigurationException(getLogPrefix() + "type \"node\" is not permitted in combination with XSLT 2.0, use type \"domdoc\"");
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


	protected ContentHandler filterInput(ContentHandler input, ParameterResolutionContext prc) {
		if (isRemoveNamespaces()) {
			log.debug(getLogPrefix()+ " providing filter to remove namespaces from input message");
			XMLFilterImpl filter = new NamespaceRemovingFilter();
			filter.setContentHandler(input);
			return filter;
		}
		return input; // TODO might be necessary to do something about namespaceaware
	}
	
	
	@Override
	public MessageOutputStream provideOutputStream(String correlationID, IPipeLineSession session, IOutputStreamingSupport nextProvider) throws StreamingException {
		MessageOutputStream target = nextProvider==null ? null : nextProvider.provideOutputStream(correlationID, session, nextProvider);
		if (target==null) {
			target=new MessageOutputStreamCap(this, nextProvider);
		}
		ContentHandler handler = createHandler(correlationID, null, session, target);
		return new MessageOutputStream(this, handler,target,this,threadLifeCycleEventListener,correlationID);
	}

	protected ContentHandler createHandler(String correlationID, Message input, IPipeLineSession session, MessageOutputStream target) throws StreamingException {
		ContentHandler handler = null;

		try {
			Map<String,Object> parametervalues = null;
			ParameterResolutionContext prc = new ParameterResolutionContext(input, session);
			if (paramList!=null) {
				parametervalues = prc.getValueMap(paramList);
			}

			TransformerPool poolToUse = transformerPool;
			if(StringUtils.isNotEmpty(styleSheetNameSessionKey) && prc.getSession().get(styleSheetNameSessionKey) != null) {
				String styleSheetNameToUse = prc.getSession().get(styleSheetNameSessionKey).toString();
			
				if(!dynamicTransformerPoolMap.containsKey(styleSheetNameToUse)) {
					dynamicTransformerPoolMap.put(styleSheetNameToUse, poolToUse = TransformerPool.configureTransformer(getLogPrefix(), getConfigurationClassLoader(), null, null, styleSheetNameToUse, null, true, getParameterList()));
					poolToUse.open();
				} else {
					poolToUse = dynamicTransformerPoolMap.get(styleSheetNameToUse);
				}
			}
			
			String outputType = getOutputType();
			if (log.isTraceEnabled()) log.trace("Configured outputmethod ["+outputType+"]");
			if (StringUtils.isEmpty(outputType)) {
				outputType = poolToUse.getOutputMethod();
				if (log.isTraceEnabled()) log.trace("Detected outputmethod ["+outputType+"]");
			}
			if (StringUtils.isEmpty(outputType)) {
				outputType = DEFAULT_OUTPUT_METHOD;
				if (log.isTraceEnabled()) log.trace("Default outputmethod ["+outputType+"]");
			}

			Object targetStream = target.asNative();
			
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
			
			Boolean omitXmlDeclaration = getOmitXmlDeclaration();
			if (targetStream instanceof ContentHandler) {
				handler = (ContentHandler)targetStream;
			} else {
				XmlWriter xmlWriter = new XmlWriter(target.asWriter());
				if (log.isTraceEnabled()) log.trace("Configured omitXmlDeclaration ["+omitXmlDeclaration+"]");
				if ("xml".equals(outputType)) {
					if (omitXmlDeclaration==null) {
						omitXmlDeclaration = poolToUse.getOmitXmlDeclaration();
						if (log.isTraceEnabled()) log.trace("Detected omitXmlDeclaration ["+omitXmlDeclaration+"]");
						if (omitXmlDeclaration==null) {
							omitXmlDeclaration=DEFAULT_OMIT_XML_DECLARATION;
							if (log.isTraceEnabled()) log.trace("Default omitXmlDeclaration ["+omitXmlDeclaration+"]");
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
				PrettyPrintFilter indentingFilter = new PrettyPrintFilter();
				indentingFilter.setContentHandler(handler);
				handler=indentingFilter;
			}
			if (isSkipEmptyTags()) {
				SkipEmptyTagsFilter skipEmptyTagsFilter = new SkipEmptyTagsFilter();
				skipEmptyTagsFilter.setContentHandler(handler);
				handler=skipEmptyTagsFilter;
			}
			

			TransformerFilter mainFilter = poolToUse.getTransformerFilter(this, threadLifeCycleEventListener, correlationID, streamingXslt);
			XmlUtils.setTransformerParameters(mainFilter.getTransformer(),parametervalues);
			mainFilter.setContentHandler(handler);
			handler=mainFilter;
			
			handler=filterInput(handler, prc);
			
			return handler;
		} catch (Exception e) {
			//log.warn(getLogPrefix()+"intermediate exception logging",e);
			throw new StreamingException(getLogPrefix()+"Exception on creating transformerHandler chain", e);
		} 
	}
	

	protected XMLReader getXmlReader(ContentHandler handler) throws ParserConfigurationException, SAXException {
		return XmlUtils.getXMLReader(true, false, handler);
	}
	

	/*
	 * alternative implementation of send message, that should do the same as the origial, but reuses the streaming content handler
	 */
	@Override
	public PipeRunResult sendMessage(String correlationID, Message message, ParameterResolutionContext prc, IOutputStreamingSupport nextProvider) throws SenderException {
		if (message==null) {
			throw new SenderException(getLogPrefix()+"got null input");
		}
		try {
			MessageOutputStream target = nextProvider==null ? null : nextProvider.provideOutputStream(correlationID, prc.getSession(), null);
			try (MessageOutputStream outputStream=target!=null?target:new MessageOutputStreamCap(this, nextProvider)) {
				ContentHandler handler = createHandler(correlationID, message, prc.getSession(), outputStream);
				InputSource source = message.asInputSource();
				XMLReader reader = getXmlReader(handler);
				reader.parse(source);
				return outputStream.getPipeRunResult();
			}
		} catch (Exception e) {
			throw new SenderException(getLogPrefix()+"Exception on transforming input", e);
		}
	}
	

	@Override
	public boolean isSynchronous() {
		return true;
	}

	@IbisDoc({"1", "Location of stylesheet to apply to the input message", ""})
	public void setStyleSheetName(String stylesheetName){
		this.styleSheetName=stylesheetName;
	}
	public String getStyleSheetName() {
		return styleSheetName;
	}

	@IbisDoc({"2", "Session key to retrieve stylesheet location. Overrides stylesheetName or xpathExpression attribute", ""})
	public void setStyleSheetNameSessionKey(String newSessionKey) {
		styleSheetNameSessionKey = newSessionKey;
	}
	public String getStyleSheetNameSessionKey() {
		return styleSheetNameSessionKey;
	}

	@IbisDoc({"3", "Size of cache of stylesheets retrieved from styleSheetNameSessionKey", "100"})
	public void setStyleSheetCacheSize(int size) {
		transformerPoolMapSize = size;
	}
	public int getStyleSheetCacheSize() {
		return transformerPoolMapSize;
	}
	
	@IbisDoc({"4", "Alternatively: xpath-expression to create stylesheet from", ""})
	public void setXpathExpression(String string) {
		xpathExpression = string;
	}
	public String getXpathExpression() {
		return xpathExpression;
	}

	@IbisDoc({"5", "omit the xml declaration on top of the output. When not set, the value specified in the stylesheet is followed", "false, if not set in stylesheet"})
	public void setOmitXmlDeclaration(boolean b) {
		omitXmlDeclaration = b;
	}
	public Boolean getOmitXmlDeclaration() { // can return null too
		return omitXmlDeclaration;
	}

	@IbisDoc({"6", "Namespace defintions for xpathExpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions. For some use other cases (NOT xpathExpression), one entry can be without a prefix, that will define the default namespace.", ""})
	public void setNamespaceDefs(String namespaceDefs) {
		this.namespaceDefs = namespaceDefs;
	}
	public String getNamespaceDefs() {
		return namespaceDefs;
	}

	@IbisDoc({"7", "For xpathExpression only: either 'text' or 'xml'.", "text"})
	public void setOutputType(String string) {
		outputType = string;
	}
	public String getOutputType() {
		return outputType;
	}

	@IbisDoc({"8", "when set <code>true</code>, result is pretty-printed. When not set, the value specified in the stylesheet is followed", "false, if not set in stylesheet"})
	public void setIndentXml(boolean b) {
		indentXml = b;
	}
	public Boolean getIndentXml() { // can return null too
		return indentXml;
	}

	@IbisDoc({"9", "when set <code>true</code> namespaces (and prefixes) in the input message are removed before transformation", "false"})
	public void setRemoveNamespaces(boolean b) {
		removeNamespaces = b;
	}
	public boolean isRemoveNamespaces() {
		return removeNamespaces;
	}

	@IbisDoc({"10", "when set <code>true</code> empty tags in the output are removed after transformation", "false"})
	public void setSkipEmptyTags(boolean b) {
		skipEmptyTags = b;
	}
	public boolean isSkipEmptyTags() {
		return skipEmptyTags;
	}

	@IbisDoc({"11", "when set to <code>2</code> xslt processor 2.0 (net.sf.saxon) will be used, otherwise xslt processor 1.0 (org.apache.xalan). <code>0</code> will auto detect", "0"})
	public void setXsltVersion(int xsltVersion) {
		this.xsltVersion=xsltVersion;
	}
	public int getXsltVersion() {
		return xsltVersion;
	}

	@IbisDoc({"12", "", "true"})
	public void setNamespaceAware(boolean b) {
		namespaceAware = b;
	}
	public boolean isNamespaceAware() {
		return namespaceAware;
	}

	@IbisDoc({"13", "Deprecated: when set <code>true</code> xslt processor 2.0 (net.sf.saxon) will be used, otherwise xslt processor 1.0 (org.apache.xalan)", "false"})
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

	@IbisDoc({"14", "when set <code>true</code> the parameter 'environment' is added", "false"})
	public void setAddEnvironmentParameter(boolean b) {
		addEnvironmentParameter = b;
	}
	public boolean isAddEnvironmentParameter() {
		return addEnvironmentParameter;
	}

	@Override
	public void setThreadLifeCycleEventListener(ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener) {
		this.threadLifeCycleEventListener=threadLifeCycleEventListener;
	}

}
