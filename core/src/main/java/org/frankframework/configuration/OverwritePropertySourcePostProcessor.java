/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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
package org.frankframework.configuration;

import java.util.Properties;

import org.frankframework.lifecycle.AbstractPropertySourcePostProcessor;
import org.frankframework.util.AppConstants;

/**
 * Overwrite a property available to the Ibis configuration and the Spring
 * configuration. When the property isn't present it will be added.
 *
 * @author Jaco de Groot
 */
public class OverwritePropertySourcePostProcessor extends AbstractPropertySourcePostProcessor {
	private String propertyName;
	private String propertyValue;

	@Override
	protected void convertProperties(Properties props) {
		AppConstants appConstants = AppConstants.getInstance();
		appConstants.setProperty(propertyName, propertyValue);
		props.put(propertyName, propertyValue);
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public void setPropertyValue(String propertyValue) {
		this.propertyValue = propertyValue;
	}
}
