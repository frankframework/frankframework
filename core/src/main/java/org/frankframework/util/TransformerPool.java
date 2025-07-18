/*
   Copyright 2013, 2016, 2019 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.SoftReferenceObjectPool;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.FrankElement;
import org.frankframework.core.IScopeProvider;
import org.frankframework.core.Resource;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.stream.MessageContext;
import org.frankframework.threading.ThreadConnector;
import org.frankframework.xml.ClassLoaderURIResolver;
import org.frankframework.xml.NonResolvingURIResolver;
import org.frankframework.xml.ThreadConnectingFilter;
import org.frankframework.xml.TransformerFilter;

/**
 * Pool of transformers. As of IBIS 4.2.e the Templates object is used to
 * improve performance and work around threading problems with the api.
 *
 * @author Gerrit van Brakel
 */
public class TransformerPool {
	protected static Logger log = LogUtil.getLogger(TransformerPool.class);

	private final TransformerFactory tFactory;
	private final TransformerErrorListener factoryErrorListener;

	private Templates templates;
	private Resource reloadResource=null;
	private final @Getter int xsltVersion;

	private final Source configSource;
	private Map<String,String> configMap;

	private final URIResolver classLoaderURIResolver;

	private ObjectPool<Transformer> pool;

	public enum OutputType {
		TEXT,
		XML;

		public String getOutputMethod() {
			return name().toLowerCase();
		}
	}

	private TransformerPool(@Nonnull Source source, @Nullable String sysId, int xsltVersion, @Nonnull Source configSource, @Nullable IScopeProvider scopeProvider) throws TransformerConfigurationException {
		super();
		this.configSource = configSource;
		try {
			if (xsltVersion <= 0) {
				String version = getConfigMap().get("version");
				xsltVersion = XmlUtils.interpretXsltVersion(version);
			}
		} catch (TransformerException | IOException e) {
			throw new TransformerConfigurationException("Could not detect xslt version", e);
		}
		if (xsltVersion <= 0) {
			throw new TransformerConfigurationException("xsltVersion [" + xsltVersion + "] must be positive for sysId [" + sysId + "] ");
		}
		this.xsltVersion = xsltVersion;
		factoryErrorListener = new TransformerErrorListener();
		tFactory = XmlUtils.getTransformerFactory(xsltVersion, factoryErrorListener);
		if (scopeProvider != null) {
			classLoaderURIResolver = new ClassLoaderURIResolver(scopeProvider);
			log.debug("created TransformerPool for sysId [{}] scopeProvider [{}]", sysId, scopeProvider);
			tFactory.setURIResolver(classLoaderURIResolver);
		} else {
			classLoaderURIResolver = new NonResolvingURIResolver();
			tFactory.setURIResolver(classLoaderURIResolver);
		}
		initTransformerPool(source, sysId);

		open();
		// check if a transformer can be initiated
		Transformer t = getTransformer();

		releaseTransformer(t);
	}

	private TransformerPool(@Nonnull Resource resource, int xsltVersion) throws TransformerConfigurationException, IOException, SAXException {
		this(resource.asSource(),resource.getSystemId(),xsltVersion,resource.asSource(), resource);
	}

	private TransformerPool(@Nonnull String xsltString, @Nullable String sysId, int xsltVersion, @Nullable IScopeProvider scopeProvider) throws TransformerConfigurationException {
		this(new StreamSource(new StringReader(xsltString)), sysId, xsltVersion,new StreamSource(new StringReader(xsltString)), scopeProvider);
	}

	/**
	 * Get an instance of a TransformerPool that can load additional resources such as referenced URIs from the provided {@link IScopeProvider}.
	 * WARNING: This should only be used to be able to load Configuration-specific resources in dynamically loaded
	 * configurations. This should not be used for application-wide transformer pools.
	 *
	 * @param xsltString The XSLT to be evaluated, as string
	 * @param xsltVersion Version of XSLT. Can be 0, 1, or 2. If 0, the actual version will be extracted from the stylesheet at a performance-penalty.
	 * @param scopeProvider The {@link IScopeProvider} for loading additional resources
	 * @return A TransformerPool instance for the input stylesheet.
	 * @throws TransformerConfigurationException
	 */
	public static TransformerPool getInstance(@Nonnull String xsltString, int xsltVersion, @Nullable IScopeProvider scopeProvider) throws TransformerConfigurationException {
		return new TransformerPool(xsltString, null, xsltVersion, scopeProvider);
	}

