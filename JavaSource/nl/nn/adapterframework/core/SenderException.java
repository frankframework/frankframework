package nl.nn.adapterframework.core;

/**
 * Exception thrown by the ISender (implementation) to notify
 * that the sending did not succeed.
 * @version Id
 *
 */
public class SenderException extends IbisException {
	public static final String version="$Id: SenderException.java,v 1.3 2004-03-26 10:42:44 NNVZNL01#L180564 Exp $";
	
/**
 * SendException constructor comment.
 */
public SenderException() {
	super();
}
/**
 * SendException constructor comment.
 * @param arg1 java.lang.String
 */
public SenderException(String arg1) {
	super(arg1);
}
/**
 * SendException constructor comment.
 * @param arg1 java.lang.String
 * @param arg2 java.lang.Throwable
 */
public SenderException(String arg1, Throwable arg2) {
	super(arg1, arg2);
}
/**
 * SendException constructor comment.
 * @param arg1 java.lang.Throwable
 */
public SenderException(Throwable arg1) {
	super(arg1);
}
}
