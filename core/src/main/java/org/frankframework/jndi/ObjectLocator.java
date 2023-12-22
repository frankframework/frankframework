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
package org.frankframework.jndi;

import java.util.Properties;

import javax.naming.NamingException;

import org.springframework.jndi.JndiObjectLocator;

/**
 * This class doesn't have to be instantiated through Spring.
 * It a helper class for setting the JNDI Context, if any.
 */
public class ObjectLocator<L> extends JndiObjectLocator {

	public ObjectLocator(Properties jndiEnvironment, Class<L> lookupClass) {
		setExpectedType(lookupClass);

		if(jndiEnvironment != null) {
			setJndiEnvironment(jndiEnvironment);
		}
	}

	@Override
	public L lookup(String jndiName) throws NamingException {
		return (L) super.lookup(jndiName, getExpectedType());
	}

	/**
	 * Directly return a JNDI Object
	 */
	public static <L> L lookup(String jndiName, Properties jndiEnvironment, Class<L> lookupClass) throws NamingException {
		return new ObjectLocator<L>(jndiEnvironment, lookupClass).lookup(jndiName);
	}
}
