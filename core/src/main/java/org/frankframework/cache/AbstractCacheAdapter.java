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
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.FrankElement;
import org.frankframework.core.IWithParameters;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.TransformerPool.OutputType;

/**
 * Baseclass for caching.
 * Provides key transformation functionality.
 *
 * @ff.parameter key provides the <code>key</code>.
 * @ff.parameter value provides the <code>value</code>.
 *
 * @author  Gerrit van Brakel
 * @since   4.11
 */
public abstract class AbstractCacheAdapter<V> implements ICache<String, V>, FrankElement, IWithParameters {
	private static final String PARAM_VALUE = "value";
	private static final String PARAM_KEY = "key";

	protected Logger log = LogUtil.getLogger(this);
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	protected @NonNull ParameterList parameterList = new ParameterList();

	private @Getter String name;
	private @Getter boolean cacheEmptyKeys = false;
	private @Getter boolean cacheEmptyValues = false;

	private @Getter String keyXPath;
	private @Getter OutputType keyXPathOutputType = OutputType.TEXT;
	private @Getter String keyNamespaceDefs;
	private @Getter String keyStyleSheet;
	private @Getter String keyInputSessionKey;

	private @Getter String valueXPath;
	private @Getter OutputType valueXPathOutputType=OutputType.XML;
	private @Getter String valueNamespaceDefs;
	private @Getter String valueStyleSheet;
	private @Getter String valueInputSessionKey;

	private TransformerPool keyTp = null;
	private TransformerPool valueTp = null;

	@Override
	public void configure() throws ConfigurationException {
		parameterList.setNamesMustBeUnique(true);
		parameterList.configure();

		if (StringUtils.isEmpty(getName())) {
			setName(applicationContext.getId()+"_cache");
		}

		if (StringUtils.isNotEmpty(getKeyXPath()) || StringUtils.isNotEmpty(getKeyStyleSheet())) {
			keyTp = TransformerPool.configureTransformer(this, getKeyNamespaceDefs(), getKeyXPath(), getKeyStyleSheet(), getKeyXPathOutputType(),false,null);
		}
		if (StringUtils.isNotEmpty(getValueXPath()) || StringUtils.isNotEmpty(getValueStyleSheet())) {
			valueTp = TransformerPool.configureTransformer(this, getValueNamespaceDefs(), getValueXPath(), getValueStyleSheet(), getValueXPathOutputType(),false,null);
		}
	}

	protected abstract V getElement(String key);
	protected abstract void putElement(String key, V value);
	protected abstract boolean removeElement(Object key);
	protected abstract V toValue(Message value);

	@Override
	public String transformKey(String input, PipeLineSession session) {
		// Tries to get the parameter with name 'key', or else falls back to the deprecated way of determining the key (using keyXPath or keyStyleSheet)
		if (getParameterList().hasParameter(PARAM_KEY)) {
			// To behave like the original flow, this obscure logic needs to remain
			String resolvedKey = getParameter(PARAM_KEY, new Message(input), session);

			if (StringUtils.isNotBlank(resolvedKey)) {
				return resolvedKey;
			}

			if (isCacheEmptyKeys()) {
				return "";
			}

			return null;
		}

		// else, use deprecated
		return deprecatedGetKey(input, session);
	}

