/*
   Copyright 2024-2025 WeAreFrank!

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
package org.frankframework.jdbc.datasource;

import java.lang.reflect.Method;
import java.sql.Driver;
import java.util.Properties;

import javax.sql.DataSource;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import org.frankframework.util.ClassUtils;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.StringUtil;

public class ObjectCreator {

	@Nonnull
	@SuppressWarnings("unchecked")
	public <O> O instantiateResource(@Nonnull FrankResource resource, @Nullable Properties environment, @Nonnull Class<O> lookupClass) throws ClassNotFoundException {
		String url = resource.getUrl();
		if(StringUtils.isEmpty(url)) {
			throw new IllegalStateException("field url is required");
		}

		Properties properties = getConnectionProperties(resource, environment);
		String type = resource.getType();
		Class<?> clazz = ClassUtils.loadClass(type);

		if(lookupClass.isAssignableFrom(DataSource.class) && Driver.class.isAssignableFrom(clazz)) { // It's also possible to use the native drivers instead of the DataSources directly.
			return (O) loadDataSourceThroughDriver(clazz, resource, properties);
		}

		if(lookupClass.isAssignableFrom(clazz)) {
			return (O) createInstance(clazz, url, properties);
		}

		throw new IllegalStateException("class is not of required type ["+type+"]");
	}

	/**
	 * Ensure that the driver is loaded, else the DriverManagerDataSource can not load it.
	 */
	private DataSource loadDataSourceThroughDriver(Class<?> clazz, FrankResource resource, Properties properties) {
		DriverManagerDataSource dmds = new DriverManagerDataSource(resource.getUrl(), properties);
		dmds.setDriverClassName(clazz.getCanonicalName()); // Initialize the JDBC Driver

		// Technically this is not required, but perhaps somebody uses the DataSource directly without the Driver?
		CredentialFactory credentials = resource.getCredentials();
		dmds.setUsername(credentials.getUsername());
		dmds.setPassword(credentials.getPassword());

		return dmds;
	}

	/**
	 * Creates the class and populates available fields
	 */
	private <O> O createInstance(Class<O> clazz, String url, Properties properties) {
		try {
			O instance = clazz.getDeclaredConstructor().newInstance();

			for(Method method: clazz.getMethods()) {
				if(!method.getName().startsWith("set") || method.getParameterTypes().length != 1)
					continue;

				String fieldName = StringUtil.lcFirst(method.getName().substring(3));
				String value = properties.getProperty(fieldName);

				if("url".equalsIgnoreCase(fieldName) || "brokerURL".equals(fieldName)) { // Ensures the URL is always set, some drivers use upper case, others lower...
					ClassUtils.invokeSetter(instance, method, url);
				} else if(StringUtils.isNotEmpty(value)) {
					ClassUtils.invokeSetter(instance, method, value);
				}
			}

			return instance;
		} catch (Exception e) {
			throw new IllegalStateException("unable to create resource ["+clazz+"]", e);
		}
	}

	/**
	 * Combines the optional (supplied) environment, provided driver properties and resolved credentials.
	 */
	private Properties getConnectionProperties(FrankResource resource, Properties environment) {
		Properties mergedProps = new Properties();
		if(environment != null) {
			mergedProps.putAll(environment);
		}
		mergedProps.putAll(resource.getProperties());

		CredentialFactory cf = resource.getCredentials();
		if(StringUtils.isNotEmpty(cf.getUsername())) {
			mergedProps.setProperty("user", cf.getUsername());
		}
		if(StringUtils.isNotEmpty(cf.getPassword())) {
			mergedProps.setProperty("password", cf.getPassword());
		}
		return mergedProps;
	}
}
