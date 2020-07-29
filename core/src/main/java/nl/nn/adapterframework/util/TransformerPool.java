/*
   Copyright 2013, 2016, 2019 Nationale-Nederlanden, 2020 WeAreFrank!

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
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.ThreadLifeCycleEventListener;
import nl.nn.adapterframework.xml.ClassLoaderURIResolver;
import nl.nn.adapterframework.xml.NonResolvingURIResolver;
import nl.nn.adapterframework.xml.TransformerFilter;

/**
 * Pool of transformers. As of IBIS 4.2.e the Templates object is used to
 * improve performance and work around threading problems with the api.
 * <p>
 * When the property 'transformerPool.useCaching' equals true, transformers are
 * put in a cache and shared (for the same stylesheet) to save memory.
 * 
 * @author Gerrit van Brakel
 */
public class TransformerPool {
	private static final boolean USE_CACHING = AppConstants.getInstance().getBoolean("transformerPool.useCaching", false);

	protected static Logger log = LogUtil.getLogger(TransformerPool.class);

	private TransformerFactory tFactory;

	private Templates templates;
	private Resource reloadResource=null;
	private int xsltVersion;
	
	private Source configSource;
	private Map<String,String> configMap;

	private URIResolver classLoaderURIResolver;

	private static class TransformerPoolKey {
		private String xsltString;
		private String urlString;
		private long urlLastModified;
		private String sysId;
		private int xsltVersion;

		TransformerPoolKey(String xsltString, URL url, String sysId, int xsltVersion) {
			this.xsltString = xsltString;
			if (url == null) {
				urlString = null;
				urlLastModified = -1;
			} else {
				urlString = url.toString();
				try {
					urlLastModified = url.openConnection().getLastModified();
				} catch (IOException e) {
					urlLastModified = 0;
				}
			}
			this.sysId = sysId;
			this.xsltVersion = xsltVersion;
		}

