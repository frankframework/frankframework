/*
 * $Log: TracingException.java,v $
 * Revision 1.1  2006-09-06 16:02:36  europe\L190409
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
