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
package nl.nn.adapterframework.jdbc;

import java.util.Properties;

import javax.naming.NamingException;
import javax.sql.CommonDataSource;

import org.springframework.jndi.JndiObjectLocator;

/**
 * This class doesn't have to be instantiated through Spring. 
 * It a helper class for setting the JNDI Context, if any.
 */
public class JndiDataSourceLocator extends JndiObjectLocator {

	public JndiDataSourceLocator() {
		this(null);
	}

	public JndiDataSourceLocator(Properties jndiEnvironment) {
		setExpectedType(CommonDataSource.class);
		setResourceRef(true);

		if(jndiEnvironment != null) {
			setJndiEnvironment(jndiEnvironment);
		}
	}

	@Override
	public CommonDataSource lookup(String jndiName) throws NamingException {
		return (CommonDataSource) super.lookup(jndiName, getExpectedType());
	}

	/**
	 * Directly return a JNDI Object 
	 */
	public static CommonDataSource lookup(String jndiName, Properties jndiEnvironment) throws NamingException {
		return new JndiDataSourceLocator(jndiEnvironment).lookup(jndiName);
	}
}
