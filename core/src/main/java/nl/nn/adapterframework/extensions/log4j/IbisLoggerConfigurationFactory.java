package nl.nn.adapterframework.extensions.log4j;

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.plugins.Plugin;

@Plugin(name = "IbisLoggerConfigurationFactory", category = "ConfigurationFactory")
@Order(10)
public class IbisLoggerConfigurationFactory extends ConfigurationFactory {
	/**
	 * Valid file extensions for XML files.
	 */
	public static final String[] SUFFIXES = new String[] {".xml", "*"};

	@Override
	protected String[] getSupportedTypes() {

		System.out.println("IN HEREE!!3");
		return SUFFIXES;
	}

	@Override
	public Configuration getConfiguration(LoggerContext loggerContext, ConfigurationSource source) {
		System.out.println("IN HEREE!!4");
		return new IbisLoggerConfiguration(loggerContext, source);
	}
}
