package org.frankframework.testutil;

import java.util.Properties;

import org.frankframework.lifecycle.CustomPropertySourcePostProcessor;
import org.frankframework.util.AppConstants;
import org.springframework.beans.factory.BeanClassLoaderAware;

public class AppConstantsPropertySource extends CustomPropertySourcePostProcessor implements BeanClassLoaderAware {
	private ClassLoader classLoader;

	@Override
	protected void convertProperties(Properties props) {
		props.putAll(AppConstants.getInstance(classLoader));
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

}
