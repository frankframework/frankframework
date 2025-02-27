/*
   Copyright 2013-2016 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.FrankElement;
import org.frankframework.core.PipeLineSession;
import org.frankframework.doc.Optional;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.TransformerPool.OutputType;

/**
 * Baseclass for caching.
 * Provides key transformation functionality.
 *
 * @author  Gerrit van Brakel
 * @since   4.11
 */
public abstract class AbstractCacheAdapter<V> implements ICache<String,V>, FrankElement {
	protected Logger log = LogUtil.getLogger(this);
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	private @Getter String name;

	private @Getter String keyXPath;
	private @Getter OutputType keyXPathOutputType=OutputType.TEXT;
	private @Getter String keyNamespaceDefs;
	private @Getter String keyStyleSheet;
	private @Getter String keyInputSessionKey;
	private @Getter boolean cacheEmptyKeys=false;

	private @Getter String valueXPath;
	private @Getter OutputType valueXPathOutputType=OutputType.XML;
	private @Getter String valueNamespaceDefs;
	private @Getter String valueStyleSheet;
	private @Getter String valueInputSessionKey;
	private @Getter boolean cacheEmptyValues=false;

	private TransformerPool keyTp=null;
	private TransformerPool valueTp=null;

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getName())) {
			setName(applicationContext.getId()+"_cache");
		}
		if (StringUtils.isNotEmpty(getKeyXPath()) || StringUtils.isNotEmpty(getKeyStyleSheet())) {
			keyTp=TransformerPool.configureTransformer(this, getKeyNamespaceDefs(), getKeyXPath(), getKeyStyleSheet(), getKeyXPathOutputType(),false,null);
		}
		if (StringUtils.isNotEmpty(getValueXPath()) || StringUtils.isNotEmpty(getValueStyleSheet())) {
			valueTp=TransformerPool.configureTransformer(this, getValueNamespaceDefs(), getValueXPath(), getValueStyleSheet(), getValueXPathOutputType(),false,null);
		}
	}

	protected abstract V getElement(String key);
	protected abstract void putElement(String key, V value);
	protected abstract boolean removeElement(Object key);
	protected abstract V toValue(Message value);

	@Override
	public String transformKey(String input, PipeLineSession session) {
		if (StringUtils.isNotEmpty(getKeyInputSessionKey()) && session!=null) {
			input=(String)session.get(getKeyInputSessionKey());
		}
		if (keyTp!=null) {
			try {
				input=keyTp.transformToString(input, null);
			} catch (Exception e) {
				log.error("{}cannot determine cache key", getLogPrefix(), e);
			}
		}
		if (StringUtils.isEmpty(input)) {
			log.debug("determined empty cache key");
			if (isCacheEmptyKeys()) {
				return "";
			}
			return null;
		}
		return input;
	}

	@Override
	public V transformValue(Message value, PipeLineSession session) {
		if (StringUtils.isNotEmpty(getValueInputSessionKey()) && session!=null) {
			value=session.getMessage(getValueInputSessionKey());
		}
		if (valueTp!=null) {
			try{
				value = valueTp.transform(value);
			} catch (Exception e) {
				log.error("{}transformValue() cannot transform cache value [{}], will not cache", getLogPrefix(), value, e);
				return null;
			}
		}
		if (value.isEmpty()) {
			log.debug("determined empty cache value");
			if (isCacheEmptyValues()) {
				return toValue(new Message(""));
			}
			return null;
		}
		return toValue(value);
	}

	@Override
	public V get(String key){
		return getElement(key);
	}
	@Override
	public void put(String key, V value) {
		putElement(key, value);
	}

	public boolean remove(String key) {
		return removeElement(key);
	}


	/**
	 * Name of the cache, will be lowercased
	 * @ff.default <code>&lt;ownerName&gt;</code>_cache
	 */
	@Override
	@Optional
	public void setName(String name) {
		if(StringUtils.isNotEmpty(name)) {
			this.name=name.toLowerCase();
		}
	}

	public String getLogPrefix() {
		return "cache ["+getName()+"] ";
	}

	/** xpath expression to extract cache key from request message */
	public void setKeyXPath(String keyXPath) {
		this.keyXPath = keyXPath;
	}

	/**
	 * output type of xpath expression to extract cache key from request message
	 * @ff.default text
	 */
	public void setKeyXPathOutputType(OutputType keyXPathOutputType) {
		this.keyXPathOutputType = keyXPathOutputType;
	}

	/** namespace defintions for keyxpath. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code> definitions */
	public void setKeyNamespaceDefs(String keyNamespaceDefs) {
		this.keyNamespaceDefs = keyNamespaceDefs;
	}

	/** stylesheet to extract cache key from request message. Use in combination with {@link #setCacheEmptyKeys(boolean) cacheEmptyKeys} to inhibit caching for certain groups of request messages */
	public void setKeyStyleSheet(String keyStyleSheet) {
		this.keyStyleSheet = keyStyleSheet;
	}

	/** session key to use as input for transformation of request message to key by keyxpath or keystylesheet */
	public void setKeyInputSessionKey(String keyInputSessionKey) {
		this.keyInputSessionKey = keyInputSessionKey;
	}

	/**
	 * controls whether empty keys are used for caching. when set true, cache entries with empty keys can exist.
	 * @ff.default false
	 */
	public void setCacheEmptyKeys(boolean cacheEmptyKeys) {
		this.cacheEmptyKeys = cacheEmptyKeys;
	}

	/** xpath expression to extract value to be cached key from response message. Use in combination with {@link #setCacheEmptyValues(boolean) cacheEmptyValues} to inhibit caching for certain groups of response messages */
	public void setValueXPath(String valueXPath) {
		this.valueXPath = valueXPath;
	}
	public void setValueXPathOutputType(OutputType valueXPathOutputType) {
		this.valueXPathOutputType = valueXPathOutputType;
	}

	/** namespace defintions for valuexpath. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code> definitions */
	public void setValueNamespaceDefs(String valueNamespaceDefs) {
		this.valueNamespaceDefs = valueNamespaceDefs;
	}

	/** stylesheet to extract value to be cached from response message */
	public void setValueStyleSheet(String valueStyleSheet) {
		this.valueStyleSheet = valueStyleSheet;
	}

	/** session key to use as input for transformation of response message to cached value by valuexpath or valuestylesheet */
	public void setValueInputSessionKey(String valueInputSessionKey) {
		this.valueInputSessionKey = valueInputSessionKey;
	}

	/**
	 * controls whether empty values will be cached. when set true, empty cache entries can exist for any key.
	 * @ff.default false
	 */
	public void setCacheEmptyValues(boolean cacheEmptyValues) {
		this.cacheEmptyValues = cacheEmptyValues;
	}

}
