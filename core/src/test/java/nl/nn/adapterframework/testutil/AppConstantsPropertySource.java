package nl.nn.adapterframework.testutil;

import java.util.Properties;

import org.springframework.beans.factory.BeanClassLoaderAware;

import nl.nn.adapterframework.lifecycle.CustomPropertySourcePostProcessor;
import nl.nn.adapterframework.util.AppConstants;

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