		@Override
		public String toString() {
			return "xsltVersion [" + xsltVersion + "] sysId [" + sysId + "] url [" + urlString +  (urlLastModified > 0 ? " " + DateUtils.format(urlLastModified) : "") + "] xsltString [" + xsltString + "]";
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof TransformerPoolKey) {
				TransformerPoolKey other = (TransformerPoolKey) o;
				if (xsltVersion == other.xsltVersion
						&& StringUtils.equals(sysId, other.sysId)
						&& StringUtils.equals(urlString, other.urlString)
						&& (urlLastModified != 0 && other.urlLastModified != 0 && urlLastModified == other.urlLastModified)
						&& StringUtils.equals(xsltString, other.xsltString)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int hashCode() {
			if (urlString == null) {
				return xsltString.hashCode();
			} else {
				return urlString.hashCode();
			}
		}
	}

	private static Map<TransformerPoolKey, TransformerPool> transformerPools = new ConcurrentHashMap<TransformerPoolKey, TransformerPool>();

	private ObjectPool pool;


	private TransformerPool(Source source, String sysId, int xsltVersion, Source configSource, ClassLoader classLoader) throws TransformerConfigurationException {
		super();
		this.configSource=configSource;
		try {
			if (xsltVersion<=0) {
				configMap=getConfigMap();
				String version=configMap.get("stylesheet-version");
				xsltVersion=XmlUtils.interpretXsltVersion(version);
			}
		} catch (TransformerException | IOException | SAXException e) {
			throw new TransformerConfigurationException("Could not detect xslt version",e);
		}
		if (xsltVersion<=0) {
			throw new TransformerConfigurationException("xsltVersion ["+xsltVersion+"] must be positive for sysId ["+sysId+"] ");
		}
		this.xsltVersion=xsltVersion;
		tFactory = XmlUtils.getTransformerFactory(xsltVersion);
		if(classLoader != null) {
			classLoaderURIResolver = new ClassLoaderURIResolver(classLoader);
			if (log.isDebugEnabled()) log.debug("created Transformerpool for sysId ["+sysId+"] classloader ["+ClassUtils.nameOf(classLoader)+"]");
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
		this(resource.asSource(),resource.getSystemId(),xsltVersion,resource.asSource(), resource.getClassLoader());
	}

	//TODO Fix this, Thread.currentThread().getContextClassLoader() should not be used and causes memory leaks upon reloading configurations!!!
	private TransformerPool(String xsltString, String sysId, int xsltVersion) throws TransformerConfigurationException {
		this(xsltString, sysId, xsltVersion, Thread.currentThread().getContextClassLoader());
	}

	private TransformerPool(String xsltString, String sysId, int xsltVersion, ClassLoader classLoader) throws TransformerConfigurationException {
		this(new StreamSource(new StringReader(xsltString)), sysId, xsltVersion,new StreamSource(new StringReader(xsltString)), classLoader);
	}

	public static TransformerPool getInstance(String xsltString) throws TransformerConfigurationException {
		return getInstance(xsltString, 0);
	}

	public static TransformerPool getInstance(String xsltString, int xsltVersion) throws TransformerConfigurationException {
		return getInstance(xsltString, null, xsltVersion);
	}

	public static TransformerPool getInstance(String xsltString, String sysId, int xsltVersion) throws TransformerConfigurationException {
		return getInstance(xsltString, sysId, xsltVersion, USE_CACHING);
	}

	public static TransformerPool getInstance(String xsltString, String sysId, int xsltVersion, boolean caching) throws TransformerConfigurationException {
		if (caching) {
			return retrieveInstance(xsltString, sysId, xsltVersion);
		} else {
			return new TransformerPool(xsltString, sysId, xsltVersion);
		}
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

	private static synchronized TransformerPool retrieveInstance(String xsltString, String sysId, int xsltVersion) throws TransformerConfigurationException {
		TransformerPoolKey tpKey = new TransformerPoolKey(xsltString, null, sysId, xsltVersion);
		if (transformerPools.containsKey(tpKey)) {
			return transformerPools.get(tpKey);
		} else {
			TransformerPool transformerPool = new TransformerPool(xsltString, sysId, xsltVersion);
			transformerPools.put(tpKey, transformerPool);
			return transformerPool;
		}
	}


	public static TransformerPool getInstance(Resource resource) throws TransformerConfigurationException, IOException {
		return getInstance(resource, 0);
	}

	public static TransformerPool getInstance(Resource resource, int xsltVersion) throws TransformerConfigurationException, IOException {
		return getInstance(resource, xsltVersion, USE_CACHING);
	}

	public static TransformerPool getInstance(Resource resource, int xsltVersion, boolean caching) throws TransformerConfigurationException, IOException {
		if (caching) {
			return retrieveInstance(resource, xsltVersion);
		} else {
			try {
				return new TransformerPool(resource, xsltVersion);
			} catch (SAXException e) {
				throw new TransformerConfigurationException(e);
			}
		}
	}

	private static synchronized TransformerPool retrieveInstance(Resource resource, int xsltVersion) throws TransformerConfigurationException, IOException {
		TransformerPoolKey tpKey = new TransformerPoolKey(null, resource.getURL(), null, xsltVersion);
		if (transformerPools.containsKey(tpKey)) {
			return transformerPools.get(tpKey);
		} else {
			try {
				TransformerPool transformerPool = new TransformerPool(resource, xsltVersion);
				transformerPools.put(tpKey, transformerPool);
				return transformerPool;
			} catch (SAXException e) {
				throw new TransformerConfigurationException(e);
			}
		}
	}


	private void initTransformerPool(Source source, String sysId) throws TransformerConfigurationException {
		if (StringUtils.isNotEmpty(sysId)) {
			sysId=ClassUtils.getCleanedFilePath(sysId); // fix websphere classpath references
			source.setSystemId(sysId);
			log.debug("setting systemId to ["+sysId+"]");
		}
		try {
			templates=tFactory.newTemplates(source);
		} catch (TransformerConfigurationException e) {
			TransformerErrorListener tel = (TransformerErrorListener)tFactory.getErrorListener();
			TransformerException te=tel.getFatalTransformerException();
			if (te!=null) {
				throw new TransformerConfigurationException(te);
			}
			throw e;
		}
	}

	private void reloadTransformerPool() throws TransformerConfigurationException, IOException {
		if (reloadResource!=null) {
			try {
				initTransformerPool(reloadResource.asSource(), reloadResource.getSystemId());
				pool.clear();
			} catch (Exception e) {
				throw new TransformerConfigurationException("Could not clear pool",e);
			}
		}
	}

	public static TransformerPool configureTransformer(String logPrefix, ClassLoader classLoader, String namespaceDefs, String xPathExpression, String styleSheetName, String outputType, boolean includeXmlDeclaration, ParameterList params, boolean mandatory) throws ConfigurationException {
		if (mandatory || StringUtils.isNotEmpty(xPathExpression) || StringUtils.isNotEmpty(styleSheetName)) {
			return configureTransformer(logPrefix,classLoader,namespaceDefs,xPathExpression,styleSheetName, outputType, includeXmlDeclaration, params);
		} 
		return null;
	}

	public static TransformerPool configureTransformer(String logPrefix, ClassLoader classLoader, String namespaceDefs, String xPathExpression, String styleSheetName, String outputType, boolean includeXmlDeclaration, ParameterList params) throws ConfigurationException {
		return configureTransformer0(logPrefix,classLoader,namespaceDefs,xPathExpression,styleSheetName,outputType,includeXmlDeclaration,params,0);
	}

	public static TransformerPool configureTransformer0(String logPrefix, ClassLoader classLoader, String namespaceDefs, String xPathExpression, String styleSheetName, String outputType, boolean includeXmlDeclaration, ParameterList params, int xsltVersion) throws ConfigurationException {
		if (logPrefix==null) {
			logPrefix="";
		}
		if (StringUtils.isNotEmpty(xPathExpression)) {
			if (StringUtils.isNotEmpty(styleSheetName)) {
				throw new ConfigurationException(logPrefix+" cannot have both an xpathExpression and a styleSheetName specified");
			}
			return XmlUtils.getXPathTransformerPool(namespaceDefs, xPathExpression, outputType, includeXmlDeclaration, params, xsltVersion);
		} 
		if (!StringUtils.isEmpty(styleSheetName)) {
			if (StringUtils.isNotEmpty(namespaceDefs)) {
				throw new ConfigurationException(logPrefix+" cannot have namespaceDefs specified for a styleSheetName");
			}
			return configureStyleSheetTransformer(logPrefix, classLoader, styleSheetName, xsltVersion);
		}
		throw new ConfigurationException(logPrefix+" either xpathExpression or styleSheetName must be specified");
	}

	public static TransformerPool configureStyleSheetTransformer(String logPrefix, ClassLoader classLoader, String styleSheetName, int xsltVersion) throws ConfigurationException {
		TransformerPool result;
		if (logPrefix==null) {
			logPrefix="";
		}
		if (!StringUtils.isEmpty(styleSheetName)) {
			Resource styleSheet=null;
			try {
				styleSheet = Resource.getResource(classLoader, styleSheetName);
				if (styleSheet==null) {
					throw new ConfigurationException(logPrefix+" cannot find ["+ styleSheetName + "] via classLoader ["+classLoader+"]"); 
				}
				if (log.isDebugEnabled()) log.debug(logPrefix+"configuring stylesheet ["+styleSheetName+"] url ["+styleSheet.getURL()+"] classloader ["+ClassUtils.nameOf(classLoader)+"]");
				result = TransformerPool.getInstance(styleSheet, xsltVersion);

				if (xsltVersion!=0) {
					String xsltVersionInStylesheet = result.getConfigMap().get("stylesheet-version");
					int detectedXsltVersion = XmlUtils.interpretXsltVersion(xsltVersionInStylesheet);
					if (xsltVersion!=detectedXsltVersion) {
						ConfigurationWarnings.add(null, log, logPrefix+"configured xsltVersion ["+xsltVersion+"] does not match xslt version ["+detectedXsltVersion+"] declared in stylesheet ["+styleSheet.getSystemId()+"]");
					}
				}
			} catch (IOException e) {
				throw new ConfigurationException(logPrefix+"cannot retrieve ["+ styleSheetName + "] resource ["+styleSheet.getSystemId()+"] url ["+styleSheet.getURL()+"]", e);
			} catch (SAXException|TransformerException e) {
				throw new ConfigurationException(logPrefix+" got error creating transformer from file [" + styleSheetName + "]", e);
			}
			if (XmlUtils.isAutoReload()) {
				result.reloadResource=styleSheet;
			}
		} else {
			throw new ConfigurationException(logPrefix+" either xpathExpression or styleSheetName must be specified");
		}
		return result;
	}

	public void open() {
		if (pool==null) {
			pool=new SoftReferenceObjectPool(new BasePoolableObjectFactory() {
				@Override
				public Object makeObject() throws Exception {
					return createTransformer();
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
			if (pool!=null) {
				pool.clear();
				pool.close();
				pool=null;
			}
		} catch (Exception e) {
			log.warn("exception clearing transformerPool",e);
		}
	}

	protected Transformer getTransformer() throws TransformerConfigurationException {
		if(pool == null) {
			throw new IllegalStateException("TransformerPool does not exist, did you forget to call open()?");
		}

		try {
			reloadTransformerPool();
			return (Transformer)pool.borrowObject();
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

	public String transform(String s, Map<String,Object> parameters) throws TransformerException, IOException, SAXException {
		return transform(XmlUtils.stringToSourceForSingleUse(s),parameters);
	}

	public String transform(String s, Map<String,Object> parameters, boolean namespaceAware) throws TransformerException, IOException, SAXException {
		return transform(XmlUtils.stringToSourceForSingleUse(s, namespaceAware),parameters);
	}

	public String transform(Source s) throws TransformerException, IOException {
		return transform(s,(Map<String,Object>)null);
	}
	
	public String transform(Source s, ParameterValueList pvl) throws TransformerException, IOException, ParameterException {
		return transform(s, null, pvl==null? (Map<String,Object>)null : pvl.getValueMap());
	}
	
	public String transform(Source s, Map<String,Object> parameters) throws TransformerException, IOException {
		return transform(s, null, parameters);
	}

	public String transform(Source s, Result r) throws TransformerException, IOException {
		return transform(s, r, (Map<String,Object>)null);
	}
	public String transform(Source s, Result r, ParameterValueList pvl) throws TransformerException, IOException, ParameterException {
		return transform(s, r, pvl==null? (Map<String,Object>)null : pvl.getValueMap());
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
				};
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

	
	public TransformerFilter getTransformerFilter(INamedObject owner, ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener, IPipeLineSession session, boolean expectChildThreads, ContentHandler handler) throws TransformerConfigurationException {
		return new TransformerFilter(owner, getTransformerHandler(), threadLifeCycleEventListener, session, expectChildThreads, handler);
	}
	
	public static List<String> getTransformerPoolsKeys() {
		List<String> transformerPoolsKeys = new LinkedList<String>();
		for (Iterator<TransformerPoolKey> it = transformerPools.keySet()
				.iterator(); it.hasNext();) {
			TransformerPoolKey transformerPoolKey = it.next();
			transformerPoolsKeys.add(transformerPoolKey.toString());
		}
		return transformerPoolsKeys;
	}

	public static void clearTransformerPools() {
		transformerPools.clear();
	}
	
	public Map<String,String> getConfigMap() throws TransformerException, IOException, SAXException {
		if (configMap==null) {
			configMap = XmlUtils.getXsltConfig(configSource);
		}
		return configMap;
	}
	
	public int getXsltVersion() throws TransformerException, IOException, SAXException {
		return xsltVersion;
	}

	public Boolean getOmitXmlDeclaration() throws TransformerException, IOException, SAXException {
		Map<String,String> configMap=getConfigMap();
		String setting=configMap.get("output-omit-xml-declaration");
		if (setting==null) {
			return null;
		}
		return "yes".equals(setting);
	}

	public Boolean getIndent() throws TransformerException, IOException, SAXException {
		Map<String,String> configMap=getConfigMap();
		String setting=configMap.get("output-indent");
		if (setting==null) {
			return null;
		}
		return "yes".equals(setting);
	}

	public String getOutputMethod() throws TransformerException, IOException, SAXException {
		Map<String,String> configMap=getConfigMap();
		return configMap.get("output-method");
	}
}
