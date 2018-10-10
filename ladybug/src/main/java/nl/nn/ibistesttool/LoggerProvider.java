/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.ibistesttool;

import java.util.Properties;

import nl.nn.testtool.util.LogUtil;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.RootLogger;

/**
 * @author Jaco de Groot
 */
public class LoggerProvider implements nl.nn.testtool.util.LoggerProvider {
	private static final String DEBUG_LOG_PREFIX = "Ibis Test Tool LoggerProvider class ";
	private static final String DEBUG_LOG_SUFFIX = "";
	private static final String WARN_LOG_PREFIX = DEBUG_LOG_PREFIX;
	private static final String WARN_LOG_SUFFIX = LogUtil.WARN_LOG_SUFFIX;
	public static final String IBIS_INSTANCE_NAME_PROPERTY_KEY = "instance.name";
	public static final String IBIS_INSTANCE_NAME_LC_PROPERTY_KEY = "instance.name.lc";
	private static Hierarchy hierarchy;
	static {
		Properties log4jProperties = LogUtil.getProperties(DEBUG_LOG_PREFIX,
				DEBUG_LOG_SUFFIX, WARN_LOG_PREFIX, WARN_LOG_SUFFIX,
				"log4j4testtool.properties");
		if (log4jProperties != null) {
			Properties dsProperties = LogUtil.getProperties(DEBUG_LOG_PREFIX,
					DEBUG_LOG_SUFFIX, WARN_LOG_PREFIX, WARN_LOG_SUFFIX,
					"DeploymentSpecifics.properties");
			if (dsProperties != null) {
				log4jProperties.put(IBIS_INSTANCE_NAME_LC_PROPERTY_KEY,
						getIbisInstanceNameLowerCase(dsProperties));
				log4jProperties.putAll(dsProperties);
				log4jProperties.put("log4j.rootLogger", log4jProperties.get("log4j4testtool.rootLogger"));
				hierarchy = new Hierarchy(new RootLogger(Level.DEBUG));
				PropertyConfigurator propertyConfigurator = new PropertyConfigurator();
				propertyConfigurator.doConfigure(log4jProperties, hierarchy);
			}
		}
	}
	
	public Logger getLogger(String name) { 
		Logger logger = null;
		if (hierarchy == null) {
			logger = Logger.getLogger(name);
		} else {
			logger = hierarchy.getLogger(name);
		}
		return logger;
	}

	public static String getIbisInstanceNameLowerCase(Properties properties) {
		String instanceNameLowerCase = properties.getProperty(LoggerProvider.IBIS_INSTANCE_NAME_PROPERTY_KEY);
		if (instanceNameLowerCase != null) {
			instanceNameLowerCase = instanceNameLowerCase.toLowerCase();
		} else {
			instanceNameLowerCase = "ibis4unknown";
		}
		return instanceNameLowerCase;
	}

}
