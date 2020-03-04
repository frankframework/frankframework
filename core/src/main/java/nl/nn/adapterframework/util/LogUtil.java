/*
   Copyright 2013, 2019-2020 Nationale-Nederlanden

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
package nl.nn.adapterframework.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Properties;

import com.sun.xml.bind.v2.TODO;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.properties.PropertiesConfiguration;
import org.apache.logging.log4j.core.config.properties.PropertiesConfigurationBuilder;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.core.config.xml.XmlConfigurationFactory;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.pattern.RegexReplacement;


/**
 * Convenience functions for logging.
 * Enables a separate log4j configuartion for each Ibis-instance.
 * Searches first for log4j4ibis.properties on the classpath. If not found, then searches for log4j.properties.
 *
 * @author  Gerrit van Brakel
 * @author  Jaco de Groot (***@dynasol.nl)
 */
public class LogUtil {
	public static final String DEBUG_LOG_PREFIX = "Ibis LogUtil class ";
	public static final String DEBUG_LOG_SUFFIX = "";
	public static final String WARN_LOG_PREFIX = DEBUG_LOG_PREFIX;
	public static final String WARN_LOG_SUFFIX = ", leaving it up to log4j's default initialization procedure: http://logging.apache.org/log4j/docs/manual.html#defaultInit";
	public static final String LOG4J_XML_FILE = "log4j4ibis.xml";
	public static final String LOG4J_PROPS_FILE = "log4j4ibis.properties";

	private static ThreadLocal<String> threadLocal_hideRegex = new ThreadLocal<String>();

	private static Properties log4jProperties;
	private static String hideRegex;
	// This is the hierarchy for log directory.
	// It will check 0th element first, and it will detect before "/" as property, and after "/" as subdirectory.
	private static String[] logDirHierarchy = new String[] {
			"site.logdir",
			"user.dir/logs",
			"user.dir/log",
			"jboss.server.base.dir/log",
			"wtp.deploy/../logs",
			"catalina.base/logs"
	};
	static {
		if (System.getProperty("log.dir") == null) {
			File logDir = findLogDir(logDirHierarchy);
			System.out.println("LOGDIR: " + logDir);
			if (logDir != null) {
				// Replace backslashes because log.dir is used in log4j4ibis.xml
				// on which substVars is done (see below) which will replace
				// double backslashes into one backslash and after that the same
				// is done by Log4j:
				// https://issues.apache.org/bugzilla/show_bug.cgi?id=22894
				System.setProperty("log.dir", logDir.getPath().replaceAll("\\\\", "/"));
			} else {
				System.out.println(DEBUG_LOG_PREFIX + "did not find system property log.dir and unable to locate it automatically");
			}
		}

		if (System.getProperty("log.level") == null) {
			// In the log4j4ibis.xml the rootlogger contains the loglevel: ${log.level}
			// You can set this property in the log4j4ibis.properties, or as system property.
			// To make sure the IBIS can startup if no log.level property has been found, it has to be explicitly set
			String stage = System.getProperty("dtap.stage");
			String logLevel = "DEBUG";
			if("ACC".equalsIgnoreCase(stage) || "PRD".equalsIgnoreCase(stage)) {
				logLevel = "WARN";
			}
			System.setProperty("log.level", logLevel);
		}

		String l4jxml;
		URL url = LogUtil.class.getClassLoader().getResource(LOG4J_XML_FILE);
		System.out.println("LOGDIRURL: " + url.getPath());
		if (url == null) {
			l4jxml = null;
			System.out.println(DEBUG_LOG_PREFIX + "did not find " + LOG4J_XML_FILE + ", will try " + LOG4J_PROPS_FILE + " instead" + DEBUG_LOG_SUFFIX);
		} else {
			try {
				l4jxml = resourceToString(url);
			} catch (IOException e) {
				l4jxml = null;
				System.out.println(DEBUG_LOG_PREFIX + "could not read " + url + " (" + e.getClass().getName() + ": " + e.getMessage() + "), will try " + LOG4J_PROPS_FILE + " instead" + DEBUG_LOG_SUFFIX);
			}
		}
		log4jProperties = getProperties(LOG4J_PROPS_FILE);
		if(log4jProperties == null) {
			System.out.println(WARN_LOG_PREFIX + "did not find " + LOG4J_PROPS_FILE + WARN_LOG_SUFFIX);
		} else {
			Properties dsProperties = getProperties("DeploymentSpecifics.properties");
			if (dsProperties != null) {
				log4jProperties.putAll(dsProperties);
			}
			log4jProperties.putAll(System.getProperties()); //Set these after reading DeploymentSpecifics as we want to override the properties
			setInstanceNameLc(); //Set instance.name.lc for log file names
			try {
				LoggerContext context = (LoggerContext) LogManager.getContext();
				Configuration config;
				if (l4jxml == null) {
					config = new PropertiesConfigurationBuilder()
						.setRootProperties(log4jProperties)
						.build();
				} else {
					l4jxml = StringResolver.substVars(l4jxml, log4jProperties);
					InputStream stream = IOUtils.toInputStream(l4jxml, "UTF-8");
					config = new XmlConfigurationFactory().getConfiguration(context, new ConfigurationSource(stream));
				}
				context.setConfiguration(config);
				PatternLayout l = PatternLayout.newBuilder().withPattern("").build();


			} catch (IOException e) {
				System.err.println("There has been an error while configuring the logger. Continuing with default configuration.");
				e.printStackTrace();
			}
			hideRegex = log4jProperties.getProperty("log.hideRegex");
		}

		if (hideRegex != null) {
			hideRegex = XmlUtils.decodeChars(hideRegex);
		}
	}

