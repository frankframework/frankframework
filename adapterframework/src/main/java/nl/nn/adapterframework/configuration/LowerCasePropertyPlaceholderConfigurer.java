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
