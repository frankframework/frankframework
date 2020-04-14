package nl.nn.adapterframework.extensions.log4j;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;

/**
 * An extension of {@link XmlConfiguration} for log4j2.
 * It makes sure that properties set for Ibis are available
 * to log4j2 as a context map. So any property set can be used
 * for configuration.
 */
public class IbisLoggerConfiguration extends XmlConfiguration{

	public static final String DEBUG_LOG_PREFIX = "Ibis LogUtil class ";
	private static final String LOG4J_PROPS_FILE = "log4j4ibis.properties";
	private static final String DS_PROPERTIES_FILE = "DeploymentSpecifics.properties";
	private static String[] logDirectoryHierarchy = new String[] {
			"site.logdir",
			"user.dir/logs",
			"user.dir/log",
			"jboss.server.base.dir/log",
			"wtp.deploy/../logs",
			"catalina.base/logs"
	};

	public IbisLoggerConfiguration(final LoggerContext context, final ConfigurationSource configSource) {
		super(context, configSource);
	}

	@Override
	protected void doConfigure() {
		try {
			setThreadContextMap(LOG4J_PROPS_FILE);
			setThreadContextMap(DS_PROPERTIES_FILE);
			init();
			setThreadContextMap(System.getProperties());
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.doConfigure();
	}

	/**
	 * Adds the properties from the given file  to
	 * {@link ThreadContext} for Log4j2 to lookup strings.
	 * @param filename Name of file that contains properties to be added.
	 * @throws IOException If properties can not be read.
	 */
	private void setThreadContextMap(String filename) throws IOException {
		setThreadContextMap(getProperties(filename));
	}

	/**
	 * Adds the properties given to {@link ThreadContext} for Log4j2
	 * to lookup strings.
	 * @param properties Properties to be added.
	 * @see <a href="https://logging.apache.org/log4j/2.x/manual/lookups.html">Log4j2 Lookups</a>
	 */
	private void setThreadContextMap(Properties properties) {
		for(String key : properties.stringPropertyNames()) {
			String value = properties.getProperty(key);
			System.out.println("Adding key: " + key + " - value: " + value);
			if(!ThreadContext.containsKey(key))
				ThreadContext.put(key, value);
		}
	}

	/**
	 * Loads the properties from filename
	 * @param filename Filename of the properties file.
	 * @return Properties that are in the file.
	 * @throws IOException If properties can not be read.
	 */
	private Properties getProperties(String filename) throws IOException {
		URL url = IbisLoggerConfiguration.class.getClassLoader().getResource(filename);
		Properties properties = new Properties();
		properties.load(url.openStream());
		return properties;
	}

	/**
	 * Converts instance.name property to lower case.
	 */
	private static void setInstanceNameLc() {
		String instanceNameLowerCase = ThreadContext.get("instance.name");
		if (instanceNameLowerCase != null) {
			instanceNameLowerCase = instanceNameLowerCase.toLowerCase();
		} else {
			instanceNameLowerCase = "ibis";
		}
		ThreadContext.put("instance.name.lc", instanceNameLowerCase);
	}

	/**
	 * Checks if log.dir property exists.
	 * Sets it with findLogDir function.
	 */
	private static void setLogDir() {
		if (System.getProperty("log.dir") == null) {
			File logDir = findLogDir();
			if (logDir != null) {
				// Replace backslashes because log.dir is used in log4j2.xml
				// on which substVars is done (see below) which will replace
				// double backslashes into one backslash and after that the same
				// is done by Log4j:
				// https://issues.apache.org/bugzilla/show_bug.cgi?id=22894
				System.setProperty("log.dir", logDir.getPath().replaceAll("\\\\", "/"));
			} else {
				System.out.println(DEBUG_LOG_PREFIX + "did not find system property log.dir and unable to locate it automatically");
			}
		}
	}

	/**
	 * Checks if the log.level is set on system level.
	 * If not set, sets it based on dtap.stage
	 */
	private static void setLevel() {
		if (System.getProperty("log.level") == null) {
			// In the log4j2.xml the rootlogger contains the loglevel: ${log.level}
			// You can set this property in the log4j4ibis.properties, or as system property.
			// To make sure the IBIS can startup if no log.level property has been found, it has to be explicitly set
			String stage = System.getProperty("dtap.stage");
			String logLevel = "DEBUG";
			if("ACC".equalsIgnoreCase(stage) || "PRD".equalsIgnoreCase(stage)) {
				logLevel = "WARN";
			}
			System.setProperty("log.level", logLevel);
		}
		Configurator.setRootLevel(Level.getLevel(System.getProperty("log.level")));
	}

	/**
	 * Finds the first directory in the given hierarchy.
	 * @param hierarchy is an array of Strings.
	 *                  Strings will be split by "/" and before split will be assumed to be property,
	 *                  and after split will be the subdirectory.
	 * @return File object that is a directory. Or null, if no directories were found.
	 */
	private static File findLogDir() {
		for(String option : logDirectoryHierarchy) {
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

	public static void init() {
		setInstanceNameLc();
		setLogDir();
		setLevel();
	}
}
