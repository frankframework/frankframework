package nl.nn.adapterframework.core;

/**
 * Exception thrown by the ISender (implementation) to notify
 * that the sending did not succeed.
 * <p>$Id: SenderException.java,v 1.2 2004-02-04 10:01:57 a1909356#db2admin Exp $</p>
 *
 */
public class SenderException extends IbisException {
	public static final String version="$Id: SenderException.java,v 1.2 2004-02-04 10:01:57 a1909356#db2admin Exp $";
	
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
