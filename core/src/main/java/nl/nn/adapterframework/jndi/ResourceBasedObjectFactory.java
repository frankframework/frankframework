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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import javax.naming.NamingException;

import nl.nn.adapterframework.util.ClassLoaderUtils;

/**
 * Baseclass for Jndi lookups, using property files on the classpath.
 * 
 * @author Gerrit van Brakel
 * 
 */
public class ResourceBasedObjectFactory<O, L> extends ObjectFactoryBase<O,L> {

	protected L createObject(Properties properties, String jndiName) throws NamingException {
		return (L)properties;
	}
	/**
	 * Performs the actual lookup
	 */
	@Override
	protected L lookup(String jndiName, Properties jndiEnvironment) throws NamingException {
		String propertyResource = "/"+jndiName+".properties";
		URL url = ClassLoaderUtils.getResourceURL(propertyResource);
		if (url == null) {
			throw new NamingException("Could not find propertyResource ["+propertyResource+"] to lookup");
		}
		Properties properties = new Properties();
		try (InputStream is = url.openStream()) {
			properties.load(is);
		} catch (IOException e) {
			NamingException ne = new NamingException("Cannot read properties from url ["+url+"] for resource ["+jndiName+"]");
			ne.setRootCause(e);
			throw ne;
		}
		log.debug("located properties for JNDI name [" + jndiName + "]");
		return createObject(properties, jndiName);
	}

}
