/*
 * $Log: ListenerException.java,v $
 * Revision 1.4  2004-03-30 07:29:59  L190409
 * updated javadoc
 *
 * Revision 1.3  2004/03/26 10:42:45  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.core;

/**
 * Exception thrown by implementations of methods of listeners.
 * 
 * @version Id
 * @author Gerrit van Brakel
 */
public class ListenerException extends IbisException {
	public static final String version="$Id: ListenerException.java,v 1.4 2004-03-30 07:29:59 L190409 Exp $";

	public ListenerException() {
		super();
	}
	public ListenerException(String errMsg) {
		super(errMsg);
	}
	public ListenerException(String errMsg, Throwable t) {
		super(errMsg, t);
	}
	public ListenerException(Throwable t) {
		super(t);
	}
}
