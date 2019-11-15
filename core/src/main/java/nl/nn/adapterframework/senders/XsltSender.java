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

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

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
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.stream.IThreadCreator;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.MessageOutputStreamCap;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.StreamingSenderBase;
import nl.nn.adapterframework.stream.ThreadLifeCycleEventListener;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DomBuilderException;
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

	private String styleSheetName;
	private String styleSheetNameSessionKey=null;
	private String xpathExpression=null;
	private String namespaceDefs = null; 
	private String outputType="text";
	private boolean omitXmlDeclaration=true;
	private boolean indentXml=true;
	private boolean removeNamespaces=false;
	private boolean skipEmptyTags=false;
	private int xsltVersion=0; // set to 0 for auto detect.
	private boolean namespaceAware=XmlUtils.isNamespaceAwareByDefault();
	
	private TransformerPool transformerPool;
	private TransformerPool transformerPoolSkipEmptyTags;
	private TransformerPool transformerPoolRemoveNamespaces;
	
	private Map<String, TransformerPool> dynamicTransformerPoolMap;
	private int transformerPoolMapSize = 100;

	private ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener;


	/**
	 * The <code>configure()</code> method instantiates a transformer for the specified
	 * XSL. If the stylesheetname cannot be accessed, a ConfigurationException is thrown.
	 */
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		
		dynamicTransformerPoolMap = Collections.synchronizedMap(new LRUMap(transformerPoolMapSize));
		
