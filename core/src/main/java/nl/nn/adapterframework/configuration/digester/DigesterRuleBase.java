/*
   Copyright 2021 WeAreFrank!

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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.digester3.Rule;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ClassUtils;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;

import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StringResolver;

public abstract class DigesterRuleBase extends Rule implements ApplicationContextAware {
	protected Logger log = LogUtil.getLogger(this);
	private @Setter ApplicationContext applicationContext;

	/**
	 * Returns the name of the object. In case a Spring proxy is being used, 
	 * the name will be something like XsltPipe$$EnhancerBySpringCGLIB$$563e6b5d
	 * ClassUtils.getUserClass() makes sure the original class will be returned.
	 */
	protected String getObjectName() {
		Object o = getBean();
		String result = ClassUtils.getUserClass(o).getSimpleName();
		if (o instanceof INamedObject) { //This assumes that setName has already been called
			String named = ((INamedObject) o).getName();
			if (StringUtils.isNotEmpty(named)) {
				return result+=" ["+named+"]";
			}
		}
		return result;
	}

	/**
	 * Add a configuration warning message to the current configuration
	 */
	protected final void addLocalWarning(String message) {
		Locator loc = getDigester().getDocumentLocator();
		String msg = getObjectName()+ " on line "+loc.getLineNumber()+", col "+loc.getColumnNumber()+" "+message;
		ConfigurationWarnings.add(null, log, msg); //TODO fix this
	}

	/**
	 * Add a global message to the application
	 */
	protected final void addGlobalWarning(String message) {
		String msg = getBeanClass() + " " + message;
		ConfigurationWarnings.addGlobalWarning(log, msg); //TODO fix this
	}

	/**
	 * @return an {@link nl.nn.adapterframework.configuration.classloaders.IConfigurationClassLoader IConfigurationClassLoader}.
	 */
	protected final ClassLoader getClassLoader() {
		if(applicationContext == null) {
			throw new IllegalStateException("ApplicationContext not set");
		}

		return applicationContext.getClassLoader();
	}

	/**
	 * @return the currently handled object, aka TOP object
	 */
	protected final Object getBean() {
		return getDigester().peek();
	}

	/**
	 * @return the resolved class of the current object
	 */
	protected final Class<?> getBeanClass() {
		return ClassUtils.getUserClass(getBean());
	}

	/**
	 * @param value or values to resolve
	 * @return The resolved value(s) using the configuration AppConstants
	 */
	protected final String resolveValue(String value) {
		String result = StringResolver.substVars(value, AppConstants.getInstance(getClassLoader()));
		log.trace("resolved [{}] to [{}] using ClassLoader [{}]", ()->value, ()->result, ()->getClassLoader());
		return result;
	}

	@Override
	public final void begin(String uri, String elementName, Attributes attributes) throws Exception {
		Object top = getBean();

		Map<String, String> map = copyAttrsToMap(attributes);
		if(top instanceof INamedObject) { //We must set the name first, to improve logging and configuration warnings
			String name = map.remove("name");
			BeanUtils.setProperty(top, "name", name);
		}

		handleBean();

		for (String attribute : map.keySet()) {
			if (log.isTraceEnabled()) {
				log.trace("checking attribute ["+attribute+"] on bean ["+getObjectName()+"]");
			}
			handleAttribute(attribute, map.get(attribute), map);
		}
	}

	private Map<String, String> copyAttrsToMap(Attributes attrs) {
		Map<String, String> map = new LinkedHashMap<>(attrs.getLength());
		for (int i = 0; i < attrs.getLength(); ++i) {
			String name = attrs.getLocalName(i);
			if ("".equals(name)) {
				name = attrs.getQName(i);
			}
			if(name != null && !name.equals("className")) {
				String value = attrs.getValue(i);
				map.put(name, value);
			}
		}
		return map;
	}

	protected abstract void handleBean();

	protected abstract void handleAttribute(String name, String value, Map<String, String> attributes) throws Exception;
}
