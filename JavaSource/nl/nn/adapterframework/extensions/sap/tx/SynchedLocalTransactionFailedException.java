/*
 * $Log: SynchedLocalTransactionFailedException.java,v $
 * Revision 1.1  2008-01-29 15:49:02  europe\L190409
 * first version
 *
 */

package nl.nn.adapterframework.extensions.sap.tx;

import nl.nn.adapterframework.extensions.sap.SapException;

/**
 * Exception thrown when a synchronized local transaction failed to complete
 * (after the main transaction has already completed).
 *
 * @author Gerrit van Brakel
 * @since  4.8
 * @see    ConnectionFactoryUtils
 */
public class SynchedLocalTransactionFailedException extends RuntimeException {

	/**
	 * Create a new SynchedLocalTransactionFailedException.
	 * @param msg the detail message
	 * @param cause the root cause
	 */
	public SynchedLocalTransactionFailedException(String msg, SapException cause) {
		super(msg, cause);
	}

}
