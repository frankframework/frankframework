/*
   Copyright 2013, 2016, 2019 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
package nl.nn.adapterframework.util;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.SoftReferenceObjectPool;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IConfigurationAware;
import nl.nn.adapterframework.core.IScopeProvider;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.ThreadConnector;
import nl.nn.adapterframework.xml.ClassLoaderURIResolver;
import nl.nn.adapterframework.xml.NonResolvingURIResolver;
import nl.nn.adapterframework.xml.TransformerFilter;

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

	private TransformerPool(Source source, String sysId, int xsltVersion, Source configSource, IScopeProvider scopeProvider) throws TransformerConfigurationException {
		super();
		this.configSource = configSource;
		try {
			if (xsltVersion <= 0) {
				configMap = getConfigMap();
				String version = configMap.get("version");
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
			log.debug("created Transformerpool for sysId [{}] scopeProvider [{}]", sysId, scopeProvider);
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

	private TransformerPool(Resource resource, int xsltVersion) throws TransformerConfigurationException, IOException, SAXException {
		this(resource.asSource(),resource.getSystemId(),xsltVersion,resource.asSource(), resource);
	}

	//TODO Fix this, Thread.currentThread().getContextClassLoader() should not be used and causes memory leaks upon reloading configurations!!!
	private TransformerPool(String xsltString, String sysId, int xsltVersion) throws TransformerConfigurationException {
		this(xsltString, sysId, xsltVersion, () -> Thread.currentThread().getContextClassLoader());
	}

	private TransformerPool(String xsltString, String sysId, int xsltVersion, IScopeProvider scopeProvider) throws TransformerConfigurationException {
		this(new StreamSource(new StringReader(xsltString)), sysId, xsltVersion,new StreamSource(new StringReader(xsltString)), scopeProvider);
	}

	/** @deprecated Use Resource or UtilityInstance instead! This can/will cause memory leaks upon reloading configurations!!! */
	@Deprecated
	public static TransformerPool getInstance(String xsltString) throws TransformerConfigurationException {
		return getInstance(xsltString, 0);
	}

	/** @deprecated Use Resource or UtilityInstance instead! This can/will cause memory leaks upon reloading configurations!!! */
	@Deprecated
	public static TransformerPool getInstance(String xsltString, int xsltVersion) throws TransformerConfigurationException {
		return new TransformerPool(xsltString, null, xsltVersion);
	}

	/**
	 * Special utility method to create a new TransformerPool without a ClassLoader.
	 * Utility pools should never use configuration classloaders, instead always read from the classpath!
	 */
	public static TransformerPool getUtilityInstance(String xsltString) throws TransformerConfigurationException {
		return getUtilityInstance(xsltString, 2);
	}

	/**
	 * Special utility method to create a new TransformerPool without a ClassLoader.
	 * Utility pools should never use configuration classloaders, instead always read from the classpath!
	 */
	public static TransformerPool getUtilityInstance(String xsltString, int xsltVersion) throws TransformerConfigurationException {
		return new TransformerPool(xsltString, null, xsltVersion, null) {
			@Override
			public void close() {
				// Not closing UtilityInstance, there are no ClassLoader references, and as it's shared between adapters it should never be closed.
			}
		};
	}

	public static TransformerPool getInstance(Resource resource) throws TransformerConfigurationException, IOException {
		return getInstance(resource, 0);
	}

	public static TransformerPool getInstance(Resource resource, int xsltVersion) throws TransformerConfigurationException, IOException {
		try {
			return new TransformerPool(resource, xsltVersion);
		} catch (SAXException e) {
			throw new TransformerConfigurationException(e);
		}
	}

	private void initTransformerPool(Source source, String sysId) throws TransformerConfigurationException {
		if (StringUtils.isNotEmpty(sysId)) {
			sysId=ClassUtils.getCleanedFilePath(sysId); // fix websphere classpath references
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

	public static TransformerPool configureTransformer(IConfigurationAware scopeProvider, String namespaceDefs, String xPathExpression, String styleSheetName, OutputType outputType, boolean includeXmlDeclaration, ParameterList params, boolean mandatory) throws ConfigurationException {
		if (mandatory || StringUtils.isNotEmpty(xPathExpression) || StringUtils.isNotEmpty(styleSheetName)) {
			return configureTransformer(scopeProvider,namespaceDefs,xPathExpression,styleSheetName,outputType, includeXmlDeclaration, params);
		}
		return null;
	}

	public static TransformerPool configureTransformer(IConfigurationAware scopeProvider, String namespaceDefs, String xPathExpression, String styleSheetName, OutputType outputType, boolean includeXmlDeclaration, ParameterList params) throws ConfigurationException {
		return configureTransformer0(scopeProvider,namespaceDefs,xPathExpression,styleSheetName,outputType,includeXmlDeclaration,params,0);
	}

	public static TransformerPool configureTransformer0(IConfigurationAware scopeProvider, String namespaceDefs, String xPathExpression, String styleSheetName, OutputType outputType, boolean includeXmlDeclaration, ParameterList params, int xsltVersion) throws ConfigurationException {
		if (StringUtils.isNotEmpty(xPathExpression)) {
			if (StringUtils.isNotEmpty(styleSheetName)) {
				throw new ConfigurationException("cannot have both an xpathExpression and a styleSheetName specified");
			}
			return getXPathTransformerPool(namespaceDefs, xPathExpression, outputType, includeXmlDeclaration, params, xsltVersion);
		}
		if (!StringUtils.isEmpty(styleSheetName)) {
			if (StringUtils.isNotEmpty(namespaceDefs)) {
				throw new ConfigurationException("cannot have namespaceDefs specified for a styleSheetName");
			}
			return configureStyleSheetTransformer(scopeProvider, styleSheetName, xsltVersion);
		}
		throw new ConfigurationException("either xpathExpression or styleSheetName must be specified");
	}

	public static TransformerPool configureStyleSheetTransformer(IConfigurationAware scopeProvider, String styleSheetName, int xsltVersion) throws ConfigurationException {
		TransformerPool result;
		if (!StringUtils.isEmpty(styleSheetName)) {
			Resource styleSheet=null;
			try {
				styleSheet = Resource.getResource(scopeProvider, styleSheetName);
				if (styleSheet==null) {
					throw new ConfigurationException("cannot find ["+ styleSheetName + "] in scope ["+scopeProvider+"]");
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
		} else {
			throw new ConfigurationException("either xpathExpression or styleSheetName must be specified");
		}
		return result;
	}

	public static TransformerPool getXPathTransformerPool(String namespaceDefs, String xPathExpression, @Nonnull OutputType outputType, boolean includeXmlDeclaration, ParameterList params) throws ConfigurationException {
		return getXPathTransformerPool(namespaceDefs, xPathExpression, outputType, includeXmlDeclaration, params, 0);
	}

	private static TransformerPool getXPathTransformerPool(String namespaceDefs, String xPathExpression, OutputType outputType, boolean includeXmlDeclaration, ParameterList params, int xsltVersion) throws ConfigurationException {
		String xslt = XmlUtils.createXPathEvaluatorSource(namespaceDefs,xPathExpression, outputType, includeXmlDeclaration, params, true, StringUtils.isEmpty(namespaceDefs), null, xsltVersion);
		log.debug("xpath [{}] resulted in xslt [{}]", xPathExpression, xslt);

		try {
			return new TransformerPool(xslt, null, xsltVersion);
		} catch (TransformerConfigurationException e) {
			throw new ConfigurationException("Cannot create TransformerPool for XPath expression ["+xPathExpression+"]", e);
		}
	}

	public void open() {
		if (pool == null) {
			pool = new SoftReferenceObjectPool<>(new BasePooledObjectFactory<Transformer>() {

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
	}

	protected Transformer getTransformer() throws TransformerConfigurationException {
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
			log.debug("Transformer was removed from pool as an error occured on the last transformation");
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

	public String transform(Document d, Map<String,Object> parameters)	throws TransformerException, IOException {
		return transform(new DOMSource(d),parameters);
	}

	public String transform(Message m, Map<String,Object> parameters) throws TransformerException, IOException, SAXException {
		return transform(m.asSource(),parameters);
	}

	public String transform(Message m, Map<String,Object> parameters, boolean namespaceAware) throws TransformerException, IOException, SAXException {
		if (namespaceAware) {
			return transform(XmlUtils.inputSourceToSAXSource(m.asInputSource(),namespaceAware, null), parameters);
		}
		try {
			return transform(XmlUtils.stringToSource(m.asString(),namespaceAware), parameters);
		} catch (DomBuilderException e) {
			throw new TransformerException(e);
		}
	}

	public String transform(String s, Map<String,Object> parameters) throws TransformerException, IOException, SAXException {
		return transform(XmlUtils.stringToSourceForSingleUse(s),parameters);
	}

	public String transform(String s, Map<String,Object> parameters, boolean namespaceAware) throws TransformerException, IOException, SAXException {
		return transform(XmlUtils.stringToSourceForSingleUse(s, namespaceAware),parameters);
	}

	public String transform(Source s) throws TransformerException, IOException {
		return transform(s,(Map<String,Object>)null);
	}

	public String transform(Source s, ParameterValueList pvl) throws TransformerException, IOException {
		return transform(s, null, pvl==null? null : pvl.getValueMap());
	}

	public String transform(Source s, Map<String,Object> parameters) throws TransformerException, IOException {
		return transform(s, null, parameters);
	}

	public String transform(Source s, Result r) throws TransformerException, IOException {
		return transform(s, r, (Map<String,Object>)null);
	}
	public String transform(Source s, Result r, ParameterValueList pvl) throws TransformerException, IOException {
		return transform(s, r, pvl==null? null : pvl.getValueMap());
	}
	public String transform(Source s, Result r, Map<String,Object> parameters) throws TransformerException, IOException {
		Transformer transformer = getTransformer();
		try {
			XmlUtils.setTransformerParameters(transformer, parameters);
			if (r == null) {
				return XmlUtils.transformXml(transformer, s);
			}
			transformer.transform(s,r);
		} catch (TransformerException te) {
			((TransformerErrorListener)transformer.getErrorListener()).setFatalTransformerException(te);
		} catch (IOException ioe) {
			((TransformerErrorListener)transformer.getErrorListener()).setFatalIOException(ioe);
		}
		finally {
			if (transformer != null) {
				TransformerErrorListener transformerErrorListener = (TransformerErrorListener)transformer.getErrorListener();
				if (transformerErrorListener.getFatalTransformerException() != null) {
					invalidateTransformerNoThrow(transformer);
					throw transformerErrorListener.getFatalTransformerException();
				}
				if (transformerErrorListener.getFatalIOException() != null) {
					invalidateTransformerNoThrow(transformer);
					throw transformerErrorListener.getFatalIOException();
				}
				try {
					releaseTransformer(transformer);
				} catch(Exception e) {
					log.warn("Exception returning transformer to pool",e);
				}
			}
		}
		return null;
	}

	public TransformerHandler getTransformerHandler() throws TransformerConfigurationException {
		TransformerHandler handler = ((SAXTransformerFactory)tFactory).newTransformerHandler(templates);
		Transformer transformer = handler.getTransformer();
		transformer.setErrorListener(new TransformerErrorListener());
			// Set URIResolver on transformer for Xalan. Setting it on the factory
			// doesn't work for Xalan. See
			// https://www.oxygenxml.com/archives/xsl-list/200306/msg00021.html
		transformer.setURIResolver(classLoaderURIResolver);
		return handler;
	}

	public TransformerFilter getTransformerFilter(ThreadConnector<?> threadConnector, ContentHandler handler) throws TransformerConfigurationException {
		return getTransformerFilter(threadConnector, handler, false, false);
	}

	public TransformerFilter getTransformerFilter(ThreadConnector<?> threadConnector, ContentHandler handler, boolean removeNamespacesFromInput, boolean handleLexicalEvents) throws TransformerConfigurationException {
		return new TransformerFilter(threadConnector, getTransformerHandler(), handler, removeNamespacesFromInput, handleLexicalEvents);
	}

	public @Nonnull Map<String, String> getConfigMap() throws TransformerException, IOException {
		// Due to lazy-loading of the config-map this can happen in multiple threads simultaneously. Hence we synchronize here, the config-source seems a logical choice that doesn't lock too much.
		synchronized (configSource) {
			if (configMap == null) {
				configMap = XmlUtils.getXsltConfig(configSource);
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
