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
package org.frankframework.util;

import lombok.NoArgsConstructor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class SpringUtils {

	public static <T> T createBean(ApplicationContext applicationContext, Class<T> beanClass) {
		T bean = applicationContext.getAutowireCapableBeanFactory().createBean(beanClass);
		autowireByName(applicationContext, bean);
		return bean;
	}

	public static void autowireByType(ApplicationContext applicationContext, Object existingBean) {
		autowire(applicationContext, existingBean, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE);
	}

	public static void autowireByName(ApplicationContext applicationContext, Object existingBean) {
		autowire(applicationContext, existingBean, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME);
	}

	public static void autowire(ApplicationContext applicationContext, Object existingBean, int autowireMode) {
		if (applicationContext==null) {
			throw new NullPointerException("ApplicationContext not set");
		}

		applicationContext.getAutowireCapableBeanFactory().autowireBeanProperties(existingBean, autowireMode, false);
		applicationContext.getAutowireCapableBeanFactory().initializeBean(existingBean, existingBean.getClass().getCanonicalName());
	}

	public static void registerSingleton(ApplicationContext applicationContext, String name, Object bean) {
		ConfigurableBeanFactory cbf = (ConfigurableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
		cbf.registerSingleton(name, bean);
	}
}
