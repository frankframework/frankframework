/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.cache;

import java.io.Serializable;

import net.sf.ehcache.Cache;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDescription; 
import nl.nn.adapterframework.pipes.FixedForwardPipe;

import org.apache.commons.lang.StringUtils;


/** 
 * @author Jaco de Groot
 */
@IbisDescription(
	"Remove specified cache key from cache with specified name." 
)
public class RemoveCacheKeyPipe extends FixedForwardPipe {
	private IbisCacheManager ibisCacheManager;
	private String cacheName;
	private KeyTransformer keyTransformer = new KeyTransformer();

	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(cacheName)) {
			throw new ConfigurationException(getLogPrefix(null) + " cacheName should be specified");
		}
		keyTransformer.configure(getName());
		ibisCacheManager = IbisCacheManager.getInstance();
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
			throws PipeRunException {
		String cacheKey = keyTransformer.transformKey((String)input, session);
		Cache cache = ibisCacheManager.getCache(cacheName);
		if (cache.remove("r"+cacheKey) && cache.remove("s"+cacheKey)) {
			log.debug("removed cache key [" + cacheKey + "] from cache ["+cacheName+"]");
		} else {
			log.warn("could not find cache key [" + cacheKey + "] to remove from cache ["+cacheName+"]");
		}
		return new PipeRunResult(getForward(), input);
	}

	public String getCacheName() {
		return cacheName;
	}

	@IbisDoc({"name of the cache to remove", ""})
	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	public String getKeyXPath() {
		return keyTransformer.getKeyXPath();
	}

	@IbisDoc({"xpath expression to extract cache key from request message", ""})
	public void setKeyXPath(String keyXPath) {
		keyTransformer.setKeyXPath(keyXPath);
	}

	public String getKeyXPathOutputType() {
		return keyTransformer.getKeyXPathOutputType();
	}

	@IbisDoc({"output type of xpath expression to extract cache key from request message, must be 'xml' or 'text'", "text"})
	public void setKeyXPathOutputType(String keyXPathOutputType) {
		keyTransformer.setKeyXPathOutputType(keyXPathOutputType);
	}

	public String getKeyNamespaceDefs() {
		return keyTransformer.getKeyNamespaceDefs();
	}

	@IbisDoc({"namespace defintions for keyxpath. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setKeyNamespaceDefs(String keyNamespaceDefs) {
		keyTransformer.setKeyNamespaceDefs(keyNamespaceDefs);
	}

	public String getKeyStyleSheet() {
		return keyTransformer.getKeyStyleSheet();
	}

	@IbisDoc({"stylesheet to extract cache key from request message. Use in combination with {@link #setCacheEmptyKeys(boolean) cacheEmptyKeys} to inhibit caching for certain groups of request messages", ""})
	public void setKeyStyleSheet(String keyStyleSheet) {
		keyTransformer.setKeyStyleSheet(keyStyleSheet);
	}

	public String getKeyInputSessionKey() {
		return keyTransformer.getKeyInputSessionKey();
	}

	@IbisDoc({"session key to use as input for transformation of request message to key by keyxpath or keystylesheet", ""})
	public void setKeyInputSessionKey(String keyInputSessionKey) {
		keyTransformer.setKeyInputSessionKey(keyInputSessionKey);
	}

}

/**
 *
 * Helper class to use the transformKey method of the abstract CacheAdapterBase
 * class.
 *
 */
class KeyTransformer extends CacheAdapterBase {

	public void close() {
	}

	public void open() {
	}

	@Override
	protected Serializable getElement(String arg0) {
		return null;
	}

	@Override
	protected void putElement(String arg0, Serializable arg1) {
	}

	@Override
	protected Object getElementObject(Object key) {
		return null;
	}

	@Override
	protected void putElementObject(Object key, Object value) {
	}

	@Override
	protected boolean removeElement(Object key) {
		return false;
	}

}
