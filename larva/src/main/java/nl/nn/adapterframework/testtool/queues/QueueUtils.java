/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.testtool.queues;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Properties;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.http.HttpSenderBase;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.LogUtil;

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

	@SuppressWarnings("unchecked")
	public static <T extends IConfigurable> T createInstance(Class<T> clazz) {
		return (T) createInstance(clazz.getCanonicalName());
	}

	public static IConfigurable createInstance(String className) {
		LOG.debug("instantiating queue [{}]", className);
		try {
			Class<?> clazz = ClassUtils.loadClass(className);
			Constructor<?> con = ClassUtils.getConstructorOnType(clazz, new Class[] {});
			Object obj = con.newInstance();

			if(obj instanceof INamedObject) { //Set the name
				((INamedObject) obj).setName("Test Tool "+clazz.getSimpleName());
			}

			if(obj instanceof HttpSenderBase) { //Disable SSL capabilities
				((HttpSenderBase) obj).setAllowSelfSignedCertificates(true);
				((HttpSenderBase) obj).setVerifyHostname(false);
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

			String setter = firstCharToLower(method.getName().substring(3));
			String value = queueProperties.getProperty(setter);
			if(value == null)
				continue;

			//Only always grab the first value because we explicitly check method.getParameterTypes().length != 1
			Object castValue = getCastValue(method.getParameterTypes()[0], value);
			LOG.debug("trying to set property ["+setter+"] with value ["+value+"] of type ["+castValue.getClass().getCanonicalName()+"] on ["+ClassUtils.nameOf(clazz)+"]");

			try {
				method.invoke(clazz, castValue);
			} catch (Exception e) {
				throw new IllegalArgumentException("unable to set method ["+setter+"] on Class ["+ClassUtils.nameOf(clazz)+"]: "+e.getMessage(), e);
			}
		}
	}

	private static Object getCastValue(Class<?> setterArgumentClass, String value) {
		if(setterArgumentClass.isEnum())
			return parseAsEnum(setterArgumentClass, value);//Try to parse the value as an Enum
		else {
			switch (setterArgumentClass.getTypeName()) {
			case "int":
			case "java.lang.Integer":
				return Integer.parseInt(value);
			case "boolean":
			case "java.lang.Boolean":
				return Boolean.parseBoolean(value);
			case "long":
				return Long.parseLong(value);
			default:
				return value;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <E extends Enum<E>> E parseAsEnum(Class<?> enumClass, String value) {
		return EnumUtils.parse((Class<E>) enumClass, value);
	}

	public static String firstCharToLower(String input) {
		return input.substring(0, 1).toLowerCase() + input.substring(1);
	}
}
