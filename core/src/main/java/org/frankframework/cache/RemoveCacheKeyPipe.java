/*
   Copyright 2016, 2020 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
package org.frankframework.cache;

import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import net.sf.ehcache.Cache;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;
import org.frankframework.util.TransformerPool.OutputType;

/**
 * Remove specified cache key from cache with specified name.
 *
 *
 * @author Jaco de Groot
 */
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.SESSION)
public class RemoveCacheKeyPipe extends FixedForwardPipe {
	private IbisCacheManager ibisCacheManager;
	private String cacheName;
	private final KeyTransformer keyTransformer = new KeyTransformer();

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(cacheName)) {
			throw new ConfigurationException("cacheName should be specified");
		}
		keyTransformer.configure(getName());
		ibisCacheManager = IbisCacheManager.getInstance();
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try {
			String cacheKey = keyTransformer.transformKey(message.asString(), session);
			Cache cache = ibisCacheManager.getCache(cacheName);
			if (cache == null) {
				log.warn("cache [{}] not found", cacheName);
			} else {
				if (cache.remove("r"+cacheKey) && cache.remove("s"+cacheKey)) {
					log.debug("removed cache key [{}] from cache [{}]", cacheKey, cacheName);
				} else {
					log.warn("could not find cache key [{}] to remove from cache [{}]", cacheKey, cacheName);
				}
			}
			return new PipeRunResult(getSuccessForward(), message);
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}
	}


	/** Name of the cache from which items are to be removed */
	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
	}
	public String getCacheName() {
		return cacheName;
	}

	/** XPath expression to extract cache key from request message */
	public void setKeyXPath(String keyXPath) {
		keyTransformer.setKeyXPath(keyXPath);
	}
	public String getKeyXPath() {
		return keyTransformer.getKeyXPath();
	}

	/**
	 * Output type of xpath expression to extract cache key from request message, must be 'xml' or 'text'
	 * @ff.default text
	 */
	public void setKeyXPathOutputType(OutputType keyXPathOutputType) {
		keyTransformer.setKeyXPathOutputType(keyXPathOutputType);
	}
	public OutputType getKeyXPathOutputType() {
		return keyTransformer.getKeyXPathOutputType();
	}

	/** Namespace defintions for keyXPath. Must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions */
	public void setKeyNamespaceDefs(String keyNamespaceDefs) {
		keyTransformer.setKeyNamespaceDefs(keyNamespaceDefs);
	}
	public String getKeyNamespaceDefs() {
		return keyTransformer.getKeyNamespaceDefs();
	}

	/** Stylesheet to extract cache key from request message. Use in combination with {@link EhCache#setCacheEmptyKeys cacheEmptyKeys} to inhibit caching for certain groups of request messages */
	public void setKeyStyleSheet(String keyStyleSheet) {
		keyTransformer.setKeyStyleSheet(keyStyleSheet);
	}
	public String getKeyStyleSheet() {
		return keyTransformer.getKeyStyleSheet();
	}

	/** Session key to use as input for transformation of request message to key by keyXPath or keyStyleSheet */
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
class KeyTransformer extends AbstractCacheAdapter {

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
