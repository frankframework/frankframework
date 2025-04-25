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
	 * Create bean without passing the bean-class. Can be used when the compiler can statically determine the class from the variable to which the bean is assigned.
	 * Do not pass actual argument to reified, Java will auto-detect the class of the bean type.
	 */
	@SafeVarargs
	@SuppressWarnings({ "unchecked", "deprecation" })
	public static <T> T createBean(ApplicationContext applicationContext, T... reified) {
		if (reified.length > 0) {
			throw new IllegalArgumentException("Do not pass any actual arguments to the reified parameter");
		}
		return (T) applicationContext.getAutowireCapableBeanFactory().createBean(getClassOf(reified), AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
	}


	/**
	 * This is effectively a superset of what autowire provides, adding initializeBean behavior.
	 * NB: Even though this has been deprecated, we cannot use the new/alternative method due to the autowireByName capability.
	 */
	@SuppressWarnings({ "unchecked", "deprecation" })
	public static <T> T createBean(ApplicationContext applicationContext, Class<T> beanClass) {
		return (T) applicationContext.getAutowireCapableBeanFactory().createBean(beanClass, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
	}

	public static Object createBean(ApplicationContext applicationContext, String className) throws ClassNotFoundException {
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

		ClassLoader classLoader = applicationContext.getClassLoader();
		Thread.currentThread().setContextClassLoader(classLoader);

		try {
			Class<?> beanClass = Class.forName(className, true, classLoader);
			return createBean(applicationContext, beanClass);
		} finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
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

	@SuppressWarnings("unchecked")
	public static <T> Class<T> getClassOf(T[] array) {
		return (Class<T>) array.getClass().getComponentType();
	}
}
