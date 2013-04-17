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

 /**
 * Overwrite a property available to the Ibis configuration and the Spring
 * configuration. When the property isn't present it will be added.
 * 
 * @author Jaco de Groot
 */
public class OverwritePropertyPlaceholderConfigurer
		extends AppConstantsPropertyPlaceholderConfigurer {
	private String propertyName;
	private String propertyValue;

	protected void convertProperties(Properties props) {
		appConstants.put(propertyName, propertyValue);
		props.put(propertyName, propertyValue);
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setPropertyValue(String propertyValue) {
		this.propertyValue = propertyValue;
	}
}