	/**
	 * Special utility method to create a new TransformerPool without a ClassLoader.
	 * Utility pools should never use configuration classloaders, instead always read from the classpath!
	 * Utility pools should always be cached statically for re-use. The caller-code is responsible for this.
	 */
	public static TransformerPool getUtilityInstance(@Nonnull String xsltString, int xsltVersion) throws TransformerConfigurationException {
		return new TransformerPool(xsltString, null, xsltVersion, null) {
			@Override
			public void close() {
				// Not closing UtilityInstance, there are no ClassLoader references, and as it's shared between adapters it should never be closed.
			}
		};
	}

	/**
	 * Get a TransformerPool instance that loads its XSLT stylesheet from the given resource. The XSLT
	 * version will be dynamically derived from the stylesheet, at a performance penalty.
	 *
	 * @param resource {@link Resource} from which to load the stylesheet
	 * @return TransformerPool instance for the XSLT stylesheet.
	 * @throws TransformerConfigurationException Thrown if there was an exception creating the TransformerPool or parsing the XSLT stylesheet
	 * @throws IOException Thrown if the resource cannot be loaded
	 */
	public static TransformerPool getInstance(@Nonnull Resource resource) throws TransformerConfigurationException, IOException {
		return getInstance(resource, 0);
	}

	/**
	 * Get a TransformerPool instance that loads its XSLT stylesheet from the given resource. The
	 * XSLT version in the stylesheet should match the {@code xlstVersion} parameter, or be {@code 0} for
	 * dynamic derivation at a performance penalty.
	 *
	 * @param resource The {@link Resource} from which to load the XSLT stylesheet
	 * @param xsltVersion The XSLT version of the stylesheet. Can be 0, 1, or 2. If 0, the actual version will
	 *                    be dynamically derived from the stylesheet itself on creation at a cost in performance.
	 * @return TransformerPool instance for the XSLT stylesheet.
	 * @throws TransformerConfigurationException Thrown if there was an exception creating the TransformerPool or parsing the XSLT stylesheet
	 * @throws IOException Thrown if the resource cannot be loaded
	 */
	public static TransformerPool getInstance(@Nonnull Resource resource, int xsltVersion) throws TransformerConfigurationException, IOException {
		try {
			return new TransformerPool(resource, xsltVersion);
		} catch (SAXException e) {
			throw new TransformerConfigurationException(e);
		}
	}

	private void initTransformerPool(@Nonnull Source source, @Nullable String sysId) throws TransformerConfigurationException {
		if (StringUtils.isNotEmpty(sysId)) {
			source.setSystemId(sysId);
			log.debug("setting systemId to [{}]", sysId);
		}
		try {
			templates=tFactory.newTemplates(source);
		} catch (TransformerConfigurationException e) {
			TransformerException te=factoryErrorListener.getFatalTransformerException();
			if (te!=null) {
				throw new TransformerConfigurationException(te);
			}
			throw e;
		}
	}

	private void reloadTransformerPool() throws TransformerConfigurationException {
		if (reloadResource!=null) {
			try {
				initTransformerPool(reloadResource.asSource(), reloadResource.getSystemId());
				pool.clear();
			} catch (Exception e) {
				throw new TransformerConfigurationException("Could not clear pool",e);
			}
		}
	}

	@Nullable
	public static TransformerPool configureTransformer(@Nullable FrankElement scopeProvider, @Nullable String namespaceDefs, @Nullable String xPathExpression, @Nullable String styleSheetName, @Nonnull OutputType outputType, boolean includeXmlDeclaration, @Nullable ParameterList params, boolean mandatory) throws ConfigurationException {
		if (mandatory || StringUtils.isNotEmpty(xPathExpression) || StringUtils.isNotEmpty(styleSheetName)) {
			return configureTransformer(scopeProvider,namespaceDefs,xPathExpression,styleSheetName,outputType, includeXmlDeclaration, params);
		}
		return null;
	}