//		if (StringUtils.isEmpty(getOutputType())) {
//			if (StringUtils.isNotEmpty(getStyleSheetName())) {
//				try {
//					TransformerPool detectOutputTypeTp=XmlUtils.getDetectXsltOutputTypeTransformerPool();
//					Resource styleSheet = Resource.getResource(getClassLoader(), getStyleSheetName());
//					if (styleSheet==null) {
//						throw new ConfigurationException(getLogPrefix()+" cannot find stylesheet ["+getStyleSheetName()+"]");
//					}
//					String outputType=detectOutputTypeTp.transform(styleSheet.asSource(), null);
//					setOutputType(outputType);
//				} catch (TransformerException | IOException | SAXException e) {
//					throw new ConfigurationException(getLogPrefix()+" could not determine output-type of stylesheet ["+getStyleSheetName()+"]");
//				}
//			} 
//			if (StringUtils.isNotEmpty(getXpathExpression())) {
//				setOutputType("text");
//			}
//		}

		if(StringUtils.isNotEmpty(getStyleSheetName()) || StringUtils.isNotEmpty(getXpathExpression())) {
			transformerPool = TransformerPool.configureTransformer0(getLogPrefix(), getClassLoader(), getNamespaceDefs(), getXpathExpression(), getStyleSheetName(), getOutputType(), !isOmitXmlDeclaration(), getParameterList(), getXsltVersion());
		}
		else if(StringUtils.isEmpty(getStyleSheetNameSessionKey())) {
			throw new ConfigurationException(getLogPrefix()+" one of xpathExpression, styleSheetName or styleSheetNameSessionKey must be specified");
		}
		
		
		if (isSkipEmptyTags()) {
			transformerPoolSkipEmptyTags = XmlUtils.getSkipEmptyTagsTransformerPool(isOmitXmlDeclaration(),isIndentXml());
		}
		if (isRemoveNamespaces()) {
			if (XmlUtils.XPATH_NAMESPACE_REMOVAL_VIA_XSLT) {
				transformerPoolRemoveNamespaces = XmlUtils.getRemoveNamespacesTransformerPool(isOmitXmlDeclaration(),isIndentXml());
			}
		}

		if (getXsltVersion()>=2) {
			ParameterList parameterList = getParameterList();
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
		if (transformerPoolSkipEmptyTags!=null) {
			try {
				transformerPoolSkipEmptyTags.open();
			} catch (Exception e) {
				throw new SenderException(getLogPrefix()+"cannot start TransformerPool SkipEmptyTags", e);
			}
		}
		if (transformerPoolRemoveNamespaces!=null) {
			try {
				transformerPoolRemoveNamespaces.open();
			} catch (Exception e) {
				throw new SenderException(getLogPrefix()+"cannot start TransformerPool RemoveNamespaces", e);
			}
		}
	}

	@Override
	public void close() throws SenderException {
		super.close();
		
		if (transformerPool!=null) {
			transformerPool.close();
		}
		
		if (!dynamicTransformerPoolMap.isEmpty()) {
			for(TransformerPool tp : dynamicTransformerPoolMap.values()) {
				tp.close();
			}
		}
		if (transformerPoolSkipEmptyTags!=null) {
			transformerPoolSkipEmptyTags.close();
		}
		if (transformerPoolRemoveNamespaces!=null) {
			transformerPoolRemoveNamespaces.close();
		}
	}

	protected Source adaptInput(String input, ParameterResolutionContext prc) throws PipeRunException, DomBuilderException, SAXException, TransformerException, IOException {
		if (transformerPoolRemoveNamespaces!=null) {
			log.debug(getLogPrefix()+ " removing namespaces from input message");
			input = transformerPoolRemoveNamespaces.transform(prc.getInputSource(true), null); 
			log.debug(getLogPrefix()+ " output message after removing namespaces [" + input + "]");
			return XmlUtils.stringToSourceForSingleUse(input, true);
		}
		return prc.getInputSource(isNamespaceAware());
	}

	protected ContentHandler filterInput(ContentHandler input, ParameterResolutionContext prc) throws PipeRunException, DomBuilderException, TransformerException, IOException {
		if (isRemoveNamespaces()) {
			log.debug(getLogPrefix()+ " providing filter to remove namespaces from input message");
			XMLFilterImpl filter = new NamespaceRemovingFilter();
			filter.setContentHandler(input);
			return filter;
		}
		return input; // TODO might be necessary to do something about namespaceaware
	}
	
	
	@Override
	public MessageOutputStream provideOutputStream(String correlationID, IPipeLineSession session, MessageOutputStream target) throws StreamingException {
		if (target==null) {
			target=new MessageOutputStreamCap();
		}
		ContentHandler handler = createHandler(correlationID, null, session, target);
		return new MessageOutputStream(handler,target);
	}
	
	private ContentHandler createHandler(String correlationID, String input, IPipeLineSession session, MessageOutputStream target) throws StreamingException {
		return createHandlerOud(correlationID, input, session, target);
	}
	private ContentHandler createHandlerNieuw(String correlationID, String input, IPipeLineSession session, MessageOutputStream target) throws StreamingException {
		ContentHandler handler = null;

		try {
			Map<String,Object> parametervalues = null;
			ParameterResolutionContext prc = new ParameterResolutionContext(input,session);
			if (paramList!=null) {
				parametervalues = prc.getValueMap(paramList);
			}

			if ("xml".equals(getOutputType())) {
				handler = target.asContentHandler();
			} else {
				XmlWriter xmlWriter = new XmlWriter(target.asWriter());
				if (!isOmitXmlDeclaration()) {
					xmlWriter.setIncludeXmlDeclaration(true);
				} else {
					xmlWriter.setTextMode(true);
				}
				xmlWriter.setIncludeXmlDeclaration(!isOmitXmlDeclaration());
				handler = xmlWriter;
				if (isIndentXml()) {
					xmlWriter.setNewlineAfterXmlDeclaration(true);
					PrettyPrintFilter indentingFilter = new PrettyPrintFilter();
					indentingFilter.setContentHandler(xmlWriter);
					handler=indentingFilter;
				}
			}

			if (isSkipEmptyTags()) {
				SkipEmptyTagsFilter skipEmptyTagsFilter = new SkipEmptyTagsFilter();
				skipEmptyTagsFilter.setContentHandler(handler);
				handler=skipEmptyTagsFilter;
			}
			
			TransformerPool poolToUse = transformerPool;
			if(StringUtils.isNotEmpty(styleSheetNameSessionKey) && prc.getSession().get(styleSheetNameSessionKey) != null) {
				String styleSheetNameToUse = prc.getSession().get(styleSheetNameSessionKey).toString();
			
				if(!dynamicTransformerPoolMap.containsKey(styleSheetNameToUse)) {
					dynamicTransformerPoolMap.put(styleSheetNameToUse, poolToUse = TransformerPool.configureTransformer(getLogPrefix(), getClassLoader(), null, null, styleSheetNameToUse, null, !isOmitXmlDeclaration(), getParameterList()));
					poolToUse.open();
				} else {
					poolToUse = dynamicTransformerPoolMap.get(styleSheetNameToUse);
				}
			}

			TransformerFilter mainFilter = poolToUse.getTransformerFilter(this, threadLifeCycleEventListener, correlationID);
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
	
	private ContentHandler createHandlerOud(String correlationID, String input, IPipeLineSession session, MessageOutputStream target) throws StreamingException {
		ContentHandler handler = null;

		try {
			Map<String,Object> parametervalues = null;
			ParameterResolutionContext prc = new ParameterResolutionContext(input,session);
			if (paramList!=null) {
				parametervalues = prc.getValueMap(paramList);
			}

			Result result;
			if ("xml".equals(getOutputType())) {
				SAXResult targetFeedingResult = new SAXResult();
				targetFeedingResult.setHandler(target.asContentHandler());
				result = targetFeedingResult;
			} else {
				result = new StreamResult(target.asWriter());
			}
			
			if (isSkipEmptyTags()) {
				TransformerHandler skipEmptyTagsHandler = transformerPoolSkipEmptyTags.getTransformerHandler();
				skipEmptyTagsHandler.setResult(result);
				SAXResult skipEmptyTagsFeedingResult = new SAXResult();
				skipEmptyTagsFeedingResult.setHandler(skipEmptyTagsHandler);
				result=skipEmptyTagsFeedingResult;
			}

			TransformerPool poolToUse = transformerPool;
			if(StringUtils.isNotEmpty(styleSheetNameSessionKey) && prc.getSession().get(styleSheetNameSessionKey) != null) {
				String styleSheetNameToUse = prc.getSession().get(styleSheetNameSessionKey).toString();
			
				if(!dynamicTransformerPoolMap.containsKey(styleSheetNameToUse)) {
					dynamicTransformerPoolMap.put(styleSheetNameToUse, poolToUse = TransformerPool.configureTransformer(getLogPrefix(), getClassLoader(), null, null, styleSheetNameToUse, null, !isOmitXmlDeclaration(), getParameterList()));
					poolToUse.open();
				} else {
					poolToUse = dynamicTransformerPoolMap.get(styleSheetNameToUse);
				}
			}

			TransformerHandler mainHandler = poolToUse.getTransformerHandler();
			XmlUtils.setTransformerParameters(mainHandler.getTransformer(),parametervalues);
			mainHandler.setResult(result);
			handler=mainHandler;
			
			handler=filterInput(handler, prc);
			
			return handler;
		} catch (Exception e) {
			//log.warn(getLogPrefix()+"intermediate exception logging",e);
			throw new StreamingException(getLogPrefix()+"Exception on creating transformerHandler chain", e);
		} 
	}

	/*
	 * alternative implementation of send message, that should do the same as the origial, but reuses the streaming content handler
	 */
	@Override
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc, MessageOutputStream target) throws SenderException {
		if (message==null) {
			throw new SenderException(getLogPrefix()+"got null input");
		}
		try {
			if (target==null) {
				target=new MessageOutputStreamCap();
			}
			InputSource source = new InputSource(new StringReader(message));
			ContentHandler handler = createHandler(correlationID, message, prc.getSession(), target);
			XMLReader reader = XmlUtils.getXMLReader(true, false, handler);
			reader.parse(source);
			return target.getResponseAsString();
		} catch (Exception e) {
			throw new SenderException(getLogPrefix()+"Exception on transforming input", e);
		}
	}
	
	/**
	 * Here the actual transforming is done. Under weblogic the transformer object becomes
	 * corrupt when a not-well formed xml was handled. The transformer is then re-initialized
	 * via the configure() and start() methods.
	 */
//	@Override
	public String sendMessage1(String correlationID, String message, ParameterResolutionContext prc, MessageOutputStream target) throws SenderException {
		String stringResult = null;
		if (message==null) {
			throw new SenderException(getLogPrefix()+"got null input");
		}
//		if (log.isDebugEnabled()) {
//			log.debug(getLogPrefix()+" transforming input ["+message+"] using prc ["+prc+"]");
//		}

		try {
			Source inputMsg=adaptInput(message, prc);
			Map<String,Object> parametervalues = null;
			if (paramList!=null) {
				parametervalues = prc.getValueMap(paramList);
			}
//			if (log.isDebugEnabled()) {
//				log.debug(getLogPrefix()+" transformerPool ["+transformerPool+"] transforming using prc ["+prc+"] and parameterValues ["+parametervalues+"]");
//				log.debug(getLogPrefix()+" prc.inputsource ["+prc.getInputSource()+"]");
//			}
						
			TransformerPool poolToUse = transformerPool;
			
			if(StringUtils.isNotEmpty(styleSheetNameSessionKey) && prc.getSession().get(styleSheetNameSessionKey) != null) {
				String styleSheetNameToUse = prc.getSession().get(styleSheetNameSessionKey).toString();
			
				if(!dynamicTransformerPoolMap.containsKey(styleSheetNameToUse)) {
					dynamicTransformerPoolMap.put(styleSheetNameToUse, poolToUse = TransformerPool.configureTransformer(getLogPrefix(), getClassLoader(), null, null, styleSheetNameToUse, null, !isOmitXmlDeclaration(), getParameterList()));
					poolToUse.open();
				} else {
					poolToUse = dynamicTransformerPoolMap.get(styleSheetNameToUse);
				}
			}
			
			if (target!=null) {
				SAXResult mainResult = new SAXResult();
				ContentHandler targetContentHandler = target.asContentHandler();
				if (isSkipEmptyTags()) {
					SAXResult skipEmptyTagsResult = new SAXResult();
					skipEmptyTagsResult.setHandler(targetContentHandler);
					TransformerHandler skipEmptyTagsHandler = transformerPoolSkipEmptyTags.getTransformerHandler();
					mainResult.setHandler(skipEmptyTagsHandler);
				} else {
					mainResult.setHandler(targetContentHandler);
				}
				poolToUse.transform(inputMsg, mainResult, parametervalues); 
				stringResult=message;
			} else {
				stringResult = poolToUse.transform(inputMsg, parametervalues); 
	
				if (isSkipEmptyTags()) {
					log.debug(getLogPrefix()+ " skipping empty tags from result [" + stringResult + "]");
					//URL xsltSource = ClassUtils.getResourceURL( this, skipEmptyTags_xslt);
					//Transformer transformer = XmlUtils.createTransformer(xsltSource);
					//stringResult = XmlUtils.transformXml(transformer, stringResult);
					stringResult = transformerPoolSkipEmptyTags.transform(XmlUtils.stringToSourceForSingleUse(stringResult, isNamespaceAware()), null); 
				}
			}
//			if (log.isDebugEnabled()) {
//				log.debug(getLogPrefix()+" transformed input ["+message+"] to ["+stringResult+"]");
//			}
		} 
		catch (Exception e) {
			//log.warn(getLogPrefix()+"intermediate exception logging",e);
			throw new SenderException(getLogPrefix()+" Exception on transforming input", e);
		} 
		return stringResult;
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

	@IbisDoc({"5", "For xpathExpression only: force the transformer generated from the xpath-expression to omit the xml declaration", "true"})
	public void setOmitXmlDeclaration(boolean b) {
		omitXmlDeclaration = b;
	}
	public boolean isOmitXmlDeclaration() {
		return omitXmlDeclaration;
	}

	@IbisDoc({"6", "For xpathExpression only: namespace defintions for xpathexpression. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions. One entry can be without a prefix, that will define the default namespace", ""})
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

	@IbisDoc({"8", "when set <code>true</code>, result is pretty-printed. (only used when <code>skipemptytags=true</code>)", "true"})
	public void setIndentXml(boolean b) {
		indentXml = b;
	}
	public boolean isIndentXml() {
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

	@Override
	public void setThreadLifeCycleEventListener(ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener) {
		this.threadLifeCycleEventListener=threadLifeCycleEventListener;
	}

}
