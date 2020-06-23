/*
   Copyright 2016, 2020 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.lang.StringUtils;

import net.sf.ehcache.Cache;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.stream.Message;

/**
 * Remove specified cache key from cache with specified name.
 *
 *
 * @author Jaco de Groot
 */
public class RemoveCacheKeyPipe extends FixedForwardPipe {
	private IbisCacheManager ibisCacheManager;
	private String cacheName;
	private KeyTransformer keyTransformer = new KeyTransformer();

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(cacheName)) {
			throw new ConfigurationException(getLogPrefix(null) + " cacheName should be specified");
		}
		keyTransformer.configure(getName());
		ibisCacheManager = IbisCacheManager.getInstance();
	}

	@Override
	public PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {
		try {
			String cacheKey = keyTransformer.transformKey(message.asString(), session);
			Cache cache = ibisCacheManager.getCache(cacheName);
			if (cache.remove("r"+cacheKey) && cache.remove("s"+cacheKey)) {
				log.debug("removed cache key [" + cacheKey + "] from cache ["+cacheName+"]");
			} else {
				log.warn("could not find cache key [" + cacheKey + "] to remove from cache ["+cacheName+"]");
			}
			return new PipeRunResult(getForward(), message);
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}
	}


	@IbisDoc({"1", "Name of the cache from which items are to be removed", ""})
	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}
	public String getCacheName() {
		return cacheName;
	}

	@IbisDoc({"2", "XPath expression to extract cache key from request message", ""})
	public void setKeyXPath(String keyXPath) {
		keyTransformer.setKeyXPath(keyXPath);
	}
	public String getKeyXPath() {
		return keyTransformer.getKeyXPath();
	}

	@IbisDoc({"3", "Output type of xpath expression to extract cache key from request message, must be 'xml' or 'text'", "text"})
	public void setKeyXPathOutputType(String keyXPathOutputType) {
		keyTransformer.setKeyXPathOutputType(keyXPathOutputType);
	}
	public String getKeyXPathOutputType() {
		return keyTransformer.getKeyXPathOutputType();
	}

	@IbisDoc({"4", "Namespace defintions for keyXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setKeyNamespaceDefs(String keyNamespaceDefs) {
		keyTransformer.setKeyNamespaceDefs(keyNamespaceDefs);
	}
	public String getKeyNamespaceDefs() {
		return keyTransformer.getKeyNamespaceDefs();
	}

	@IbisDoc({"5", "Stylesheet to extract cache key from request message. Use in combination with {@link #setCacheEmptyKeys(boolean) cacheEmptyKeys} to inhibit caching for certain groups of request messages", ""})
	public void setKeyStyleSheet(String keyStyleSheet) {
		keyTransformer.setKeyStyleSheet(keyStyleSheet);
	}
	public String getKeyStyleSheet() {
		return keyTransformer.getKeyStyleSheet();
	}

	@IbisDoc({"6", "Session key to use as input for transformation of request message to key by keyXPath or keyStyleSheet", ""})
	public void setKeyInputSessionKey(String keyInputSessionKey) {
		keyTransformer.setKeyInputSessionKey(keyInputSessionKey);
	}
	public String getKeyInputSessionKey() {
		return keyTransformer.getKeyInputSessionKey();
	}


}

/**
 * 
 * Helper class to use the transformKey method of the abstract CacheAdapterBase
 * class.
 *
 */
class KeyTransformer extends CacheAdapterBase {

	@Override
	public void close() {
	}

	@Override
	public void open() {
	}

	@Override
	protected Serializable getElement(String arg0) {
		return null;
	}

	@Override
	protected void putElement(String arg0, Object arg1) {
	}

	@Override
	protected boolean removeElement(Object key) {
		return false;
	}

	@Override
	protected Object toValue(Message value) {
		return value;
	}

}