	@Nonnull
	public static TransformerPool configureTransformer(@Nullable FrankElement scopeProvider, @Nullable String namespaceDefs, @Nullable String xPathExpression, @Nullable String styleSheetName, @Nullable OutputType outputType, boolean includeXmlDeclaration, @Nullable ParameterList params) throws ConfigurationException {
		return configureTransformer0(scopeProvider,namespaceDefs,xPathExpression,styleSheetName,outputType,includeXmlDeclaration,params,0);
	}

	@Nonnull
	public static TransformerPool configureTransformer0(@Nullable FrankElement scopeProvider, @Nullable String namespaceDefs, @Nullable String xPathExpression, @Nullable String styleSheetName, @Nonnull OutputType outputType, boolean includeXmlDeclaration, @Nullable ParameterList params, int xsltVersion) throws ConfigurationException {
		if (StringUtils.isNotEmpty(xPathExpression)) {
			if (StringUtils.isNotEmpty(styleSheetName)) {
				throw new ConfigurationException("cannot have both an xpathExpression and a styleSheetName specified");
			}
			try {
				return getXPathTransformerPool(namespaceDefs, xPathExpression, outputType, includeXmlDeclaration, params, xsltVersion);
			} catch (TransformerConfigurationException e) {
				throw new ConfigurationException("Cannot create TransformerPool for XPath expression ["+xPathExpression+"]", e);
			}
		}
		if (!StringUtils.isEmpty(styleSheetName)) {
			if (StringUtils.isNotEmpty(namespaceDefs)) {
				throw new ConfigurationException("cannot have namespaceDefs specified for a styleSheetName");
			}
			return configureStyleSheetTransformer(scopeProvider, styleSheetName, xsltVersion);
		}
		throw new ConfigurationException("either xpathExpression or styleSheetName must be specified");
	}

	@Nonnull
	public static TransformerPool configureStyleSheetTransformer(@Nullable FrankElement scopeProvider, @Nullable String styleSheetName, int xsltVersion) throws ConfigurationException {
		if (StringUtils.isEmpty(styleSheetName)) {
			throw new ConfigurationException("either xpathExpression or styleSheetName must be specified");
		}
		TransformerPool result;
		Resource styleSheet=null;
		try {
			styleSheet = Resource.getResource(scopeProvider, styleSheetName);
			if (styleSheet==null) {
				throw new ConfigurationException("cannot find ["+ styleSheetName + "] in scope ["+(scopeProvider != null ? scopeProvider.getName() : "<Global>")+"]");
			}
			log.debug("configuring stylesheet [{}] resource [{}]", styleSheetName, styleSheet);
			result = TransformerPool.getInstance(styleSheet, xsltVersion);

			if (xsltVersion!=0) {
				String xsltVersionInStylesheet = result.getConfigMap().get("version");
				int detectedXsltVersion = XmlUtils.interpretXsltVersion(xsltVersionInStylesheet);
				if (xsltVersion!=detectedXsltVersion) {
					ConfigurationWarnings.add(scopeProvider, log, "configured xsltVersion ["+xsltVersion+"] does not match xslt version ["+detectedXsltVersion+"] declared in stylesheet ["+styleSheet.getSystemId()+"]");
				}
			}
		} catch (IOException e) {
			throw new ConfigurationException("cannot retrieve ["+ styleSheetName + "] resource ["+styleSheet+"]", e);
		} catch (TransformerException e) {
			throw new ConfigurationException("got error creating transformer from file [" + styleSheetName + "]", e);
		}
		if (XmlUtils.isAutoReload()) {
			result.reloadResource=styleSheet;
		}
		return result;
	}

	@Nonnull
	public static TransformerPool getXPathTransformerPool(@Nonnull String xpathExpression, int xsltVersion) throws TransformerConfigurationException {
		return getXPathTransformerPool(null, xpathExpression, OutputType.TEXT, false, null, xsltVersion);
	}

