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
 * Make a lower case variant of the instance.name property (instance.name.lc)
 * available to the Ibis configuration and the Spring configuration.
 *
 * @author Jaco de Groot
 */
public class LowerCasePropertySourcePostProcessor extends AbstractPropertySourcePostProcessor {

	@Override
	protected void convertProperties(Properties props) {
		AppConstants appConstants = AppConstants.getInstance();
		String instanceName = appConstants.getProperty("instance.name");
		if (instanceName != null) {
			String lowerCase = instanceName.toLowerCase();
			AppConstants.setGlobalProperty("instance.name.lc", lowerCase);
			props.put("instance.name.lc", lowerCase);
		}
	}
}
