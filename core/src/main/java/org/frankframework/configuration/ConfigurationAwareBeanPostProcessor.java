/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.configuration;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Similar to {@link org.springframework.context.support.ApplicationContextAwareProcessor} sets 
 * the {@link Configuration} class on the {@link ConfigurationAware} beans.
 */
public class ConfigurationAwareBeanPostProcessor implements BeanPostProcessor {
	private final Configuration configuration;
	public ConfigurationAwareBeanPostProcessor(Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof ConfigurationAware awareBean) {
			awareBean.setConfiguration(configuration);
		}
		return bean;
	}
}
