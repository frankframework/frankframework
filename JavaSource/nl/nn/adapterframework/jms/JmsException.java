/*
 * $Log: JmsException.java,v $
 * Revision 1.1  2005-10-20 15:40:41  europe\L190409
 * added JmsException
 *
 */
package nl.nn.adapterframework.jms;

import nl.nn.adapterframework.core.IbisException;

/**
 * JMS related exception.
 * 
 * @author  Gerrit van Brakel
 * @since   4.4
 * @version Id
 */
public class JmsException extends IbisException {
	public JmsException() {
		super();
	}
	public JmsException(String msg) {
		super(msg);
	}
	public JmsException(String msg, Throwable t) {
		super(msg, t);
	}
	public JmsException(Throwable t) {
		super(t);
	}
}