	@Nonnull
	public static TransformerPool getXPathTransformerPool(@Nullable String namespaceDefs, @Nonnull String xpathExpression, @Nonnull OutputType outputMethod) throws TransformerConfigurationException {
		return getXPathTransformerPool(namespaceDefs, xpathExpression, outputMethod, false, null, XmlUtils.DEFAULT_XSLT_VERSION);
	}

	@Nonnull
	public static TransformerPool getXPathTransformerPool(@Nullable String namespaceDefs, @Nonnull String xPathExpression, @Nonnull OutputType outputType, boolean includeXmlDeclaration, @Nullable ParameterList params) throws TransformerConfigurationException {
		return getXPathTransformerPool(namespaceDefs, xPathExpression, outputType, includeXmlDeclaration, params, XmlUtils.DEFAULT_XSLT_VERSION);
	}

	@Nonnull
	public static TransformerPool getXPathTransformerPool(@Nullable String namespaceDefs, @Nonnull String xPathExpression, @Nonnull OutputType outputType, boolean includeXmlDeclaration, @Nullable ParameterList params, int xsltVersion) throws TransformerConfigurationException {
		String xslt = XmlUtils.createXPathEvaluatorSource(namespaceDefs,xPathExpression, outputType, includeXmlDeclaration, params, true, StringUtils.isEmpty(namespaceDefs), null, xsltVersion);
		log.debug("xpath [{}] resulted in xslt [{}]", xPathExpression, xslt);

		return new TransformerPool(xslt, null, xsltVersion == 0 ? XmlUtils.DEFAULT_XSLT_VERSION : xsltVersion, null);
	}

	public void open() {
		if (pool == null) {
			pool = new SoftReferenceObjectPool<>(new BasePooledObjectFactory<>() {

				@Override
				public Transformer create() throws Exception {
					return createTransformer();
				}

				@Override
				public PooledObject<Transformer> wrap(Transformer transformer) {
					return new DefaultPooledObject<>(transformer);
				}

			});
		}
	}

	/**
	 * Closing the Pool doesn't automatically mean all references remaining in the pool will be terminated.
	 * After closing, manually releases any associated resources in the pool
	 */
	public void close() {
		try {
			if (pool != null) {
				pool.clear();
				pool.close();
				pool = null;
			}
		} catch (Exception e) {
			log.warn("exception clearing transformerPool", e);
		}
		tFactory.setURIResolver(null);
	}

	protected @Nonnull Transformer getTransformer() throws TransformerConfigurationException {
		if(pool == null) {
			throw new IllegalStateException("TransformerPool does not exist, did you forget to call open()?");
		}

		try {
			reloadTransformerPool();
			return pool.borrowObject();
		} catch (Exception e) {
			throw new TransformerConfigurationException(e);
		}
	}

	protected void releaseTransformer(Transformer t) throws TransformerConfigurationException {
		try {
			pool.returnObject(t);
		} catch (Exception e) {
			throw new TransformerConfigurationException("exception returning transformer to pool", e);
		}
	}

	protected void invalidateTransformer(Transformer t) throws Exception {
		pool.invalidateObject(t);
	}

	protected void invalidateTransformerNoThrow(Transformer transformer) {
		try {
			invalidateTransformer(transformer);
			log.debug("Transformer was removed from pool as an error occurred on the last transformation");
		} catch (Throwable t) {
			log.error("Error on removing transformer from pool", t);
		}
	}

	protected synchronized Transformer createTransformer() throws TransformerConfigurationException {
		Transformer t = templates.newTransformer();
		if (t==null) {
			throw new TransformerConfigurationException("cannot instantiate transformer");
		}
		t.setErrorListener(new TransformerErrorListener());
		// Set URIResolver on transformer for Xalan. Setting it on the factory
		// doesn't work for Xalan. See
		// https://www.oxygenxml.com/archives/xsl-list/200306/msg00021.html
		t.setURIResolver(classLoaderURIResolver);
		return t;
	}

