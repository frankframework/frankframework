/*
   Copyright 2021 WeAreFrank!

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
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;

import org.apache.logging.log4j.Logger;

import lombok.SneakyThrows;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Baseclass for Object lookups.
 * 
 * @param <O> Object class used by clients
 * @param <L> Class looked up
 */
public abstract class ObjectFactoryBase<O,L> {
	protected Logger log = LogUtil.getLogger(this);

	protected Map<String,O> objects = new ConcurrentHashMap<>();

	/**
	 * Perform the actual lookup
	 */
	protected abstract L lookup(String jndiName, Properties jndiEnvironment) throws NamingException;

	@SuppressWarnings("unchecked")
	protected O augment(L object, String objectName) throws NamingException {
		return (O)object;
	}


	public O get(String jndiName) throws NamingException {
		return get(jndiName, null);
	}

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

	@SneakyThrows(NamingException.class)
	private O compute(L object, String jndiName) {
		return augment(object, jndiName);
	}

}
