/*
 * $Log: MonitorException.java,v $
 * Revision 1.3  2011-11-30 13:51:43  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2008/07/14 17:21:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version of flexible monitoring
 *
 */
package nl.nn.adapterframework.monitoring;

import nl.nn.adapterframework.core.IbisException;

/**
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class MonitorException extends IbisException {

	public MonitorException() {
		super();
	}
	public MonitorException(String message) {
		super(message);
	}
	public MonitorException(String message, Throwable cause) {
		super(message, cause);
	}
	public MonitorException(Throwable cause) {
		super(cause);
	}

}
