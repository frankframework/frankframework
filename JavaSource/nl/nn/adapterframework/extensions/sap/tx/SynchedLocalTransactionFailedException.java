/*
 * $Log: SynchedLocalTransactionFailedException.java,v $
 * Revision 1.3  2011-11-30 13:51:53  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2008/01/29 15:49:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
