/*
 * $Log: MonitorException.java,v $
 * Revision 1.1  2008-07-14 17:21:18  europe\L190409
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
