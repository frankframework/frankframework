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
package nl.nn.adapterframework.util;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
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
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.ObjectPool;
import org.apache.commons.pool.impl.SoftReferenceObjectPool;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;

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

	protected Logger log = LogUtil.getLogger(this);

	private TransformerFactory tFactory;

	private Templates templates;
	private URL reloadURL=null;

	private ClassLoaderURIResolver classLoaderURIResolver =
			new ClassLoaderURIResolver(
					Thread.currentThread().getContextClassLoader());

	private static class TransformerPoolKey {
		private String xsltString;
		private String urlString;
		private long urlLastModified;
		private String sysId;
		private boolean xslt2;

		TransformerPoolKey(String xsltString, URL url, String sysId,
				boolean xslt2) {
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
			this.xslt2 = xslt2;
		}

		@Override
		public String toString() {
			return "xslt2 [" + xslt2 + "] sysId [" + sysId + "] url ["
					+ urlString
					+ (urlLastModified > 0
							? " " + DateUtils.format(urlLastModified) : "")
					+ "] xsltString [" + xsltString + "]";
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof TransformerPoolKey) {
				TransformerPoolKey other = (TransformerPoolKey) o;
				if (xslt2 == other.xslt2
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
	
	private ObjectPool pool = new SoftReferenceObjectPool(new BasePoolableObjectFactory() {
		@Override
		public Object makeObject() throws Exception {
			return createTransformer();
		}
	}); 

	private TransformerPool(Source source, String sysId) throws TransformerConfigurationException {
		this(source,sysId,false);
	}	

	private TransformerPool(Source source, String sysId, boolean xslt2) throws TransformerConfigurationException {
		super();
		tFactory = XmlUtils.getTransformerFactory(xslt2);
		tFactory.setURIResolver(classLoaderURIResolver);
		initTransformerPool(source, sysId);

		// check if a transformer can be initiated
		Transformer t = getTransformer();
		
		releaseTransformer(t);
	}	
	
	private TransformerPool(URL url, boolean xslt2) throws TransformerConfigurationException, IOException {
		this(new StreamSource(url.openStream(),Misc.DEFAULT_INPUT_STREAM_ENCODING),url.toString(),xslt2);
	}
	
	private TransformerPool(String xsltString, String sysId, boolean xslt2) throws TransformerConfigurationException {
		this(new StreamSource(new StringReader(xsltString)), sysId, xslt2);
	}

	public static TransformerPool getInstance(String xsltString)
			throws TransformerConfigurationException {
		return getInstance(xsltString, false);
	}

	public static TransformerPool getInstance(String xsltString, boolean xslt2)
			throws TransformerConfigurationException {
		return getInstance(xsltString, null, xslt2);
	}

	public static TransformerPool getInstance(String xsltString, String sysId,
			boolean xslt2) throws TransformerConfigurationException {
		return getInstance(xsltString, sysId, xslt2, USE_CACHING);
	}

	public static TransformerPool getInstance(String xsltString, String sysId,
			boolean xslt2, boolean caching) throws TransformerConfigurationException {
		if (caching) {
			return retrieveInstance(xsltString, sysId, xslt2);
		} else {
			return new TransformerPool(xsltString, sysId, xslt2);
		}
	}

	private static synchronized TransformerPool retrieveInstance(
			String xsltString, String sysId, boolean xslt2)
			throws TransformerConfigurationException {
		TransformerPoolKey tpKey = new TransformerPoolKey(xsltString, null,
				sysId, xslt2);
		if (transformerPools.containsKey(tpKey)) {
			return transformerPools.get(tpKey);
		} else {
			TransformerPool transformerPool = new TransformerPool(xsltString,
					sysId, xslt2);
			transformerPools.put(tpKey, transformerPool);
			return transformerPool;
		}
	}

	public static TransformerPool getInstance(URL url)
			throws TransformerConfigurationException, IOException {
		return getInstance(url, false);
	}

	public static TransformerPool getInstance(URL url, boolean xslt2)
			throws TransformerConfigurationException, IOException {
		return getInstance(url, xslt2, USE_CACHING);
	}

	public static TransformerPool getInstance(URL url, boolean xslt2, boolean caching)
			throws TransformerConfigurationException, IOException {
		if (caching) {
			return retrieveInstance(url, xslt2);
		} else {
			return new TransformerPool(url, xslt2);
		}
	}

	private static synchronized TransformerPool retrieveInstance(URL url,
			boolean xslt2)
			throws TransformerConfigurationException, IOException {
		TransformerPoolKey tpKey = new TransformerPoolKey(null, url, null,
				xslt2);
		if (transformerPools.containsKey(tpKey)) {
			return transformerPools.get(tpKey);
		} else {
			TransformerPool transformerPool = new TransformerPool(url, xslt2);
			transformerPools.put(tpKey, transformerPool);
			return transformerPool;
		}
	}

	private void initTransformerPool(Source source, String sysId) throws TransformerConfigurationException {
		if (StringUtils.isNotEmpty(sysId)) {
			source.setSystemId(sysId);
			log.debug("setting systemId to ["+sysId+"]");
		}
		templates=tFactory.newTemplates(source);
	}

	private void reloadTransformerPool() throws TransformerConfigurationException, IOException {
		if (reloadURL!=null) {
			initTransformerPool(new StreamSource(reloadURL.openStream(),Misc.DEFAULT_INPUT_STREAM_ENCODING),reloadURL.toString());
			try {
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
		return configureTransformer0(logPrefix,classLoader,namespaceDefs,xPathExpression,styleSheetName,outputType,includeXmlDeclaration,params,false);
	}

	public static TransformerPool configureTransformer0(String logPrefix, ClassLoader classLoader, String namespaceDefs, String xPathExpression, String styleSheetName, String outputType, boolean includeXmlDeclaration, ParameterList params, boolean xslt2) throws ConfigurationException {
		TransformerPool result;
		if (logPrefix==null) {
			logPrefix="";
		}
		if (StringUtils.isNotEmpty(xPathExpression)) {
			if (StringUtils.isNotEmpty(styleSheetName)) {
				throw new ConfigurationException(logPrefix+" cannot have both an xpathExpression and a styleSheetName specified");
			}
			try {
				List paramNames = null;
				if (params!=null) {
					paramNames = new ArrayList();
					Iterator iterator = params.iterator();
					while (iterator.hasNext()) {
						paramNames.add(((Parameter)iterator.next()).getName());
					}
				}
				result = TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(namespaceDefs,xPathExpression, outputType, includeXmlDeclaration, paramNames), xslt2);
			} 
			catch (TransformerConfigurationException te) {
				throw new ConfigurationException(logPrefix+" got error creating transformer from xpathExpression [" + xPathExpression + "] namespaceDefs [" + namespaceDefs + "]", te);
			}
		} 
		else {
			if (StringUtils.isNotEmpty(namespaceDefs)) {
				throw new ConfigurationException(logPrefix+" cannot have namespaceDefs specified for a styleSheetName");
			}
			if (!StringUtils.isEmpty(styleSheetName)) {
				URL resource = ClassUtils.getResourceURL(classLoader, styleSheetName);
				if (resource==null) {
					throw new ConfigurationException(logPrefix+" cannot find ["+ styleSheetName + "]"); 
				}
				try {
					result = TransformerPool.getInstance(resource, xslt2);
				} catch (IOException e) {
					throw new ConfigurationException(logPrefix+"cannot retrieve ["+ styleSheetName + "], resource ["+resource.toString()+"]", e);
				} catch (TransformerConfigurationException te) {
					throw new ConfigurationException(logPrefix+" got error creating transformer from file [" + styleSheetName + "]", te);
				}
				if (XmlUtils.isAutoReload()) {
					result.reloadURL=resource;
				}
			} else {
				throw new ConfigurationException(logPrefix+" either xpathExpression or styleSheetName must be specified");
			}
		}
		return result;
	}
	

	
	public void open() throws Exception {
	}
	
	public void close() {
		try {
			pool.clear();			
		} catch (Exception e) {
			log.warn("exception clearing transformerPool",e);
		}
	}
	
	protected Transformer getTransformer() throws TransformerConfigurationException {
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

	public String transform(Document d, Map parameters)	throws TransformerException, IOException {
		return transform(new DOMSource(d),parameters);
	}

	public String transform(String s, Map parameters) throws TransformerException, IOException, DomBuilderException {
		return transform(XmlUtils.stringToSourceForSingleUse(s),parameters);
	}

	public String transform(String s, Map parameters, boolean namespaceAware) throws TransformerException, IOException, DomBuilderException {
		return transform(XmlUtils.stringToSourceForSingleUse(s, namespaceAware),parameters);
	}

	public String transform(Source s, Map parameters) throws TransformerException, IOException {
		return transform(s, null, parameters);
	}

	public String transform(Source s, Result r, Map parameters) throws TransformerException, IOException {
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
}
