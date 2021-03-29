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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.Setter;
import lombok.SneakyThrows;
import nl.nn.adapterframework.core.JndiContextPrefixFactory;
import nl.nn.adapterframework.jdbc.ObjectLocator;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Baseclass for Jndi lookups.
 * Would be nice if we could have used JndiObjectFactoryBean but it has too much overhead
 * 
 * @param <O> Object class used by clients
 * @param <L> Class looked up in JNDI
 */
public class JndiObjectFactory<O,L> implements ApplicationContextAware {
	protected Logger log = LogUtil.getLogger(this);

	private Class<L> lookupClass;
	private @Setter String jndiContextPrefix = null;

	protected Map<String,O> objects = new ConcurrentHashMap<>();

	public JndiObjectFactory(Class<L> lookupClass) {
		this.lookupClass = lookupClass;
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
	 * Performs the actual JNDI lookup
	 */
	private L lookup(String jndiName, Properties jndiEnvironment) throws NamingException {
		L object = null;
		String prefixedJndiName = getPrefixedJndiName(jndiName);
		try {
			object = ObjectLocator.lookup(prefixedJndiName, jndiEnvironment, lookupClass);
		} catch (NamingException e) { //Fallback and search again but this time without prefix
			if (!jndiName.equals(prefixedJndiName)) { //Only if a prefix is set!
				log.debug("prefixed JNDI name [" + prefixedJndiName + "] not found - trying original name [" + jndiName + "], exception: ("+ClassUtils.nameOf(e)+"): "+e.getMessage());

				try {
					object = ObjectLocator.lookup(jndiName, jndiEnvironment, lookupClass);
				} catch (NamingException e2) {
					e.addSuppressed(e2);
					throw e;
				}
			} else { //Either the fallback lookup should throw the NamingException or this one if no Object is found!
				throw e;
			}
		}

		log.debug("located Object with JNDI name [" + prefixedJndiName + "]"); //No exceptions during lookup means we found something!
		return object;
	}

	@SuppressWarnings("unchecked")
	protected O augment(L object, String objectName) throws NamingException {
		return (O)object;
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

	private String getPrefixedJndiName(String jndiName) {
		return (StringUtils.isNotEmpty(jndiContextPrefix)) ? jndiContextPrefix + jndiName : jndiName;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		JndiContextPrefixFactory jndiContextFactory = applicationContext.getBean("jndiContextPrefixFactory", JndiContextPrefixFactory.class);
		if(jndiContextPrefix == null) { // setJndiContextPrefix is called before setApplicationContext. If explicitly set (ie prefix is not null), don't override this value.
			setJndiContextPrefix(jndiContextFactory.getContextPrefix());
		}
	}
}
