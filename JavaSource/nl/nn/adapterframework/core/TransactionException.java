/*
 * $Log: TransactionException.java,v $
 * Revision 1.5  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.2  2011/10/19 14:59:25  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * do not print versions anymore
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2004/03/30 07:29:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.2  2004/03/26 10:42:50  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.1  2004/03/23 16:54:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * initial version
 *
 */
package nl.nn.adapterframework.core;

/**
 * Wrapper for numerous transaction handling related exceptions.
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since   4.1
 */
public class TransactionException extends IbisException {
	
	public TransactionException() {
		super();
	}
	public TransactionException(String errMsg) {
		super(errMsg);
	}
	public TransactionException(String errMsg, Throwable t) {
		super(errMsg, t);
	}
	public TransactionException(Throwable t) {
		super(t);
	}

}
