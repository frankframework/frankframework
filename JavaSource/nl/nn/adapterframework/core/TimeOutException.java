package nl.nn.adapterframework.core;

/**
 * Exception thrown to signal that a timeout occurred.
 */
public class TimeOutException extends IbisException {
	public static final String version="$Id: TimeOutException.java,v 1.1 2004-02-04 08:36:10 a1909356#db2admin Exp $";
	
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
