/*
   Copyright 2021-2023 WeAreFrank!

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
package nl.nn.adapterframework.configuration.digester;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;

import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.HasSpecialDefaultValues;
import nl.nn.adapterframework.configuration.SuppressKeys;
import nl.nn.adapterframework.doc.Protected;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.StringResolver;

/**
 * @author Niels Meijer
 */
public class ValidateAttributeRule extends DigesterRuleBase {

	/**
	 * @see DigesterRuleBase#handleBean()
	 */
	@Override
	protected void handleBean() {
			Class<?> clazz = getBeanClass();
			ConfigurationWarning warning = AnnotationUtils.findAnnotation(clazz, ConfigurationWarning.class);
			if(warning != null) {
				String msg = "";
				boolean isDeprecated = AnnotationUtils.findAnnotation(clazz, Deprecated.class) != null;
				if(isDeprecated) {
					msg += "is deprecated";
				}
				if(StringUtils.isNotEmpty(warning.value())) {
					msg += ": "+warning.value();
				}

				if (isDeprecated) {
					addSuppressableWarning(msg, SuppressKeys.DEPRECATION_SUPPRESS_KEY);
				} else {
					addLocalWarning(msg);
				}
			}
	}

	/**
	 * @see DigesterRuleBase#handleAttribute(String, String, Map)
	 *
	 * @param name Name of attribute
	 * @param value Attribute Value
	 * @param attributes Map of all attributes
	 * @throws Exception Can throw any exception in bean property manipulation.
	 */
	@Override
	protected void handleAttribute(String name, String value, Map<String, String> attributes) throws Exception {
		PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(getBean().getClass(), name);
		Method m = null;
		if (pd != null) {
			m = pd.getWriteMethod();
		}
		if (m==null) { //validate if the attribute exists
			addLocalWarning("does not have an attribute ["+name+"] to set to value ["+value+"]");
		} else if(AnnotationUtils.findAnnotation(m, Protected.class) != null) {
			addLocalWarning("attribute ["+name+"] is protected, cannot be set from configuration");
		} else {
			checkDeprecationAndConfigurationWarning(name, m); //check if the setter has been deprecated

			if(value.contains(StringResolver.DELIM_START) && value.contains(StringResolver.DELIM_STOP)) { //If value contains a property, resolve it
				value = resolveValue(value);
			} else { //Only check for default values for non-property values
				Method readMethod = pd.getReadMethod();
				if(readMethod != null) { //And if a read method (getter) exists
					checkTypeCompatibility(readMethod, name, value, attributes);
				}
			}

			log.trace("attempting to populate field [{}] with value [{}] on bean [{}]", name, value, getBean());
			try {
				ClassUtils.invokeSetter(getBean(), m, value);
			} catch (IllegalStateException e) {
				log.warn("error while invoking method [{}] with value [{}] on bean [{}]", m, value, getBean(), e);
				addLocalWarning(e.getCause().getMessage());
			} catch (IllegalArgumentException e) {
				log.debug("unable to set attribute [{}] on method [{}] with value [{}]", name, m, value, e);
				// When it's unable to convert to the provided type and:
				// The type is not a String AND The value is empty
				// Do not create a warning.
				if(!"".equals(value)) {
					addLocalWarning(e.getMessage());
				}
			}
		}
	}

	/**
	 * Check if the value:,
	 * - Can be parsed to match the Getters return type,
	 * - Does not equal the default value (parsed by invoking the getter, if present).
	 * If no Getter is present, tries to match the type to the Setters first argument.
	 */
	private void checkTypeCompatibility(Method readMethod, String name, String value, Map<String, String> attrs) {
		try {
			Object bean = getBean();
			Object defaultValue = readMethod.invoke(bean);
			if (bean instanceof HasSpecialDefaultValues) {
				defaultValue = ((HasSpecialDefaultValues)bean).getSpecialDefaultValue(name, defaultValue, attrs);
			}
			if (equals(defaultValue, value)) {
				addSuppressableWarning("attribute ["+name+"] already has a default value ["+value+"]", SuppressKeys.DEFAULT_VALUE_SUPPRESS_KEY);
			}
			// if the default value is null, then it can mean that the real default value is determined in configure(),
			// so we cannot assume setting it to "" has no effect
		} catch (Exception e) {
			addLocalWarning("is unable to parse attribute ["+name+"] value ["+value+"] to method ["+readMethod.getName()+"] with type ["+readMethod.getReturnType()+"]");
			log.warn("Error on getting default for object [" + getObjectName() + "] with method [" + readMethod.getName() + "] attribute ["+name+"] value ["+value+"]", e);
		}
	}

	/** Fancy equals that type-checks the attribute value against the defaultValue. */
	private boolean equals(Object defaultValue, String value) {
		if(defaultValue == null) {
			return false;
		}

		try {
			return ClassUtils.convertToType(defaultValue.getClass(), value).equals(defaultValue);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private void checkDeprecationAndConfigurationWarning(String name, Method m) {
		ConfigurationWarning warning = AnnotationUtils.findAnnotation(m, ConfigurationWarning.class);
		if(warning != null) {
			String msg = "attribute ["+name+"]";
			boolean isDeprecated = AnnotationUtils.findAnnotation(m, Deprecated.class) != null;

			if(isDeprecated) {
				msg += " is deprecated";
			}

			if(StringUtils.isNotEmpty(warning.value())) {
				msg += ": " + warning.value();
			}

			if (isDeprecated) {
				addSuppressableWarning(msg, SuppressKeys.DEPRECATION_SUPPRESS_KEY);
			} else {
				addLocalWarning(msg);
			}
		}
	}
}
