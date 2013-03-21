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
/*
 * $Log: OverwritePropertyPlaceholderConfigurer.java,v $
 * Revision 1.1  2012-09-07 13:15:17  m00f069
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
