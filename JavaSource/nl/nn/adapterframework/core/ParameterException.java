/*
 * $Log: ParameterException.java,v $
 * Revision 1.1  2004-10-05 10:03:58  L190409
 * reorganized parameter code
 *
 * Revision 1.4  2004/03/30 07:29:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
public class ParameterException extends IbisException {
	public static final String version="$Id: ParameterException.java,v 1.1 2004-10-05 10:03:58 L190409 Exp $";
		
	public ParameterException() {
		super();
	}
	public ParameterException(String errMsg) {
		super(errMsg);
	}
	public ParameterException(String errMsg, Throwable t) {
		super(errMsg, t);
	}
	public ParameterException(Throwable t) {
		super(t);
	}
}
