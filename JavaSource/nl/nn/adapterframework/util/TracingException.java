/*
 * $Log: TracingException.java,v $
 * Revision 1.3  2011-11-30 13:51:49  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2006/09/06 16:02:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.util;

import nl.nn.adapterframework.core.IbisException;

/**
 * METT tracing related exception.
 * 
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
 */
public class TracingException extends IbisException {

	public TracingException() {
		super();
	}

	public TracingException(String msg) {
		super(msg);
	}

	public TracingException(Throwable t) {
		super(t);
	}

	public TracingException(String msg, Throwable t) {
		super(msg, t);
	}
}
