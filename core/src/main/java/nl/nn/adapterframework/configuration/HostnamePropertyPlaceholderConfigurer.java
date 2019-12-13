/*
   Copyright 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.configuration;

import java.util.Properties;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

 /**
 * Make the hostname property available to the Ibis configuration and the Spring
 * configuration.
 *
 * @author Jaco de Groot
 */
public class HostnamePropertyPlaceholderConfigurer
		extends PropertyPlaceholderConfigurer {
	private static String HOSTNAME_PROPERTY = "hostname";

	public HostnamePropertyPlaceholderConfigurer() {
		setIgnoreUnresolvablePlaceholders(true);
	}

	@Override
	protected void convertProperties(Properties props) {
		AppConstants appConstants = AppConstants.getInstance();
		String hostname = appConstants.getResolvedProperty(HOSTNAME_PROPERTY);
		if (StringUtils.isEmpty(hostname)) {
			hostname = Misc.getHostname();
			appConstants.setProperty(HOSTNAME_PROPERTY, hostname);
			props.put(HOSTNAME_PROPERTY, hostname);
		}
	}

}