	public String transformToString(Message m, Map<String,Object> parameters, boolean namespaceAware) throws TransformerException, IOException, SAXException {
		if (namespaceAware) {
			// TODO: This does not appear to properly honour namespaceAware=false
			return transformToString(XmlUtils.inputSourceToSAXSource(m.asInputSource(), namespaceAware, null), parameters);
		}
		try {
			// TODO: Converting message to string first is rather inefficient with memory but this is the only way to have namespaceAware=false
			return transformToString(XmlUtils.stringToSource(m.asString(), namespaceAware), parameters);
		} catch (DomBuilderException e) {
			throw new TransformerException(e);
		}
	}

	public String transformToString(String s) throws TransformerException, IOException, SAXException {
		return transformToString(XmlUtils.stringToSourceForSingleUse(s), null);
	}

	public String transformToString(String s, Map<String,Object> parameters) throws TransformerException, IOException, SAXException {
		return transformToString(XmlUtils.stringToSourceForSingleUse(s), parameters);
	}

	public String transformToString(String s, Map<String,Object> parameters, boolean namespaceAware) throws TransformerException, IOException, SAXException {
		return transformToString(XmlUtils.stringToSourceForSingleUse(s, namespaceAware), parameters);
	}

	public String transformToString(Source s) throws TransformerException, IOException {
		return transformToString(s, null);
	}

	public String transformToString(Source s, Map<String,Object> parameters) throws TransformerException, IOException {
		StringWriter out = new StringWriter(XmlUtils.getBufSize());
		Result result = new StreamResult(out);
		transform(s, result, parameters);
		return out.toString();
	}

	public String transformToString(@Nonnull Message input) throws TransformerException, IOException, SAXException {
		return transformToString(input.asSource(), null);
	}

	public @Nonnull Message transform(@Nonnull Message message) throws IOException, TransformerException, SAXException {
		return transform(message, (Map<String, Object>) null);
	}

	public @Nonnull Message transform(@Nonnull Message message, @Nullable ParameterValueList pvl) throws IOException, TransformerException, SAXException {
		return transform(message, pvl != null ? pvl.getValueMap() : null);
	}

	/**
	 * Transforms Frank messages.
	 */
	public @Nonnull Message transform(@Nonnull Message m, @Nullable Map<String, Object> parameterMap) throws IOException, TransformerException, SAXException {
		if (m.isEmpty()) {
			return Message.nullMessage();
		}
		MessageBuilder messageBuilder = new MessageBuilder();
		try (OutputStream outputStream = messageBuilder.asOutputStream()) {
			StreamResult result = new StreamResult(outputStream);
			Source source = m.asSource();
			if (source == null) throw new IllegalStateException("Message#asSource may not be null when message is not empty");
			transform(source, result, parameterMap);
		}
		Message output = messageBuilder.build();
		output.getContext().putAll(createMessageContext().getAll());
		return output;
	}

	private MessageContext createMessageContext() {
		try {
			MessageContext context = new MessageContext();
			for(Entry<String, String> entry : getConfigMap().entrySet()) {
				String name = entry.getKey();
				if("output-method".equals(name) && "xml".equals(entry.getValue())) {
					context.withMimeType(MediaType.APPLICATION_XML);
				} else if ("output-media-type".equals(name)) {
					context.withMimeType(MediaType.parseMediaType(entry.getValue()));
				}
				context.put("Xslt."+name, entry.getValue());
			}

			return context;
		} catch (TransformerException | IOException e) {
			// ignore errors
			return new MessageContext();
		}
	}

