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

import java.util.Properties;

import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.Setter;
import nl.nn.adapterframework.core.JndiContextPrefixFactory;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Baseclass for Jndi lookups.
 * Would be nice if we could have used JndiObjectFactoryBean but it has too much overhead
 * 
 * @author Gerrit van Brakel
 * 
 * @param <O> Object class used by clients
 * @param <L> Class looked up in JNDI
 */
public class JndiObjectFactory<O,L> extends ObjectFactoryBase<O,L> implements ApplicationContextAware {

	private final Class<L> lookupClass;
	private @Setter String jndiContextPrefix = null;

	public JndiObjectFactory(Class<L> lookupClass) {
		this.lookupClass = lookupClass;
	}

	/**
	 * Performs the actual JNDI lookup
	 */
	@Override
	protected L lookup(String jndiName, Properties jndiEnvironment) throws NamingException {
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
