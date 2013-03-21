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
 * $Log: LogUtil.java,v $
 * Revision 1.9  2011-11-30 13:51:48  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.7  2010/09/23 09:18:56  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Print exception class name also when an exception is thrown
 *
 * Revision 1.6  2010/09/21 16:26:48  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Moved log4j4ibis.properties to AF jar
 * Use EnhancedPatternLayout instead of PatternLayout as adviced by PatternLayout javadoc
 *
 * Revision 1.5  2010/09/09 14:02:35  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Upgrade log4j-1.2.7.jar to log4j-1.2.16.jar: RootCategory replaced by RootLogger
 *
 * Revision 1.4  2007/08/30 15:11:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use only hierarchy if log4j4ibis.properties is present
 *
 * Revision 1.3  2007/08/29 15:13:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * enables use of log4j4ibis.properties
 *
 * Revision 1.2  2007/02/12 15:56:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * configure logging hierarchy from log4j.properties
 *
 * Revision 1.1  2007/02/12 14:10:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of LogUtil
 *
 */
package nl.nn.adapterframework.util;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.RootLogger;

/**
 * Convenience functions for logging.
 * Enables a separate log4j configuartion for each Ibis-instance.
 * Searches first for log4j4ibis.properties on the classpath. If not found, then searches for log4j.properties.
 * 
 * @author  Gerrit van Brakel
 * @author  Jaco de Groot (***@dynasol.nl)
 * @version $Id$
 */
public class LogUtil {
	public static final String version="$RCSfile: LogUtil.java,v $  $Revision: 1.9 $ $Date: 2011-11-30 13:51:48 $";
	public static final String DEBUG_LOG_PREFIX = "Ibis LogUtil class ";
	public static final String DEBUG_LOG_SUFFIX = "";
	public static final String WARN_LOG_PREFIX = DEBUG_LOG_PREFIX;
	public static final String WARN_LOG_SUFFIX = ", leaving it up to log4j's default initialization procedure: http://logging.apache.org/log4j/docs/manual.html#defaultInit";

	private static Hierarchy hierarchy=null;
	
	static {
		Properties log4jProperties = getProperties("log4j4ibis.properties");
		if (log4jProperties != null) {
			Properties dsProperties = getProperties("DeploymentSpecifics.properties");
			if (dsProperties != null) {
				String instanceNameLowerCase = dsProperties.getProperty("instance.name");
				if (instanceNameLowerCase != null) {
					instanceNameLowerCase = instanceNameLowerCase.toLowerCase();
				} else {
					instanceNameLowerCase = "ibis4unknown";
				}
				log4jProperties.put("instance.name.lc", instanceNameLowerCase);
				log4jProperties.putAll(dsProperties);
				hierarchy = new Hierarchy(new RootLogger(Level.DEBUG));
				new PropertyConfigurator().doConfigure(log4jProperties, hierarchy);
			}
		}
	}

	public static Logger getRootLogger() { 
		if (hierarchy == null) {
			return Logger.getRootLogger();
		} else {
			return hierarchy.getRootLogger();
		}
	}
	
	public static Logger getLogger(String name) { 
		Logger logger = null;
		if (hierarchy == null) {
			logger = Logger.getLogger(name);
		} else {
			logger = hierarchy.getLogger(name);
		}
		return logger;
	}

	public static Logger getLogger(Class clazz) { 
		return getLogger(clazz.getName());
	}

	public static Logger getLogger(Object owner) { 
		return getLogger(owner.getClass());
	}


	private static Properties getProperties(String resourceName) {
		Properties properties = null;
		URL url = LogUtil.class.getClassLoader().getResource(resourceName);
		if (url == null) {
			System.out.println(WARN_LOG_PREFIX + "did not find " + resourceName + WARN_LOG_SUFFIX);
		} else {
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

}
