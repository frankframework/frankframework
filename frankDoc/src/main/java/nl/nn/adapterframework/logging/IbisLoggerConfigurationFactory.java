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
