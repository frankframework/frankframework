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

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Setter;

/**
 * Baseclass for Object lookups.
 *
 * @param <O> Object class used by clients
 *
 * @author Gerrit van Brakel
 * @author Niels Meijer
 */
public abstract class ObjectFactoryBase<O> implements InitializingBean, DisposableBean {
	protected final Logger log = LogUtil.getLogger(this);
	private final Class<O> lookupClass;

	protected Map<String,O> objects = new ConcurrentHashMap<>();

	@Autowired @Setter
	private List<? extends IObjectLocator> objectLocators;

	protected ObjectFactoryBase(Class<O> lookupClass) {
		this.lookupClass = lookupClass;
	}

	protected abstract O augment(O object, String objectName);

	protected O get(String name, Properties environment) {
		return objects.computeIfAbsent(name, k -> compute(k, environment));
	}

	/**
	 * Add and augment an Object to this factory so it can be used without the need of a JNDI lookup.
	 * Should only be called during jUnit Tests or when registering an Object through Spring. Never through a JNDI lookup.
	 */
	public O add(O object, String name) {
		return objects.computeIfAbsent(name, k -> compute(object, name));
	}

	private O compute(O object, String name) {
		return augment(object, name);
	}

	private O compute(String name, Properties environment) {
		for(IObjectLocator objectLocator : objectLocators) {
			try {
				O ds = objectLocator.lookup(name, environment, lookupClass);
				if(ds != null) {
					return augment(ds, name);
				}
			} catch (Exception e) {
				throw new IllegalStateException("unable to create resource ["+name+"] found in locator [" + objectLocator + "]", e);
			}
		}
		throw new IllegalStateException("resource ["+name+"] not found in locators " + objectLocators);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(objectLocators == null) {
			throw new IllegalStateException("no objectLocators set, unable to perform lookup using ["+this.getClass().getSimpleName()+"]");
		}
	}

	@Override
	public void destroy() throws Exception {
		Exception masterException=null;
		for (Entry<String,O> entry:objects.entrySet()) {
			String name = entry.getKey();
			if (entry.getValue() instanceof AutoCloseable closable) {
				try {
					log.debug("closing ["+ClassUtils.nameOf(closable)+"] object ["+name+"]");
					closable.close();
				} catch (Exception e) {
					if (masterException==null) {
						masterException = new Exception("Exception caught closing ["+ClassUtils.nameOf(closable)+"] object ["+name+"] held by ("+getClass().getSimpleName()+")", e);
					} else {
						masterException.addSuppressed(e);
					}
				}
			}
		}
		objects.clear();
		if (masterException!=null) {
			throw masterException;
		}
	}
}
