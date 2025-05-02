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
import java.util.Map.Entry;
import java.util.Properties;

import javax.sql.DataSource;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.StringResolver;
import org.frankframework.util.StringUtil;

public class ObjectCreator {

	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();

	@Nonnull
	@SuppressWarnings("unchecked")
	public <O> O instantiateResource(@Nonnull FrankResource resource, @Nullable Properties environment, @Nonnull Class<O> lookupClass) throws ClassNotFoundException {
		if(StringUtils.isEmpty(resource.getUrl())) {
			throw new IllegalStateException("field url is required");
		}

		Properties properties = getConnectionProperties(resource, environment);
		String url = StringResolver.substVars(resource.getUrl(), APP_CONSTANTS);
		String type = StringResolver.substVars(resource.getType(), APP_CONSTANTS);

		Class<?> clazz = ClassUtils.loadClass(type);

		if(lookupClass.isAssignableFrom(DataSource.class) && Driver.class.isAssignableFrom(clazz)) { // It's also possible to use the native drivers instead of the DataSources directly.
			return (O) loadDataSourceThroughDriver(clazz, url, properties);
		}

		if(lookupClass.isAssignableFrom(clazz)) {
			return (O) createInstance(clazz, url, properties);
		}

		throw new IllegalStateException("class is not of required type ["+type+"]");
	}

	/**
	 * Ensure that the driver is loaded, else the DriverManagerDataSource can not load it.
	 */
	private DataSource loadDataSourceThroughDriver(Class<?> clazz, String url, Properties properties) {
		DriverManagerDataSource dmds = new DriverManagerDataSource(url, properties);
		dmds.setDriverClassName(clazz.getCanonicalName()); // Initialize the JDBC Driver
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

		Properties connProps = resource.getProperties();
		if (connProps != null) {
			for(Entry<Object, Object> entry : connProps.entrySet()) {
				String key = String.valueOf(entry.getKey());
				String value = String.valueOf(entry.getValue());
				if(StringUtils.isNotEmpty(value)) {
					mergedProps.setProperty(key, StringResolver.substVars(value, APP_CONSTANTS));
				}
			}
		}

		String alias = substituteVariables(resource.getAuthalias());
		String username = substituteVariables(resource.getUsername());
		String password = substituteVariables(resource.getPassword());

		// if alias is null, use username and password directly
		if (StringUtils.isEmpty(alias)) {
			setPropertyIfNotEmpty(mergedProps, "user", username);
			setPropertyIfNotEmpty(mergedProps, "password", password);
		} else {
			CredentialFactory credentialFactory = new CredentialFactory(alias, username, password);
			setPropertyIfNotEmpty(mergedProps, "user", credentialFactory.getUsername());
			setPropertyIfNotEmpty(mergedProps, "password", credentialFactory.getPassword());
		}

		return mergedProps;
	}

	/**
	 * Sets the property in the merged properties if the value is not empty.
	 * @param mergedProps The merged properties.
	 * @param propertyName The name of the property to set.
	 * @param propertyValue The value of the property to set.
	 */
	private void setPropertyIfNotEmpty(Properties mergedProps, String propertyName, String propertyValue) {
		if (StringUtils.isNotEmpty(propertyValue)) {
			mergedProps.setProperty(propertyName, propertyValue);
		}
	}

	/**
	 * Substitutes the variables in the given value using the AppConstants.
	 * @param value The value to substitute.
	 * @return The substituted value, or null if the input was null.
	 */
	private String substituteVariables(String value) {
		if (StringUtils.isNotEmpty(value)) {
			return StringResolver.substVars(value, APP_CONSTANTS);
		}

		return value;
	}
}
