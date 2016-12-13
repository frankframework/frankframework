/*
   Copyright 2016 Nationale-Nederlanden

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
import nl.nn.adapterframework.pipes.FixedForwardPipe;

import org.apache.commons.lang.StringUtils;

/**
 * Remove specified cache key from cache with specified name.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.WsdlGeneratorPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCacheName(String) cacheName}</td><td>name of the cache to remove</td><td></td></tr>
 * <tr><td>{@link #setKeyXPath(String) keyXPath}</td><td>xpath expression to extract cache key from request message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeyXPathOutputType(String) keyXPathOutputType}</td><td>output type of xpath expression to extract cache key from request message, must be 'xml' or 'text'</td><td>text</td></tr>
 * <tr><td>{@link #setKeyNamespaceDefs(String) keyNamespaceDefs}</td><td>namespace defintions for keyXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeyStyleSheet(String) keyStyleSheet}</td><td>stylesheet to extract cache key from request message. Use in combination with {@link #setCacheEmptyKeys(boolean) cacheEmptyKeys} to inhibit caching for certain groups of request messages</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeyInputSessionKey(String) keyInputSessionKey}</td><td>session key to use as input for transformation of request message to key by keyXPath or keyStyleSheet</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 *
 * @author Jaco de Groot
 */
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

	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}

	public String getKeyXPath() {
		return keyTransformer.getKeyXPath();
	}

	public void setKeyXPath(String keyXPath) {
		keyTransformer.setKeyXPath(keyXPath);
	}

	public String getKeyXPathOutputType() {
		return keyTransformer.getKeyXPathOutputType();
	}

	public void setKeyXPathOutputType(String keyXPathOutputType) {
		keyTransformer.setKeyXPathOutputType(keyXPathOutputType);
	}

	public String getKeyNamespaceDefs() {
		return keyTransformer.getKeyNamespaceDefs();
	}

	public void setKeyNamespaceDefs(String keyNamespaceDefs) {
		keyTransformer.setKeyNamespaceDefs(keyNamespaceDefs);
	}

	public String getKeyStyleSheet() {
		return keyTransformer.getKeyStyleSheet();
	}

	public void setKeyStyleSheet(String keyStyleSheet) {
		keyTransformer.setKeyStyleSheet(keyStyleSheet);
	}

	public String getKeyInputSessionKey() {
		return keyTransformer.getKeyInputSessionKey();
	}

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