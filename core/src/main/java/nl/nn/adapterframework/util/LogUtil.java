package nl.nn.adapterframework.util;

import nl.nn.adapterframework.extensions.log4j.IbisLoggerConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogUtil {
	static {
		IbisLoggerConfiguration.init();
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
