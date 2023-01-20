/*
   Copyright 2021, 2022 WeAreFrank!

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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.AnnotationUtils;

import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.HasSpecialDefaultValues;
import nl.nn.adapterframework.configuration.SuppressKeys;
import nl.nn.adapterframework.doc.Protected;
import nl.nn.adapterframework.util.EnumUtils;
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
		PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(getBean(), name);
		Method m=null;
		if (pd!=null) {
			m = PropertyUtils.getWriteMethod(pd);
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
				checkTypeCompatibility(pd, name, value, attributes);
			}

			Object valueToSet = parseValueToSet(m, value);
			log.trace("attempting to populate field [{}] with value [{}] on object [{}]", ()->name, ()->valueToSet, ()->getBean());

			if(valueToSet != null) {
				try {
					BeanUtils.setProperty(getBean(), name, valueToSet);
				} catch (InvocationTargetException e) {
					log.warn("unable to populate field [{}] with value [{}] on object [{}]", name, valueToSet, getBean(), e);
					addLocalWarning(e.getCause().getMessage());
				}
			}
		}
	}

	private Object parseValueToSet(Method m, String value) {
		Class<?> setterArgumentClass = m.getParameters()[0].getType();
		//Try to parse the value as an Enum
		if(setterArgumentClass.isEnum()) {
			char[] c = m.getName().substring(3).toCharArray();
			c[0] = Character.toLowerCase(c[0]);
			String fieldName = new String(c);

			return parseAsEnum(setterArgumentClass, fieldName, value);
		}

		return value;
	}

	/**
	 * Attempt to parse the attributes value as an Enum.
	 * @param enumClass The Enum class used to parse the value
	 * @param fieldName The setter name (fieldName) to set
	 * @param value The value to be parsed
	 * @return The Enum constant or <code>NULL</code> (and a local configuration warning) if it cannot parse the value.
	 */
	@SuppressWarnings("unchecked")
	private <E extends Enum<E>> E parseAsEnum(Class<?> enumClass, String fieldName, String value) {
		try {
			return EnumUtils.parse((Class<E>) enumClass, fieldName, value);
		} catch(IllegalArgumentException e) {
			addLocalWarning(e.getMessage());
			return null;
		}
	}

	/**
	 * Check if the value:,
	 * - Can be parsed to match the Getters return type,
	 * - Does not equal the default value (parsed by invoking the getter, if present).
	 * If no Getter is present, tries to match the type to the Setters first argument.
	 */
	private void checkTypeCompatibility(PropertyDescriptor pd, String name, String value, Map<String, String> attrs) {
		Method rm = PropertyUtils.getReadMethod(pd);
		if (rm != null) {
			try {
				Object bean = getBean();
				Object defaultValue = rm.invoke(bean);
				if (bean instanceof HasSpecialDefaultValues) {
					defaultValue = ((HasSpecialDefaultValues)bean).getSpecialDefaultValue(name, defaultValue, attrs);
				}
				if (equals(defaultValue, value)) {
					addSuppressableWarning("attribute ["+name+"] already has a default value ["+value+"]", SuppressKeys.DEFAULT_VALUE_SUPPRESS_KEY);
				}
				// if the default value is null, then it can mean that the real default value is determined in configure(),
				// so we cannot assume setting it to "" has no effect
			} catch (NumberFormatException e) {
				addLocalWarning("attribute ["+ name+"] with value ["+value+"] cannot be converted to a number: "+e.getMessage());
			} catch (Throwable t) {
				addLocalWarning("is unable to parse attribute ["+name+"] value ["+value+"] to method ["+rm.getName()+"] with type ["+rm.getReturnType()+"]");
				log.warn("Error on getting default for object [" + getObjectName() + "] with method [" + rm.getName() + "] attribute ["+name+"] value ["+value+"]", t);
			}
		} else {
			//No readMethod, thus we cannot check the default value. We can however check if we can parse the value
			if (value.length()==0) {
				// no need to check if we can parse an empty string, but no warning either, as empty string might be an appropriate value
				return;
			}

			//If it's a number (int/long) try to parse it, else BeanUtils will call the method with 0.
			try {
				Class<?> setterArgumentClass = pd.getWriteMethod().getParameters()[0].getType();

				switch (setterArgumentClass.getTypeName()) {
				case "int":
				case "java.lang.Integer":
					Integer.parseInt(value);
					break;
				case "long":
					Long.parseLong(value);
					break;
				}
			} catch(NumberFormatException e) {
				addLocalWarning("attribute ["+name+"] with value ["+value+"] cannot be converted to a number");
			} catch(Exception e) {
				log.debug("unable to get the first setter parameter of attribute["+name+"] writeMethod ["+pd.getWriteMethod()+"]", e);
			}
		}
	}

	/**
	 * Fancy equals that type-checks one value against another.
	 */
	private boolean equals(Object defaultValue, String value) {
		return	(defaultValue instanceof String && value.equals(defaultValue)) ||
				(defaultValue instanceof Boolean && Boolean.valueOf(value).equals(defaultValue)) ||
				(defaultValue instanceof Integer && Integer.valueOf(value).equals(defaultValue)) ||
				(defaultValue instanceof Long && Long.valueOf(value).equals(defaultValue)) ||
				(defaultValue instanceof Enum && enumEquals(defaultValue, value));
	}

	/** Attempt to parse the attribute value as an Enum and compare it to the defaultValue. */
	@SuppressWarnings("unchecked")
	private <E extends Enum<E>> boolean enumEquals(Object defaultValue, String value) {
		Class<?> enumClass = defaultValue.getClass();
		try {
			return EnumUtils.parse((Class<E>) enumClass, value) == defaultValue;
		} catch(IllegalArgumentException e) {
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
