package org.frankframework.testutil;

import java.util.Properties;

import jakarta.annotation.Nonnull;

import org.springframework.beans.factory.BeanClassLoaderAware;

import org.frankframework.lifecycle.AbstractPropertySourcePostProcessor;
import org.frankframework.util.AppConstants;

public class AppConstantsPropertySource extends AbstractPropertySourcePostProcessor implements BeanClassLoaderAware {
	private ClassLoader classLoader;

	@Override
	protected void convertProperties(Properties props) {
		props.putAll(AppConstants.getInstance(classLoader));
	}

	@Override
	public void setBeanClassLoader(@Nonnull ClassLoader classLoader) {
		this.classLoader = classLoader;
	}

}
