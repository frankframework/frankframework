/*
 * $Log: SenderException.java,v $
 * Revision 1.4  2004-03-30 07:29:54  L190409
 * updated javadoc
 *
 */
package nl.nn.adapterframework.core;

/**
 * Exception thrown by the ISender (implementation) to notify
 * that the sending did not succeed.
 * 
 * @version Id
 * @author  Gerrit van Brakel
 */
public class SenderException extends IbisException {
	public static final String version="$Id: SenderException.java,v 1.4 2004-03-30 07:29:54 L190409 Exp $";
		
	public SenderException() {
		super();
	}
	public SenderException(String errMsg) {
		super(errMsg);
	}
	public SenderException(String errMsg, Throwable t) {
		super(errMsg, t);
	}
	public SenderException(Throwable t) {
		super(t);
	}
}
