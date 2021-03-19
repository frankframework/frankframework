package nl.nn.adapterframework.util;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

public class SpringUtils {

	@SuppressWarnings("unchecked")
	public static <T> T createBean(ApplicationContext applicationContext, Class<T> beanClass) {
		return (T) applicationContext.getAutowireCapableBeanFactory().createBean(beanClass, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
	}

	public static <T> void autowire(ApplicationContext applicationContext, Object existingBean) {
		applicationContext.getAutowireCapableBeanFactory().autowireBeanProperties(existingBean, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
	}

}
