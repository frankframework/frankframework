/*
   Copyright 2013, 2020 Nationale-Nederlanden

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
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.HasSpecialDefaultValues;
import nl.nn.adapterframework.configuration.SuppressKeys;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.StringResolver;

/**
 * Helper class to check that each attribute set from the configuration is available on the 
 * object being configured. 
 * 
 * @author  Gerrit van Brakel
 */
public class ValidateAttributeRule extends AbstractSpringPoweredDigesterRule {
	private boolean suppressDeprecationWarnings = AppConstants.getInstance().getBoolean(SuppressKeys.DEPRECATION_SUPPRESS_KEY.getKey(), false);

	@Override
	protected void handleBean(String beanName, Object bean) {
		if(!suppressDeprecationWarnings) {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			if(!ConfigurationWarnings.isSuppressed(SuppressKeys.DEPRECATION_SUPPRESS_KEY, null, classLoader)) {
				Class<?> clazz = getBeanClass();
				ConfigurationWarning warning = AnnotationUtils.findAnnotation(clazz, ConfigurationWarning.class);
				if(warning != null) {
					String msg = "";
					if(AnnotationUtils.findAnnotation(clazz, Deprecated.class) != null) {
						msg += " is deprecated";
					}
					if(StringUtils.isNotEmpty(warning.value())) {
						msg += ": " + warning.value();
					}
					//Only print it once per deprecated class
					addWarning(msg);
//					ConfigurationWarnings.addGlobalWarning(log, msg, SuppressKeys.DEPRECATION_SUPPRESS_KEY, classLoader);
				}
			}
		}
	}

	@Override
	protected void handleAttribute(String name, String value, Map<String, String> attributes) throws Exception {
		PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(getBean(), name);
		Method m=null;
		if (pd!=null) {
			m = PropertyUtils.getWriteMethod(pd);
		}
		if (m==null) { //validate if the attribute exists
			addWarning("does not have an attribute ["+name+"] to set to value ["+value+"]");
//			ConfigurationWarnings.add(null, log, msg); //We need to use this as it's a configuration specific warning
		} else {
			checkDeprecation(name, m); //check if the setter has been deprecated

			if(!value.startsWith(StringResolver.DELIM_START) && !value.endsWith(StringResolver.DELIM_STOP)) {
				checkReadMethodType(pd, name, value, attributes); //check if the default value is changed
			}

			BeanUtils.setProperty(getBean(), name, value);
		}
	}

	protected void checkReadMethodType(PropertyDescriptor pd, String name, String value, Map<String, String> attrs) {
		Method rm = PropertyUtils.getReadMethod(pd);
		if (rm!=null) {
			try {
				Object bean = getBean();
				Object dv = rm.invoke(bean, new Object[0]);
				if (bean instanceof HasSpecialDefaultValues) {
					dv = ((HasSpecialDefaultValues)bean).getSpecialDefaultValue(name, dv, attrs);
				}
				if (dv!=null) {
					if (value.length()==0) {
						addWarning("attribute ["+name+"] has no value");
						throw new AttributeValidationException(getBean(), ) // "XsltSender [pietjePuk] on line "+loc.getLineNumber()+", col "+loc.getColumnNumber()+": "+message;
					} else if (equals(dv, value)) {
						addWarning("attribute ["+name+"] already has a default value ["+value+"]");
					} else {
						addWarning("unable to parse attribute ["+name+"] value ["+value+"] to type ["+rm.getReturnType()+"]");
					}
				}
			} catch (NumberFormatException e) {
				addWarning("attribute ["+ name+"] with value ["+value+"] cannot be converted to a number: "+e.getMessage());
			} catch (Throwable t) {
				t.printStackTrace();
//				log.warn("Error on getting default for object [" + getObjectName(currObj, beanName) + "] with method [" + rm.getName() + "]", t);
			}
		}
	}

	private boolean equals(Object dv, String value) {
		return	(dv instanceof String && value.equals(dv)) ||
				(dv instanceof Boolean && Boolean.valueOf(value).equals(dv)) ||
				(dv instanceof Integer && Integer.valueOf(value).equals(dv)) ||
				(dv instanceof Long && Long.valueOf(value).equals(dv));
	}

	private void checkDeprecation(String name, Method m) {
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

			if(!(suppressDeprecationWarnings && isDeprecated)) { //Don't log if deprecation warnings are suppressed and it is deprecated
				addWarning(msg);
			}
		}
	}
}
