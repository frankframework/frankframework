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
package org.frankframework.lifecycle;

import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;

/**
* Property Configurer that adds additional properties to the Spring environment through a custom property-source.
* Implements BeanFactoryPostProcessor so it's executed before other beans are created.
*
* @author Niels Meijer
*/
public abstract class AbstractPropertySourcePostProcessor implements BeanFactoryPostProcessor, PriorityOrdered, EnvironmentAware {
	private static final String CUSTOM_PROPERTIES_PROPERTY_SOURCE_NAME = "CustomPropertySource";
	private ConfigurableEnvironment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = (ConfigurableEnvironment) environment;
	}

	/**
	 * @return Properties either empty or with previously set properties
	 */
	private Properties getCustomProperties() {
		Properties props = new Properties();

		PropertiesPropertySource customPropertySource = (PropertiesPropertySource) environment.getPropertySources().get(CUSTOM_PROPERTIES_PROPERTY_SOURCE_NAME);
		if(customPropertySource != null) {
			props.putAll(customPropertySource.getSource());
		}

		return props;
	}

	private PropertiesPropertySource createPropertySource(Properties properties) {
		return new PropertiesPropertySource(CUSTOM_PROPERTIES_PROPERTY_SOURCE_NAME, properties);
	}

	/**
	 * @param props An empty or previously set {@link Properties} object
	 */
	protected abstract void convertProperties(Properties props);

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		MutablePropertySources propertySources = environment.getPropertySources();

		Properties customProperties = getCustomProperties();

		convertProperties(customProperties);

		PropertiesPropertySource propertySource = createPropertySource(customProperties);
		if(propertySources.contains(CUSTOM_PROPERTIES_PROPERTY_SOURCE_NAME)) {
			propertySources.replace(CUSTOM_PROPERTIES_PROPERTY_SOURCE_NAME, propertySource);
		} else {
			propertySources.addFirst(propertySource);
		}
	}
}
