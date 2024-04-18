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
package org.frankframework.jndi;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.StringResolver;
import org.frankframework.util.StringUtil;

import lombok.Getter;
import lombok.Setter;

public class FrankResources {

	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();

	private @Setter List<JdbcResource> jdbc;
	private @Setter List<JmsResource> jms;

	@Getter @Setter
	public static class JdbcResource {
		String name;
		String type;
		String url;
		String authalias;
		String username;
		String password;
		Properties properties;

		@Override
		public String toString() {
			return "JDBCResource ["+ name+"]";
		}
	}

	@Getter @Setter
	public static class JmsResource {
		String name;
		String type;
		String url;
		String authalias;
		String username;
		String password;
		Properties properties;

		@Override
		public String toString() {
			return "JMSResource ["+ name+"]";
		}
	}

	@Override
	public String toString() {
		System.err.println(jdbc);
		return super.toString();
	}

	@SuppressWarnings("unchecked")
	public <O> O lookup(@Nonnull String name, Properties environment, @Nonnull Class<O> lookupClass) throws ClassNotFoundException {
		if(name.indexOf('/') == -1) {
			throw new IllegalStateException("no resource prefix found");
		}

		String prefix = name.split("/")[0];
		if("jdbc".equals(prefix)) {
			String asdfadsfadfs = name.split("/")[1];
			Optional<JdbcResource> optional = jdbc.stream().filter(e -> asdfadsfadfs.equals(e.getName())).findFirst();
			if(optional.isPresent()) {
				JdbcResource resource = optional.get();

				if(StringUtils.isEmpty(resource.getUrl())) {
					throw new IllegalStateException("field url is required");
				}

				Class<?> clazz = ClassUtils.loadClass(resource.getType());
				if(!lookupClass.isAssignableFrom(clazz)) {
					throw new IllegalStateException("class is not of required type ["+resource.getType()+"]");
				}

				Properties mergedProps = new Properties();
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


				try {
					O dataSource = (O) clazz.getDeclaredConstructor().newInstance();

					for(Method method: clazz.getMethods()) {
						if(!method.getName().startsWith("set") || method.getParameterTypes().length != 1)
							continue;

						String fieldName = StringUtil.lcFirst(method.getName().substring(3));
						String value = mergedProps.getProperty(fieldName);

						if("url".equalsIgnoreCase(fieldName)) {

							ClassUtils.invokeSetter(dataSource, method, StringResolver.substVars(resource.getUrl(), APP_CONSTANTS));
						} else if(StringUtils.isNotEmpty(value)) {
							ClassUtils.invokeSetter(dataSource, method, value);
						}
					}

					return dataSource;
				} catch (Exception e) {
					throw new IllegalStateException("unable to create database driver");
				}
			}
		}
		return null;
	}

	private CredentialFactory getCredentials(JdbcResource resource) {
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