	private String deprecatedGetKey(String input, PipeLineSession session) {
		if (StringUtils.isNotEmpty(getKeyInputSessionKey()) && session != null) {
			input = (String) session.get(getKeyInputSessionKey());
		}

		if (keyTp != null) {
			try {
				input = keyTp.transformToString(input);
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
		// Tries to get the parameter with name 'value', or else falls back to the deprecated way of determining the value

		Message returnMessage = null;

		if (getParameterList().hasParameter(PARAM_VALUE)) {
			String resolvedValue = getParameter(PARAM_VALUE, value, session);

			// To behave like the original flow, this obscure logic needs to remain
			if (StringUtils.isNotBlank(resolvedValue)) {
				returnMessage = new Message(resolvedValue);
			} else if (isCacheEmptyValues()) {
				returnMessage = new Message("");
			}
		} else {
			returnMessage = deprecatedGetValue(value, session);
		}

		// Make sure not to call toValue with a null parameter
		if (returnMessage == null) {
			return null;
		}

		return toValue(returnMessage);
	}

	private Message deprecatedGetValue(Message value, PipeLineSession session) {
		if (StringUtils.isNotEmpty(getValueInputSessionKey()) && session != null) {
			value = session.getMessage(getValueInputSessionKey());
		}

		if (valueTp != null) {
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
				return new Message("");
			}
			return null;
		}

		return value;
	}

	/**
	 * Gets a parameter with parameterName from the parameter list, if it exists. Returns the parameter value as a string, or null if the parameter does not
	 * exist or an error occurs when trying to determine the parameter value.
	 */
	private String getParameter(String parameterName, Message input, PipeLineSession session) {
		if (getParameterList().hasParameter(parameterName)) {
			try {
				ParameterValueList parameterValueList = getParameterList().getValues(input, session);
				ParameterValue parameterValue = parameterValueList.findParameterValue(parameterName);

				if (parameterValue != null) {
					return parameterValue.asStringValue();
				}
			} catch (ParameterException e) {
				log.error("{}cannot parameter '{}' from parameter list", getLogPrefix(), parameterName, e);
			}
		}

		return null;
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
	@org.frankframework.doc.Optional
	public void setName(String name) {
		if (StringUtils.isNotEmpty(name)) {
			this.name = name.toLowerCase();
		}
	}

	public String getLogPrefix() {
		return "cache ["+getName()+"] ";
	}

	/** xpath expression to extract cache key from request message */
	@Deprecated(forRemoval = true, since = "10.2")
	@ConfigurationWarning("Use the 'key' parameter to provide the key")
	public void setKeyXPath(String keyXPath) {
		this.keyXPath = keyXPath;
	}

	/**
	 * output type of xpath expression to extract cache key from request message
	 * @ff.default text
	 */
	@Deprecated(forRemoval = true, since = "10.2")
	@ConfigurationWarning("Use the 'key' parameter to provide the key")
	public void setKeyXPathOutputType(OutputType keyXPathOutputType) {
		this.keyXPathOutputType = keyXPathOutputType;
	}

	/** namespace definitions for keyxpath. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code> definitions */
	@Deprecated(forRemoval = true, since = "10.2")
	@ConfigurationWarning("Use the 'key' parameter to provide the key")
	public void setKeyNamespaceDefs(String keyNamespaceDefs) {
		this.keyNamespaceDefs = keyNamespaceDefs;
	}

	/** stylesheet to extract cache key from request message. Use in combination with {@link #setCacheEmptyKeys(boolean) cacheEmptyKeys} to inhibit caching for certain groups of request messages */
	@Deprecated(forRemoval = true, since = "10.2")
	@ConfigurationWarning("Use the 'key' parameter to provide the key")
	public void setKeyStyleSheet(String keyStyleSheet) {
		this.keyStyleSheet = keyStyleSheet;
	}

	/** session key to use as input for transformation of request message to key by keyxpath or keystylesheet */
	@Deprecated(forRemoval = true, since = "10.2")
	@ConfigurationWarning("Use the 'key' parameter to provide the key")
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
	@Deprecated(forRemoval = true, since = "10.2")
	@ConfigurationWarning("Use the 'value' parameter to provide the value")
	public void setValueXPath(String valueXPath) {
		this.valueXPath = valueXPath;
	}

	@Deprecated(forRemoval = true, since = "10.2")
	@ConfigurationWarning("Use the 'value' parameter to provide the value")
	public void setValueXPathOutputType(OutputType valueXPathOutputType) {
		this.valueXPathOutputType = valueXPathOutputType;
	}

	/** namespace definitions for valuexpath. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code> definitions */
	@Deprecated(forRemoval = true, since = "10.2")
	@ConfigurationWarning("Use the 'value' parameter to provide the value")
	public void setValueNamespaceDefs(String valueNamespaceDefs) {
		this.valueNamespaceDefs = valueNamespaceDefs;
	}

	/** stylesheet to extract value to be cached from response message */
	@Deprecated(forRemoval = true, since = "10.2")
	@ConfigurationWarning("Use the 'value' parameter to provide the value")
	public void setValueStyleSheet(String valueStyleSheet) {
		this.valueStyleSheet = valueStyleSheet;
	}

	/** session key to use as input for transformation of response message to cached value by valuexpath or valuestylesheet */
	@Deprecated(forRemoval = true, since = "10.2")
	@ConfigurationWarning("Use the 'value' parameter to provide the value")
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

	@Override
	public void addParameter(IParameter p) {
		parameterList.add(p);
	}

	@Override
	public @NonNull ParameterList getParameterList() {
		return parameterList;
	}
}