	/*
	 * Should ideally only be used internally, however there is one use outside this class.
	 */
	public void transform(@Nonnull Source s, @Nonnull Result r, Map<String,Object> parameters) throws TransformerException, IOException {
		Exception exceptionToThrow;
		Transformer transformer = getTransformer();
		try {
			XmlUtils.setTransformerParameters(transformer, parameters);
			transformer.transform(s,r);
		} catch (TransformerException te) {
			((TransformerErrorListener)transformer.getErrorListener()).setFatalTransformerException(te);
		} catch (IOException ioe) {
			((TransformerErrorListener)transformer.getErrorListener()).setFatalIOException(ioe);
		} finally {
			TransformerErrorListener transformerErrorListener = (TransformerErrorListener) transformer.getErrorListener();
			if (transformerErrorListener.getFatalTransformerException() != null) {
				invalidateTransformerNoThrow(transformer);
				exceptionToThrow = transformerErrorListener.getFatalTransformerException();
			} else if (transformerErrorListener.getFatalIOException() != null) {
				invalidateTransformerNoThrow(transformer);
				exceptionToThrow = transformerErrorListener.getFatalIOException();
			} else {
				exceptionToThrow = null;
				try {
					releaseTransformer(transformer);
				} catch(Exception e) {
					log.warn("Exception returning transformer to pool",e);
				}
			}
		}
		// Should not throw an exception from a "finally" block
		if (exceptionToThrow instanceof TransformerException transformerException) {
			throw transformerException;
		} else if (exceptionToThrow instanceof IOException ioException) {
			throw ioException;
		}
	}

	private TransformerHandler getTransformerHandler() throws TransformerConfigurationException {
		TransformerHandler handler = ((SAXTransformerFactory)tFactory).newTransformerHandler(templates);
		Transformer transformer = handler.getTransformer();
		transformer.setErrorListener(new TransformerErrorListener());
			// Set URIResolver on transformer for Xalan. Setting it on the factory
			// doesn't work for Xalan. See
			// https://www.oxygenxml.com/archives/xsl-list/200306/msg00021.html
		transformer.setURIResolver(classLoaderURIResolver);
		return handler;
	}

	public TransformerFilter getTransformerFilter(@Nonnull ContentHandler handler) throws TransformerConfigurationException {
		return getTransformerFilter(handler, false, false);
	}

	public TransformerFilter getTransformerFilter(@Nonnull ContentHandler handler, boolean removeNamespacesFromInput, boolean handleLexicalEvents) throws TransformerConfigurationException {
		return new TransformerFilter(getTransformerHandler(), handler, removeNamespacesFromInput, handleLexicalEvents);
	}

	public TransformerFilter getTransformerFilter(@Nonnull ThreadConnector<?> threadConnector, @Nonnull ContentHandler handler) throws TransformerConfigurationException {
		return new TransformerFilter(getTransformerHandler(), new ThreadConnectingFilter(threadConnector, handler), false, false);
	}

	private static Map<String,String> getXsltConfig(Source source) throws TransformerException, IOException {
		TransformerPool tp = UtilityTransformerPools.getGetXsltConfigTransformerPool();
		String metadataString = tp.transformToString(source);
		Map<String,String> result = new LinkedHashMap<>();
		for (final String s : StringUtil.split(metadataString, ";")) {
			List<String> kv = StringUtil.split(s, "=");
			String key = kv.get(0);
			String value = kv.get(1);
			result.put(key, value);
		}
		return result;
	}

	// protected because of tests, not even package private methods should access this...
	protected @Nonnull Map<String, String> getConfigMap() throws TransformerException, IOException {
		// Due to lazy-loading of the config-map this can happen in multiple threads simultaneously. Hence we synchronize here, the config-source seems a logical choice that doesn't lock too much.
		synchronized (configSource) {
			if (configMap == null) {
				configMap = getXsltConfig(configSource);
			}
		}
		return configMap;
	}

	public @Nullable Boolean getOmitXmlDeclaration() throws TransformerException, IOException {
		String setting = getConfigMap().get("output-omit-xml-declaration");
		if (setting == null) {
			return null;
		}
		return "yes".equals(setting);
	}

	public @Nullable Boolean getIndent() throws TransformerException, IOException {
		String setting = getConfigMap().get("output-indent");
		if (setting == null) {
			return null;
		}
		return "yes".equals(setting);
	}

	public @Nullable String getOutputMethod() throws TransformerException, IOException {
		return getConfigMap().get("output-method");
	}

	public @Nullable Boolean getDisableOutputEscaping() throws TransformerException, IOException {
		String setting = getConfigMap().get("disable-output-escaping");
		if (setting == null) {
			return null;
		}
		return "yes".equals(setting);
	}
}
