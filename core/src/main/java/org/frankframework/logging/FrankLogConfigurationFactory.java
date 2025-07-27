/*
   Copyright 2020, 2022-2025 WeAreFrank!

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
package org.frankframework.logging;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;

/**
 * This ConfigurationFactory is loaded after the log4j2.properties file has been initialized.
 * Both Configurations are then combined via a CompositeConfiguration
 * 
 * NOTE:
 * The use of Lombok is not allowed, this may break annotation processing! This is required now that package scanning is no longer allowed.
 * Should not depend on any (util) classes that use a logger!
 *
 * @author Niels Meijer
 */
@Order(1000)
@Plugin(name = "FrankLogConfigurationFactory", category = ConfigurationFactory.CATEGORY)
public class FrankLogConfigurationFactory extends ConfigurationFactory {

	static {
		System.setProperty("java.util.logging.manager", org.apache.logging.log4j.jul.LogManager.class.getCanonicalName());
	}

	@Override
	protected String[] getSupportedTypes() {
		return new String[] {"log4j4ibis.xml"};
	}

	/** Also called when refreshed (see UpdateLogDefinitions) */
	@Override
	public Configuration getConfiguration(LoggerContext loggerContext, ConfigurationSource source) {
		return new XmlConfiguration(loggerContext, source);
	}

}
