/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020 WeAreFrank!

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

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.TransformerPool;

/**
 * Baseclass for caching.
 * Provides key transformation functionality.
 * 
 * 
 * @author  Gerrit van Brakel
 * @since   4.11
 */
public abstract class CacheAdapterBase<V> implements ICacheAdapter<String,V> {
	protected Logger log = LogUtil.getLogger(this);
	private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

	private String name;

	private String keyXPath;
	private String keyXPathOutputType="text";
	private String keyNamespaceDefs;
	private String keyStyleSheet;
	private String keyInputSessionKey;
	private boolean cacheEmptyKeys=false;

	private String valueXPath;
	private String valueXPathOutputType="xml";
	private String valueNamespaceDefs;
	private String valueStyleSheet;
	private String valueInputSessionKey;
	private boolean cacheEmptyValues=false;

	private TransformerPool keyTp=null;
	private TransformerPool valueTp=null;

	@Override
	public void configure(String ownerName) throws ConfigurationException {
		if (StringUtils.isEmpty(getName())) {
			setName(ownerName+"Cache");
		}
		if (!("xml".equals(getKeyXPathOutputType()) || "text".equals(getKeyXPathOutputType()))) {
			throw new ConfigurationException(getLogPrefix()+"keyXPathOutputType ["+getKeyXPathOutputType()+"] must be either 'xml' or 'text'");
		}
		if (!("xml".equals(getValueXPathOutputType()) || "text".equals(getValueXPathOutputType()))) {
			throw new ConfigurationException(getLogPrefix()+"valueXPathOutputType ["+getValueXPathOutputType()+"] must be either 'xml' or 'text'");
		}
		if (StringUtils.isNotEmpty(getKeyXPath()) || StringUtils.isNotEmpty(getKeyStyleSheet())) {
			keyTp=TransformerPool.configureTransformer(getLogPrefix(), classLoader, getKeyNamespaceDefs(), getKeyXPath(), getKeyStyleSheet(),getKeyXPathOutputType(),false,null);
		}
		if (StringUtils.isNotEmpty(getValueXPath()) || StringUtils.isNotEmpty(getValueStyleSheet())) {
			valueTp=TransformerPool.configureTransformer(getLogPrefix(), classLoader, getValueNamespaceDefs(), getValueXPath(), getValueStyleSheet(),getValueXPathOutputType(),false,null);
		}
	}
	
	protected abstract V getElement(String key);
	protected abstract void putElement(String key, V value);
	protected abstract boolean removeElement(Object key);
	protected abstract V toValue(Message value);

	@Override
	public String transformKey(String input, IPipeLineSession session) {
		if (StringUtils.isNotEmpty(getKeyInputSessionKey()) && session!=null) {
			input=(String)session.get(getKeyInputSessionKey());
		}
		if (keyTp!=null) {
			try {
				input=keyTp.transform(input, null);
			} catch (Exception e) {
			   log.error(getLogPrefix()+"cannot determine cache key",e);
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
	public V transformValue(Message value, IPipeLineSession session) {
		if (StringUtils.isNotEmpty(getValueInputSessionKey()) && session!=null) {
			value=Message.asMessage(session.get(getValueInputSessionKey()));
		}
		if (valueTp!=null) {
			try{
				value=new Message(valueTp.transform(value, null));
			} catch (Exception e) {
				log.error(getLogPrefix() + "transformValue() cannot transform cache value [" + value + "], will not cache", e);
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

	public boolean remove(Object key) {
		return removeElement(key);
	}

	public String getName() {
		return name;
	}

	@IbisDoc({"name of the cache, will be set from owner", ""})
	public void setName(String name) {
		this.name=name;
	}

	public String getLogPrefix() {
		return "cache ["+getName()+"] ";
	}
	
	public String getKeyXPath() {
		return keyXPath;
	}

	@IbisDoc({"xpath expression to extract cache key from request message", ""})
	public void setKeyXPath(String keyXPath) {
		this.keyXPath = keyXPath;
	}
	public String getKeyXPathOutputType() {
		return keyXPathOutputType;
	}

	@IbisDoc({"output type of xpath expression to extract cache key from request message, must be 'xml' or 'text'", "text"})
	public void setKeyXPathOutputType(String keyXPathOutputType) {
		this.keyXPathOutputType = keyXPathOutputType;
	}
	public String getKeyNamespaceDefs() {
		return keyNamespaceDefs;
	}

	@IbisDoc({"namespace defintions for keyxpath. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setKeyNamespaceDefs(String keyNamespaceDefs) {
		this.keyNamespaceDefs = keyNamespaceDefs;
	}
	public String getKeyStyleSheet() {
		return keyStyleSheet;
	}

	@IbisDoc({"stylesheet to extract cache key from request message. Use in combination with {@link #setCacheEmptyKeys(boolean) cacheEmptyKeys} to inhibit caching for certain groups of request messages", ""})
	public void setKeyStyleSheet(String keyStyleSheet) {
		this.keyStyleSheet = keyStyleSheet;
	}

	public String getKeyInputSessionKey() {
		return keyInputSessionKey;
	}

	@IbisDoc({"session key to use as input for transformation of request message to key by keyxpath or keystylesheet", ""})
	public void setKeyInputSessionKey(String keyInputSessionKey) {
		this.keyInputSessionKey = keyInputSessionKey;
	}

	public boolean isCacheEmptyKeys() {
		return cacheEmptyKeys;
	}

	@IbisDoc({"controls whether empty keys are used for caching. when set true, cache entries with empty keys can exist.", "false"})
	public void setCacheEmptyKeys(boolean cacheEmptyKeys) {
		this.cacheEmptyKeys = cacheEmptyKeys;
	}

	public String getValueXPath() {
		return valueXPath;
	}

	@IbisDoc({"xpath expression to extract value to be cached key from response message. Use in combination with {@link #setCacheEmptyValues(boolean) cacheEmptyValues} to inhibit caching for certain groups of response messages", ""})
	public void setValueXPath(String valueXPath) {
		this.valueXPath = valueXPath;
	}
	public String getValueXPathOutputType() {
		return valueXPathOutputType;
	}
	public void setValueXPathOutputType(String valueXPathOutputType) {
		this.valueXPathOutputType = valueXPathOutputType;
	}
	public String getValueNamespaceDefs() {
		return valueNamespaceDefs;
	}

	@IbisDoc({"namespace defintions for valuexpath. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setValueNamespaceDefs(String valueNamespaceDefs) {
		this.valueNamespaceDefs = valueNamespaceDefs;
	}
	public String getValueStyleSheet() {
		return valueStyleSheet;
	}

	@IbisDoc({"stylesheet to extract value to be cached from response message", ""})
	public void setValueStyleSheet(String valueStyleSheet) {
		this.valueStyleSheet = valueStyleSheet;
	}
	
	public String getValueInputSessionKey() {
		return valueInputSessionKey;
	}

	@IbisDoc({"session key to use as input for transformation of response message to cached value by valuexpath or valuestylesheet", ""})
	public void setValueInputSessionKey(String valueInputSessionKey) {
		this.valueInputSessionKey = valueInputSessionKey;
	}

	public boolean isCacheEmptyValues() {
		return cacheEmptyValues;
	}

	@IbisDoc({"controls whether empty values will be cached. when set true, empty cache entries can exist for any key.", "false"})
	public void setCacheEmptyValues(boolean cacheEmptyValues) {
		this.cacheEmptyValues = cacheEmptyValues;
	}

}
