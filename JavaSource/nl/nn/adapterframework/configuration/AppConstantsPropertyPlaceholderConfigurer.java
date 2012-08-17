package nl.nn.adapterframework.configuration;

import java.util.Properties;

import nl.nn.adapterframework.util.AppConstants;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

/**
 * Make AppConstants properties available to the Spring configuration.
 * 
 * @author Jaco de Groot
 */
public class AppConstantsPropertyPlaceholderConfigurer
		extends PropertyPlaceholderConfigurer {
	protected AppConstants appConstants;

	protected void convertProperties(Properties props) {
		props.putAll(appConstants);
	}

	public AppConstants getAppConstants() {
		return appConstants;
	}

	public void setAppConstants(AppConstants appConstants) {
		this.appConstants = appConstants;
	}

}
