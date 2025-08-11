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
package org.frankframework.configuration.digester;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.core.annotation.AnnotationUtils;

import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.configuration.SuppressKeys;
import org.frankframework.doc.Protected;
import org.frankframework.doc.Unsafe;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.StringResolver;

/**
 * @author Niels Meijer
 */
public class ValidateAttributeRule extends AbstractDigesterRule {

	/**
	 * @see AbstractDigesterRule#handleBean()
	 */
	@Override
	protected void handleBean() {
		addConfigWarning(getBeanClass());
	}

	/**
	 * @see AbstractDigesterRule#handleAttribute(String, String, Map)
	 *
	 * @param name Name of attribute
	 * @param value Attribute Value
	 * @param attributes Map of all attributes
	 */
	@Override
	protected void handleAttribute(String name, String value, Map<String, String> attributes) {
		PropertyDescriptor pd = BeanUtils.getPropertyDescriptor(getBean().getClass(), name);
		Method m = null;
		if (pd != null) {
			m = pd.getWriteMethod();
		}
		if (m == null) { // Validate if the attribute exists
			addLocalWarning("does not have an attribute ["+name+"] to set to value ["+value+"]");
			return;
		}

		if (AnnotationUtils.findAnnotation(m, Protected.class) != null) {
			addLocalWarning("attribute ["+name+"] is protected, cannot be set from configuration");
			return;
		}

		checkDeprecationAndConfigurationWarning(name, value, m); // Check if the setter or enum value is deprecated

		if (value.contains(StringResolver.DELIM_START) && value.contains(StringResolver.DELIM_STOP)) { // If value contains a property, resolve it
			value = resolveValue(value);
		} else { // Only check for default values for non-property values
			Method readMethod = pd.getReadMethod();
			if(readMethod != null) { // And if a read method (getter) exists
				Object defaultValue = getDefaultValue(readMethod, name, value, attributes);

				if (equals(defaultValue, value)) {
					addSuppressibleWarning("attribute ["+name+"] already has a default value ["+value+"]", SuppressKeys.DEFAULT_VALUE_SUPPRESS_KEY);
				} else {
					// Contains read method, value does not equal the default, check if method is unsafe.
					checkIfMethodIsMarkedAsUnsafe(m, name);
				}
			} else {
				// No read method, thus no default value, always check if method is unsafe.
				checkIfMethodIsMarkedAsUnsafe(m, name);
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

	/**
	 * Check if the value:,
	 * - Can be parsed to match the Getters return type,
	 * - Does not equal the default value (parsed by invoking the getter, if present).
	 * If no Getter is present, tries to match the type to the Setters first argument.
	 */
	@Nullable
	private Object getDefaultValue(Method readMethod, String name, String value, Map<String, String> attrs) {
		try {
			Object bean = getBean();
			return readMethod.invoke(bean);
			// If the default value is null, then it can mean that the real default value is determined in configure(),
			// so we cannot assume setting it to "" has no effect
		} catch (Exception e) {
			addLocalWarning("is unable to parse attribute ["+name+"] value ["+value+"] to method ["+readMethod.getName()+"] with type ["+readMethod.getReturnType()+"]");
			log.warn("Error on getting default for object [{}] with method [{}] attribute [{}] value [{}]", getObjectName(), readMethod.getName(), name, value, e);
			return null;
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

	private void checkDeprecationAndConfigurationWarning(String name, String value, Method setterMethod) {
		addConfigWarning(setterMethod, name);

		// Check enum Configuration Warnings
		Class<?> setterArgumentClass = setterMethod.getParameters()[0].getType();
		if (!setterArgumentClass.isEnum()) // only first parameter is relevant to check
			return; // Skip non-enum setters
		try {
			Object o = ClassUtils.convertToType(setterArgumentClass, value);
			if (o instanceof Enum<?> enumValue) {
				addConfigWarning(enumValue, name);
			}
		} catch (IllegalArgumentException ignored) { // Can not happen with enums
		}
	}

	private void checkIfMethodIsMarkedAsUnsafe(Method setterMethod, String attributeName) {
		Unsafe unsafe = AnnotationUtils.findAnnotation(setterMethod, Unsafe.class);

		if (unsafe == null) {
			return;
		}

		String warning = "[" + attributeName + "] is unsafe and should not be used in a production environment";
		addSuppressibleWarning(warning, SuppressKeys.UNSAFE_ATTRIBUTE_SUPPRESS_KEY);
	}

	private void addConfigWarning(Class<?> clazz) {
		ConfigurationWarning warning = AnnotationUtils.findAnnotation(clazz, ConfigurationWarning.class);
		if(warning != null) {
			Deprecated deprecated = AnnotationUtils.findAnnotation(clazz, Deprecated.class);
			addConfigWarning(warning.value(), deprecated, null);
		}
	}

	private void addConfigWarning(Method setterMethod, String attributeName) {
		ConfigurationWarning warning = AnnotationUtils.findAnnotation(setterMethod, ConfigurationWarning.class);
		if(warning != null) {
			Deprecated deprecated = AnnotationUtils.findAnnotation(setterMethod, Deprecated.class);
			addConfigWarning(warning.value(), deprecated, attributeName);
		}
	}

	private void addConfigWarning(Enum<?> enumValue, String attributeName) {
		ConfigurationWarning warning = EnumUtils.findAnnotation(enumValue, ConfigurationWarning.class);
		if(warning != null) {
			Deprecated deprecatedEnum = EnumUtils.findAnnotation(enumValue, Deprecated.class);
			addConfigWarning(warning.value(), deprecatedEnum, attributeName + "." + enumValue);
		}
	}

	/**
	 * Creates a formatted configuration warning.
	 * @param warningMessage the {@link ConfigurationWarning} used to log
	 * @param deprecated enriches the configuration warning using deprecated value/since/forRemoval text.
	 * @param attributeName attribute to enrich the configuration warning
	 */
	private void addConfigWarning(@Nullable String warningMessage, @Nullable Deprecated deprecated, @Nullable String attributeName) {
		List<String> messageBuilder = new ArrayList<>();

		if (StringUtils.isNotEmpty(attributeName)) {
			messageBuilder.add("attribute [" + attributeName + "]");
		}

		if (deprecated != null) {
			String since = deprecated.since();
			if(StringUtils.isNotEmpty(since)) {
				messageBuilder.add("has been deprecated since v" + since);
			} else {
				messageBuilder.add("is deprecated");
			}
			if(deprecated.forRemoval()) {
				messageBuilder.add("and has been marked for removal");
			}
		}

		String msg = StringUtils.join(messageBuilder, " ");
		if (StringUtils.isNotEmpty(warningMessage)) {
			msg += ": " + warningMessage;
		}

		if (deprecated != null) {
			addSuppressibleWarning(msg, SuppressKeys.DEPRECATION_SUPPRESS_KEY);
		} else {
			addLocalWarning(msg);
		}
	}
}
