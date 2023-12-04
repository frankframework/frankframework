/*
   Copyright 2021, 2022 WeAreFrank!

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
package nl.nn.adapterframework.jndi;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;

import lombok.SneakyThrows;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Baseclass for Object lookups.
 *
 * @author Gerrit van Brakel
 *
 * @param <O> Object class used by clients
 * @param <L> Class looked up
 */
public abstract class ObjectFactoryBase<O,L> implements DisposableBean {
	protected final Logger log = LogUtil.getLogger(this);

	protected Map<String,O> objects = new ConcurrentHashMap<>();

	/**
	 * Perform the actual lookup
	 */
	protected abstract L lookup(String jndiName, Properties jndiEnvironment) throws NamingException;

	@SuppressWarnings("unchecked")
	protected O augment(L object, String objectName) {
		return (O)object;
	}


	public O get(String jndiName) throws NamingException {
		return get(jndiName, null);
	}

	//Sonar doesn't see the SneakyThrows on compute
	public O get(String jndiName, Properties jndiEnvironment) throws NamingException {
		return objects.computeIfAbsent(jndiName, k -> compute(k, jndiEnvironment));
	}

	@SneakyThrows(NamingException.class)
	private O compute(String jndiName, Properties jndiEnvironment) {
		return augment(lookup(jndiName, jndiEnvironment), jndiName);
	}


	/**
	 * Add and augment an Object to this factory so it can be used without the need of a JNDI lookup.
	 * Should only be called during jUnit Tests or when registering an Object through Spring. Never through a JNDI lookup.
	 */
	public O add(L object, String jndiName) {
		return objects.computeIfAbsent(jndiName, k -> compute(object, jndiName));
	}

	private O compute(L object, String jndiName) {
		return augment(object, jndiName);
	}

	@Override
	public void destroy() throws Exception {
		Exception masterException=null;
		for (Entry<String,O> entry:objects.entrySet()) {
			String name = entry.getKey();
			O object = entry.getValue();
			if (object instanceof AutoCloseable) {
				try {
					log.debug("closing ["+ClassUtils.nameOf(object)+"] object ["+name+"]");
					((AutoCloseable)object).close();
				} catch (Exception e) {
					if (masterException==null) {
						masterException = new Exception("Exception caught closing ["+ClassUtils.nameOf(object)+"] object ["+name+"] held by ("+getClass().getSimpleName()+")", e);
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
