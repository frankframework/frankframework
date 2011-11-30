/*
 * $Log: ManagedStateException.java,v $
 * Revision 1.4  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.2  2011/10/19 14:59:25  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * do not print versions anymore
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2004/03/30 07:29:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 */
package nl.nn.adapterframework.core;

/**
 * Exception thrown if a {@link IManagable ManagedObject} like an Adapter or Receiver is in
 * an unexpected or illegal state.
 * 
 * @version Id
 * @author Gerrit van Brakel
 */
public class ManagedStateException extends IbisException {

	public ManagedStateException() {
		super();
	}
	public ManagedStateException(String msg) {
		super(msg);
	}
	public ManagedStateException(String errMsg, Throwable t) {
		super(errMsg, t);
	}
	public ManagedStateException(Throwable t) {
		super(t);
	}
}