	public static Logger getRootLogger() {
		return LogManager.getRootLogger();
	}

	private static void setInstanceNameLc() {
		String instanceNameLowerCase = log4jProperties.getProperty("instance.name");
		if (instanceNameLowerCase != null) {
			instanceNameLowerCase = instanceNameLowerCase.toLowerCase();
		} else {
			instanceNameLowerCase = "ibis";
		}
		log4jProperties.put("instance.name.lc", instanceNameLowerCase);
	}

	public static Logger getLogger(String name) {
		return LogManager.getLogger(name);
	}

	public static Logger getLogger(Class<?> clazz) {
		return getLogger(clazz.getName());
	}

	public static Logger getLogger(Object owner) {
		return getLogger(owner.getClass());
	}

	public static Properties getLog4jProperties() {
		return log4jProperties;
	}

	private static Properties getProperties(String resourceName) {
		Properties properties = null;
		URL url = LogUtil.class.getClassLoader().getResource(resourceName);
		if (url != null) {
			properties = getProperties(url);
		}
		return properties;
	}

	private static Properties getProperties(URL url) {
		Properties properties = new Properties();
		try {
			properties.load(url.openStream());
			if (System.getProperty("log4j.debug") != null) {
				System.out.println(DEBUG_LOG_PREFIX + "loaded properties from " + url.toString() + DEBUG_LOG_SUFFIX);
			}
		} catch (IOException e) {
			properties = null;
			System.out.println(WARN_LOG_PREFIX + "could not read " + url + " (" + e.getClass().getName() + ": " + e.getMessage() + ")" + WARN_LOG_SUFFIX);
		}
		return properties;
	}

	private static String resourceToString(URL url) throws IOException {
		char[] buff = new char[1024];
		Writer stringWriter = new StringWriter();
		InputStream stream = url.openStream();
		try {
			Reader reader = new BufferedReader(StreamUtil.getCharsetDetectingInputStreamReader(stream));
			int n;
			while ((n = reader.read(buff))!=-1) {
				stringWriter.write(buff, 0, n);
			}
			return stringWriter.toString();
		}
		finally {
			stringWriter.close();
			stream.close();
		}
	}

	public static String getLog4jHideRegex() {
		return hideRegex;
	}

	public static void setThreadHideRegex(String hideRegex) {
		threadLocal_hideRegex.set(hideRegex);
	}

	public static String getThreadHideRegex() {
		return threadLocal_hideRegex.get();
	}

	public static void removeThreadHideRegex() {
		threadLocal_hideRegex.remove();
	}

	/**
	 * Finds the first directory in the given hierarchy.
	 * @param hierarchy is an array of Strings.
	 *                  Strings will be split by "/" and before split will be assumed to be property,
	 *                  and after split will be the subdirectory.
	 * @return File object that is a directory. Or null, if no directories were found.
	 */
	private static File findLogDir(String[] hierarchy) {
		for(String option : hierarchy) {
			int splitIndex = option.indexOf('/');

			String property = System.getProperty(
					option.substring(0, (splitIndex == -1) ? option.length() : splitIndex));
			if(property == null)
				continue;

			File dir;
			if(splitIndex == -1) {
				dir = new File(property);
			}else {
				dir = new File(property, option.substring(splitIndex));
			}

			if(dir.isDirectory())
				return dir;
		}
		return null;
	}
}
