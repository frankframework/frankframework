package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.core.IbisException;

/**
 * Exception thrown by ConfigurationDigester and configure()-methods, signaling the configuration
 * did not succeed.
 */
public class ConfigurationException extends IbisException {
public ConfigurationException(String msg) {
	super(msg);
}
public ConfigurationException(String msg, Throwable th) {
	super(msg,th);
	}
public ConfigurationException(Throwable e) {
	super(e);
}
}
