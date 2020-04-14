package nl.nn.adapterframework.util;

import nl.nn.adapterframework.extensions.log4j.IbisLoggerConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;

public class LogUtil {
	public static final String[] decprecated = new String[] {"log4j4ibis.xml"};
	static {
		// Make sure logger configuration is initialized each time servlet starts.
		IbisLoggerConfiguration.init();

		String message = "You seem to be using our old logger configuration file [%s]. " +
				"We have upgraded our logger system, and now using log4j2.xml instead. " +
				"Check out this url for more information: https://logging.apache.org/log4j/2.x/manual/configuration.html";
		for (String f : decprecated) {
			URL url = LogUtil.class.getClassLoader().getResource(f);
			if (url != null)
				System.err.println(String.format(message, f));
		}
	}

	public static Logger getRootLogger() {
		return LogManager.getRootLogger();
	}

	public static Logger getLogger(String name) {
		return LogManager.getLogger(name);
	}

	public static Logger getLogger(Class<?> clazz) {
		return LogManager.getLogger(clazz);
	}

	public static Logger getLogger(Object owner) {
		return LogManager.getLogger(owner);
	}
}
