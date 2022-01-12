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

package nl.nn.adapterframework.logging;

import java.io.IOException;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;

//@Plugin(name = "IbisLoggerConfigurationFactory", category = ConfigurationFactory.CATEGORY)
public class IbisLoggerConfigurationFactory extends ConfigurationFactory {
	@Override
	protected String[] getSupportedTypes() {
		return new String[] {".xml"};
	}

	@Override
	public Configuration getConfiguration(LoggerContext loggerContext, ConfigurationSource source) {
		try {
			return new XmlConfiguration(loggerContext, source.resetInputStream()) { //We have to 'reset' the source as the old stream has been read.
	
				@Override // Add hashcode to toString() so we can differentiate the XmlConfigurations in the startup log
				public String toString() {
					return this.getClass().getCanonicalName() + "@" + Integer.toHexString(this.hashCode()) + "[location=" + getConfigurationSource() + "]";
				}
			};
		} catch(IOException e) {
			throw new IllegalStateException("Unable to configure log4j2", e);
		}
	}
}
