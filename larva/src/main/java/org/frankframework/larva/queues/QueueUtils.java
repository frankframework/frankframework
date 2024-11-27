/*
   Copyright 2022-2023 WeAreFrank!

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
package org.frankframework.larva.queues;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import org.frankframework.core.IConfigurable;
import org.frankframework.core.INamedObject;
import org.frankframework.http.AbstractHttpSender;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StringUtil;

/**
 * Reflection helper to create Larva Queues'
 *
 * When a class is created it will attempt to set the name and disable HTTP SSL capabilities by default
 * When setting the bean properties it loops through the available setter methods and looks for a matching property.
 *
 * @author Niels Meijer
 */
public class QueueUtils {
	private static final Logger LOG = LogUtil.getLogger(QueueUtils.class);

	public static IConfigurable createInstance(ClassLoader classLoader, String className) {
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(classLoader);
		try {
			return createInstance(className);
		} finally {
			if (originalClassLoader != null) {
				Thread.currentThread().setContextClassLoader(originalClassLoader);
			}
		}
	}

	private static IConfigurable createInstance(String className) {
		LOG.debug("instantiating queue [{}]", className);
		try {
			Class<?> clazz = ClassUtils.loadClass(className);
			Constructor<?> con = ClassUtils.getConstructorOnType(clazz, new Class[] {});
			Object obj = con.newInstance();

			if(obj instanceof INamedObject object) { //Set the name
				object.setName("Test Tool "+clazz.getSimpleName());
			}

			if(obj instanceof AbstractHttpSender base) { //Disable SSL capabilities
				base.setAllowSelfSignedCertificates(true);
				base.setVerifyHostname(false);
			}

			return (IConfigurable) obj;
		}
		catch (Exception e) {
			throw new IllegalStateException("unable to initialize class ["+className+"]", e);
		}
	}

	public static Properties getSubProperties(Properties properties, String keyBase) {
		if(!keyBase.endsWith("."))
			keyBase +=".";

		Properties filteredProperties = new Properties();
		for(Object objKey: properties.keySet()) {
			String key = (String) objKey;
			if(key.startsWith(keyBase)) {
				filteredProperties.put(key.substring(keyBase.length()), properties.get(key));
			}
		}

		return filteredProperties;
	}

	public static void invokeSetters(Object clazz, Properties queueProperties) {
		for(Method method: clazz.getClass().getMethods()) {
			if(!method.getName().startsWith("set") || method.getParameterTypes().length != 1)
				continue;

			String setter = StringUtil.lcFirst(method.getName().substring(3));
			String value = queueProperties.getProperty(setter);
			if(value == null)
				continue;

			try {
				ClassUtils.invokeSetter(clazz, method, value);
			} catch (Exception e) {
				throw new IllegalArgumentException("unable to set method ["+setter+"] on Class ["+ClassUtils.nameOf(clazz)+"]: "+e.getMessage(), e);
			}
		}
	}
}
