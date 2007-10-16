/*
 * $Log: IfsaException.java,v $
 * Revision 1.1  2007-10-16 08:15:43  europe\L190409
 * introduced switch class for jms and ejb
 *
 * Revision 1.2  2004/07/05 14:30:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made descender of IbisException
 *
 */
package nl.nn.adapterframework.extensions.ifsa.jms;

import nl.nn.adapterframework.core.IbisException;

/**
 * Exception thrown by Ifsa-related classes.
 *
 * @see nl.nn.adapterframework.core.IbisException
 *
 * @author Gerrit van Brakel
 * @version Id
 */
public class IfsaException extends IbisException {
	public IfsaException() {
		super();
	}
	public IfsaException(String msg) {
		super(msg);
	}
	public IfsaException(String msg, Throwable t) {
		super(msg, t);
	}
	public IfsaException(Throwable t) {
		super(t);
	}
}
