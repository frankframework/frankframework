package nl.nn.adapterframework.core;

/**
 * Exception thrown if a {@link IManagable ManagedObject} like an Adapter or Receiver is in
 * an unexpected or illegal state.
 */
public class ManagedStateException extends IbisException {
		public static final String version="$Id: ManagedStateException.java,v 1.1 2004-02-04 08:36:12 a1909356#db2admin Exp $";

	public ManagedStateException() {
		super();
	}
	public ManagedStateException(String msg) {
		super(msg);
	}
}
