/*
 * $Log: TimeOutException.java,v $
 * Revision 1.4  2004-03-30 07:29:54  L190409
 * updated javadoc
 *
 */
package nl.nn.adapterframework.core;

/**
 * Exception thrown to signal that a timeout occurred.
 * @version Id
 */
public class TimeOutException extends IbisException {
	public static final String version="$Id: TimeOutException.java,v 1.4 2004-03-30 07:29:54 L190409 Exp $";
	
	public TimeOutException() {
		super();
	}
	public TimeOutException(String arg1) {
		super(arg1);
	}
	public TimeOutException(String arg1, Throwable arg2) {
		super(arg1, arg2);
	}
	public TimeOutException(Throwable arg1) {
		super(arg1);
	}
}
