/*
 * $Log: ConfigurationException.java,v $
 * Revision 1.3  2004-03-30 07:30:05  L190409
 * updated javadoc
 *
 * Revision 1.2  2004/03/26 10:42:51  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.core.IbisException;

/**
 * Exception thrown by ConfigurationDigester and configure()-methods, signaling the configuration
 * did not succeed.
 * 
 * @version Id
 * @author Johan Verrips
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
