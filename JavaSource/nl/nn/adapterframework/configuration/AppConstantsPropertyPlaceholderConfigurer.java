/*
 * $Log: AppConstantsPropertyPlaceholderConfigurer.java,v $
 * Revision 1.3  2012-09-07 13:15:17  m00f069
 * Messaging related changes:
 * - Use CACHE_CONSUMER by default for ESB RR
 * - Don't use JMSXDeliveryCount to determine whether message has already been processed
 * - Added maxDeliveries
 * - Delay wasn't increased when unable to write to error store (it was reset on every new try)
 * - Don't call session.rollback() when isTransacted() (it was also called in afterMessageProcessed when message was moved to error store)
 * - Some cleaning along the way like making some synchronized statements unnecessary
 * - Made BTM and ActiveMQ work for testing purposes
 *
 */
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

	public AppConstantsPropertyPlaceholderConfigurer() {
		setIgnoreUnresolvablePlaceholders(true);
	}
	
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
