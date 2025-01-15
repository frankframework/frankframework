/*
   Copyright 2024 WeAreFrank!

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
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;

import javax.sql.DataSource;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import lombok.Getter;
import lombok.Setter;

import org.apache.commons.lang3.StringUtils;

import org.springframework.jdbc.datasource.DriverManagerDataSource;

import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.StringResolver;
import org.frankframework.util.StringUtil;

public class FrankResources {

	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();

	private @Setter List<Resource> jdbc;
	private @Setter List<Resource> jms;
	private @Setter List<Resource> mqtt;
	private @Setter List<Resource> other;

	@Getter @Setter
	public static class Resource {
		String name;
		String type;
		String url;
		String authalias;
		String username;
		String password;
		Properties properties;

		@Override
		public String toString() {
			return "Resource ["+ name+"]";
		}
	}

	@SuppressWarnings("unchecked")
	public @Nullable <O> O lookup(@Nonnull String name, @Nullable Properties environment, @Nonnull Class<O> lookupClass) throws ClassNotFoundException {
		if(name.indexOf('/') == -1) {
			throw new IllegalStateException("no resource prefix found, expected one of [jdbc/jms/mongodb/mqtt]");
		}

		Resource resource = findResource(name);
		if(resource == null) {
			return null; // If the lookup returns null, fail-fast to allow other ResourceFactories to locate the object.
		}
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

	private @Nullable Resource findResource(@Nonnull String name) {
		int slashPos = name.indexOf('/');
		String prefix = name.substring(0, slashPos);
		String resourceName = name.substring(slashPos + 1);

		return switch (prefix) {
			case "jdbc" -> findResource(jdbc, resourceName);
			case "jms" -> findResource(jms, resourceName);
			case "mqtt" -> findResource(mqtt, resourceName);
			case "mongodb" -> findResource(other, resourceName);
			default -> throw new IllegalArgumentException("unexpected lookup type: " + prefix);
		};
	}

	private @Nullable Resource findResource(@Nullable List<Resource> resources, @Nonnull String name) {
		if(resources == null) {
			return null;
		}

		Optional<Resource> optional = resources.stream().filter(e -> name.equals(e.getName())).findFirst();
		return optional.orElse(null);
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
	private Properties getConnectionProperties(Resource resource, Properties environment) {
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
		CredentialFactory cf = getCredentials(resource);
		if(StringUtils.isNotEmpty(cf.getUsername())) {
			mergedProps.setProperty("user", cf.getUsername());
		}
		if(StringUtils.isNotEmpty(cf.getPassword())) {
			mergedProps.setProperty("password", cf.getPassword());
		}
		return mergedProps;
	}

	/**
	 * Performs a 'safe' lookup of credentials.
	 */
	private CredentialFactory getCredentials(Resource resource) {
		String alias = resource.getAuthalias();
		if(StringUtils.isNotEmpty(alias)) {
			alias = StringResolver.substVars(alias, APP_CONSTANTS);
		}
		String username = resource.getUsername();
		if(StringUtils.isNotEmpty(username)) {
			username = StringResolver.substVars(username, APP_CONSTANTS);
		}
		String password = resource.getPassword();
		if(StringUtils.isNotEmpty(password)) {
			password = StringResolver.substVars(password, APP_CONSTANTS);
		}

		return new CredentialFactory(alias, username, password);
	}
}
