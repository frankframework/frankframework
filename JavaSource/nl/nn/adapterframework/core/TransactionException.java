/*
 * $Log: TransactionException.java,v $
 * Revision 1.2  2004-03-26 10:42:50  NNVZNL01#L180564
 * added @version tag in javadoc
 *
 * Revision 1.1  2004/03/23 16:54:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * initial version
 *
 */
package nl.nn.adapterframework.core;

/**
 * Wrapper for numerous transaction handling related exceptions.
 * @version Id
 * @author Gerrit van Brakel
 * @since  4.1
 */
public class TransactionException extends IbisException {
	public static final String version="$Id: TransactionException.java,v 1.2 2004-03-26 10:42:50 NNVZNL01#L180564 Exp $";
	
	public TransactionException() {
		super();
	}

	public TransactionException(String arg1) {
		super(arg1);
	}

	public TransactionException(String arg1, Throwable arg2) {
		super(arg1, arg2);
	}

	public TransactionException(Throwable arg1) {
		super(arg1);
	}

}
