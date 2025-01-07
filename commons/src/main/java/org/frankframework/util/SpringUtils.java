/*
   Copyright 2021-2025 WeAreFrank!

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

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;

public class SpringUtils {

	/**
	 * This is effectively a supersetof what autowire provides, adding initializeBean behavior.
	 * NB: Even though this has been deprecated, we cannot use the new/alternative method due to the autowireByName capability.
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	public static <T> T createBean(ApplicationContext applicationContext, Class<T> beanClass) {
		return (T) applicationContext.getAutowireCapableBeanFactory().createBean(beanClass, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
	}

	public static void autowireByType(ApplicationContext applicationContext, Object existingBean) {
		autowire(applicationContext, existingBean, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE);
	}

	public static void autowireByName(ApplicationContext applicationContext, Object existingBean) {
		autowire(applicationContext, existingBean, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME);
	}

	public static void autowire(ApplicationContext applicationContext, Object existingBean, int autowireMode) {
		if (applicationContext == null) {
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
