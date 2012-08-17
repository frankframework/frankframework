package nl.nn.adapterframework.configuration;

import java.util.Properties;

import nl.nn.adapterframework.util.AppConstants;

 /**
 * Make a lower case variant of the instance.name property (instance.name.lc)
 * available to the Ibis configuration and the Spring configuration.
 * 
 * @author Jaco de Groot
 */
public class LowerCasePropertyPlaceholderConfigurer
		extends AppConstantsPropertyPlaceholderConfigurer {

	protected void convertProperties(Properties props) {
		String lowerCase = appConstants.getProperty("instance.name").toLowerCase();
		appConstants.put("instance.name.lc", lowerCase);
		props.put("instance.name.lc", lowerCase);
	}

}
