/*
   Copyright 2021-2024 WeAreFrank!

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
package org.frankframework.jndi;

import java.util.Properties;

import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.core.JndiContextPrefixFactory;
import org.frankframework.jdbc.datasource.IObjectLocator;
import org.frankframework.util.ClassUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.jndi.JndiTemplate;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;

/**
 * Baseclass for JDNI lookups.
 * Would be nice if we could have used JndiObjectFactoryBean but it has too much overhead
 *
 * @author Niels Meijer
 *
 * @param <O> Object class used by clients
 */
@Log4j2
public class JndiObjectFactory implements IObjectLocator, ApplicationContextAware {

	private @Setter String jndiContextPrefix = null;

	@Override
	public <O> O lookup(String jndiName, Properties jndiEnvironment, Class<O> lookupClass) throws NamingException {
		O object = null;
		String prefixedJndiName = getPrefixedJndiName(jndiName);
		JndiTemplate locator = new JndiTemplate(jndiEnvironment);
		try {
			object = locator.lookup(prefixedJndiName, lookupClass);
		} catch (NamingException e) { //Fallback and search again but this time without prefix
			if (!jndiName.equals(prefixedJndiName)) { //Only if a prefix is set!
				log.debug("prefixed JNDI name [" + prefixedJndiName + "] not found - trying original name [" + jndiName + "], exception: ("+ClassUtils.nameOf(e)+"): "+e.getMessage());

				try {
					object = locator.lookup(jndiName, lookupClass);
				} catch (NamingException e2) {
					e.addSuppressed(e2);
					throw e;
				}
			} else { //Either the fallback lookup should throw the NamingException or this one if no Object is found!
				throw e;
			}
		}

		log.debug("located Object with JNDI name [{}]", prefixedJndiName); //No exceptions during lookup means we found something!
		return object;
	}

	private String getPrefixedJndiName(String jndiName) {
		return StringUtils.isNotEmpty(jndiContextPrefix) && jndiName.indexOf(':') == -1 ? jndiContextPrefix + jndiName : jndiName;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		JndiContextPrefixFactory jndiContextFactory = applicationContext.getBean("jndiContextPrefixFactory", JndiContextPrefixFactory.class);
		if(jndiContextPrefix == null) { // setJndiContextPrefix is called before setApplicationContext. If explicitly set (ie prefix is not null), don't override this value.
			setJndiContextPrefix(jndiContextFactory.getContextPrefix());
		}
	}
}
