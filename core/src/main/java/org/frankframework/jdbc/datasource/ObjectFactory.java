/*
   Copyright 2021 - 2024 WeAreFrank!

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
package org.frankframework.jdbc.datasource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StringUtil;

/**
 * Baseclass for Object lookups.
 *
 * Already created Objects are stored in a ConcurrentHashMap.
 * Objects will be searched in all available {@link IObjectLocator IObjectLocators}. If it cannot find the object in the first locator, it will attempt to do so in the next available one.
 * Every Objects can be augmented before it is added.
 *
 * @param <O> Object class used by clients
 *
 * @author Gerrit van Brakel
 * @author Niels Meijer
 */
public abstract class ObjectFactory<O, P> implements InitializingBean, DisposableBean {
	protected final Logger log = LogUtil.getLogger(this);
	private final Class<P> lookupClass;
	private final @Getter String resourcePrefix;
	private final @Getter String displayName;

	private final Map<String, O> objects = new ConcurrentHashMap<>();

	@Autowired @Setter
	private List<? extends IObjectLocator> objectLocators;

	protected ObjectFactory(Class<P> lookupClass, String resourcePrefix, String displayName) {
		this.lookupClass = lookupClass;
		this.resourcePrefix = resourcePrefix;
		this.displayName = displayName;
	}

	/**
	 * Allow implementing classes to augment the looked up object class 'O'.
	 * Allows the originally created object to be mutated to another object. Useful to generate an object from a filled DTO.
	 */
	@SuppressWarnings({ "java:S1172", "unchecked" })
	protected O augment(P object, String objectName) {
		return (O) object;
	}

	/**
	 * Returns the object matching the name and return type.
	 * If not cached yet, attempts to traverse all {@link IObjectLocator IObjectLocators} to do so.
	 *
	 * When using a JNDI environment it allows initial properties to use for JNDI lookups.
	 */
	protected final O get(String name, Properties environment) {
		String nameWithResourcePrefix = StringUtils.prependIfMissing(name, resourcePrefix+"/");
		return objects.computeIfAbsent(nameWithResourcePrefix, k -> compute(k, environment));
	}

	/**
	 * Add and augment an Object to this factory so it can be used without the need of a lookup.
	 * Should only be called during jUnit Tests or when registering an Object through Spring. Never through a lookup.
	 */
	public O add(P object, String name) {
		return objects.computeIfAbsent(name, k -> augment(object, name));
	}

	private O compute(String name, Properties environment) {
		for(IObjectLocator objectLocator : objectLocators) {
			try {
				P object = objectLocator.lookup(name, environment, lookupClass);
				if(object != null) {
					log.debug("located Object [{}] in objectLocator [{}]", name, objectLocator);
					return augment(object, name);
				}
			} catch (Exception e) { // If an exception occurred, assume we were able to find the Object but unable to create it.
				throw new IllegalStateException("unable to create resource ["+name+"] found in locator [" + objectLocator + "]", e);
			}
		}
		throw new IllegalStateException("unable to find resource ["+name+"] using locators " + objectLocators);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(objectLocators == null) {
			throw new IllegalStateException("no objectLocators set, unable to perform lookup using ["+this.getClass().getSimpleName()+"]");
		}
	}

	protected List<String> getObjectNames() {
		List<String> names = new ArrayList<>(objects.keySet());
		names.sort(Comparator.naturalOrder()); // AlphaNumeric order
		return Collections.unmodifiableList(names);
	}

	public List<ObjectInfo> getObjectInfo() {
		return getObjectNames().stream().map(this::toObjectInfo).toList();
	}

	/**
	 * Mapping from <O> to a information object, used for logging and console actions.
	 */
	protected ObjectInfo toObjectInfo(String name) {
		Object obj = get(name, null);
		return new ObjectInfo(name, StringUtil.reflectionToString(obj), null);
	}

	public static record ObjectInfo (String name, String info, String connectionPoolProperties) {
		@Override
		public final String toString() {
			return "Resource [%s] %s, POOL: %s".formatted(name, info, connectionPoolProperties);
		}
	}

	@Override
	public final void destroy() throws Exception {
		Exception masterException=null;

		for (Entry<String, O> entry : objects.entrySet()) {
			final String name = entry.getKey();
			final O objectToDestroy = entry.getValue();
			try {
				log.debug("closing [{}] object [{}]", () -> ClassUtils.nameOf(objectToDestroy), () -> name);
				destroyObject(entry.getValue());
			} catch (Exception e) {
				if (masterException == null) {
					masterException = new Exception("Exception caught closing [" + ClassUtils.nameOf(objectToDestroy) + "] object [" + name + "] held by (" + getClass().getSimpleName() + ")", e);
				} else {
					masterException.addSuppressed(e);
				}
			}
		}

		objects.clear();
		if (masterException != null) {
			throw masterException;
		}
	}

	/**
	 * This method is called when the objects stored in this Factory are removed.
	 * It allows implementations to handle their own close actions.
	 */
	protected void destroyObject(O object) throws Exception {
		if (object instanceof AutoCloseable closable) {
			closable.close();
		} else if (object instanceof DisposableBean bean) {
			bean.destroy();
		}
	}
}
