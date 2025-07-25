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

import java.io.File;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.status.StatusLogger;

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
	private static final Logger LOGGER = StatusLogger.getLogger();
	private static final Marker LOOKUP = MarkerManager.getMarker("FRANK-LOG-CONFIG");

	static {
		System.setProperty("java.util.logging.manager", org.apache.logging.log4j.jul.LogManager.class.getCanonicalName());
		setLogDir();
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

	/**
	 * Checks if log.dir property exists.
	 * Sets it with findLogDir function.
	 */
	private static void setLogDir() {
		if (System.getProperty("log.dir") == null) {
			File logDir = findLogDir();
			if (logDir != null) {
				System.setProperty("log.dir", fixLogDirectorySlashes(logDir.getPath()));
			} else {
				LOGGER.warn(LOOKUP, "did not find system property log.dir and unable to locate it automatically");
			}
		}
	}

	/**
	 * Replace backslashes because log.dir is used in log4j2.xml
	 * on which substVars is done (see below) which will replace
	 * double backslashes into one backslash and after that the same
	 * is done by Log4j:
	 * https://issues.apache.org/bugzilla/show_bug.cgi?id=22894
	 * */
	private static String fixLogDirectorySlashes(String directory) {
		return directory.replace("\\", "/");
	}

	/**
	 * Hierarchy of log directories to search for. Strings will be split by "/".
	 * Before "/" split will be assumed to be a property, and after the split will be a (sub-) directory.
	 */
	private static List<String> getDefaultLogDirectories() {
		return List.of("site.logdir", "user.dir/logs", "user.dir/log", "jboss.server.base.dir/log", "wtp.deploy/../logs", "catalina.base/logs");
	}

	/**
	 * Finds the first directory in the given hierarchy.
	 * @see #getDefaultLogDirectories()
	 * @return File object that is a directory. Or null, if no directories were found.
	 */
	private static File findLogDir() {
		for(String option : getDefaultLogDirectories()) {
			int splitIndex = option.indexOf('/');

			String property = System.getProperty(option.substring(0, splitIndex == -1 ? option.length() : splitIndex));
			if(property == null)
				continue;

			File dir;
			if(splitIndex == -1) {
				dir = new File(property);
			} else {
				dir = new File(property, option.substring(splitIndex));
			}

			if(dir.isDirectory())
				return dir;
		}
		return null;
	}
}
