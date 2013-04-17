/*
   Copyright 2013 Nationale-Nederlanden

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
