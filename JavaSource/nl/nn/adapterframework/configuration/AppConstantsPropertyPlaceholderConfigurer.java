package nl.nn.adapterframework.configuration;

import java.util.Properties;

import nl.nn.adapterframework.util.AppConstants;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

/**
 * @author Jaco de Groot
 */
public class AppConstantsPropertyPlaceholderConfigurer
		extends PropertyPlaceholderConfigurer {

	protected void convertProperties(Properties props) {
		super.convertProperties(props); 
		props.putAll(AppConstants.getInstance());
		props.put("instance.name.lc", props.getProperty("instance.name").toLowerCase());
	}

}
