/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.jta.narayana;

import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;

import com.arjuna.common.util.propertyservice.PropertiesFactory;
import com.arjuna.common.util.propertyservice.PropertiesFactoryStax;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

public class NarayanaConfigurationBean implements InitializingBean {
	protected Logger log = LogUtil.getLogger(this);

	private Properties customProperties;

	private class NarayanaPropertiesFactory extends PropertiesFactoryStax {
		@Override
		public Properties getPropertiesFromFile(String propertyFileName, ClassLoader classLoader) {
			Properties properties = super.getPropertiesFromFile(propertyFileName, classLoader); //Loads the default narayana-jta.jar/jbossts-properties.xml properties.
			properties.putAll(AppConstants.getInstance()); //Override with properties set in the Ibis
			properties.putAll(customProperties); //Override with spring configured properties
			return properties;
		}
	}

	/**
	 * Populate all the jBossTS EnvironmentBeans.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		PropertiesFactory.setDelegatePropertiesFactory(new NarayanaPropertiesFactory());
	}

	public void setProperties(Properties properties) {
		this.customProperties = properties;
	}
}
