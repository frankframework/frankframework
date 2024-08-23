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

import javax.naming.NameNotFoundException;
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
 */
@Log4j2
public class JndiObjectLocator implements IObjectLocator, ApplicationContextAware {

	private @Setter String jndiContextPrefix = null;

	/**
	 * Attempt to lookup the Object in the JNDI. If it cannot find the object, attempt to try again (without a JNDI-prefix).
	 * @param <O> Object class used by clients
	 */
	@Override
	public <O> O lookup(String jndiName, Properties jndiEnvironment, Class<O> lookupClass) throws NamingException {
		String prefixedJndiName = getPrefixedJndiName(jndiName);
		JndiTemplate locator = new JndiTemplate(jndiEnvironment);
		try {
			return locator.lookup(prefixedJndiName, lookupClass);
		} catch (NameNotFoundException e) { //Fallback and search again but this time without prefix
			if (!jndiName.equals(prefixedJndiName)) { //But only if a prefix was used during the first lookup.
				log.debug("prefixed JNDI name [{}] not found - trying original name [{}], exception: ({}): {}", ()->prefixedJndiName, ()->jndiName, ()->ClassUtils.nameOf(e), e::getMessage);
				try {
					return locator.lookup(jndiName, lookupClass);
				} catch (NameNotFoundException e2) {
					log.debug("non-prefixed JNDI name [{}] not found, exception: ({}): {}", ()->jndiName, ()->ClassUtils.nameOf(e2), e2::getMessage);
				}
			}
			return null; //Neither lookup returned an (unexpected) exception, assume the object cannot be found in this IObjectLocator.
		}
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
