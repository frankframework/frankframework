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
package org.frankframework.jms;

import java.util.List;
import java.util.Properties;

import javax.naming.NamingException;

import jakarta.jms.ConnectionFactory;

public interface IConnectionFactoryFactory {

	ConnectionFactory getConnectionFactory(String connectionFactoryName) throws NamingException;
	ConnectionFactory getConnectionFactory(String connectionFactoryName, Properties jndiEnvironment) throws NamingException;

	/**
	 * Return all known/registered ConnectionFactories
	 */
	List<String> getConnectionFactoryNames();

}
