/**
 * $Log: ListenerException.java,v $
 * Revision 1.3  2004-03-26 10:42:45  NNVZNL01#L180564
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
		public static final String version="$Id: ListenerException.java,v 1.3 2004-03-26 10:42:45 NNVZNL01#L180564 Exp $";

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
