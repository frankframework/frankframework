package nl.nn.adapterframework.core;

/**
 * Exception thrown by implementations of methods of listeners.
 * 
 * <p>$Id: ListenerException.java,v 1.2 2004-02-04 10:01:58 a1909356#db2admin Exp $</p>
 * @author Gerrit van Brakel
 */
public class ListenerException extends IbisException {
		public static final String version="$Id: ListenerException.java,v 1.2 2004-02-04 10:01:58 a1909356#db2admin Exp $";

/**
 * SendException constructor comment.
 */
public ListenerException() {
	super();
}
/**
 * SendException constructor comment.
 * @param arg1 java.lang.String
 */
public ListenerException(String arg1) {
	super(arg1);
}
/**
 * SendException constructor comment.
 * @param arg1 java.lang.String
 * @param arg2 java.lang.Throwable
 */
public ListenerException(String arg1, Throwable arg2) {
	super(arg1, arg2);
}
/**
 * SendException constructor comment.
 * @param arg1 java.lang.Throwable
 */
public ListenerException(Throwable arg1) {
	super(arg1);
}
}
