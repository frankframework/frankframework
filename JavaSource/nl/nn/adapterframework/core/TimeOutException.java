package nl.nn.adapterframework.core;

/**
 * Exception thrown to signal that a timeout occurred.
 * @version Id
 */
public class TimeOutException extends IbisException {
	public static final String version="$Id: TimeOutException.java,v 1.3 2004-03-26 10:42:44 NNVZNL01#L180564 Exp $";
	
/**
 * SendException constructor comment.
 */
public TimeOutException() {
	super();
}
/**
 * SendException constructor comment.
 * @param arg1 java.lang.String
 */
public TimeOutException(String arg1) {
	super(arg1);
}
/**
 * SendException constructor comment.
 * @param arg1 java.lang.String
 * @param arg2 java.lang.Throwable
 */
public TimeOutException(String arg1, Throwable arg2) {
	super(arg1, arg2);
}
/**
 * SendException constructor comment.
 * @param arg1 java.lang.Throwable
 */
public TimeOutException(Throwable arg1) {
	super(arg1);
}
}
