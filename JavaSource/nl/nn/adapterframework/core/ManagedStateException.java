/*
 * $Log: ManagedStateException.java,v $
 * Revision 1.2  2004-03-30 07:29:54  L190409
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
		public static final String version="$Id: ManagedStateException.java,v 1.2 2004-03-30 07:29:54 L190409 Exp $";

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
