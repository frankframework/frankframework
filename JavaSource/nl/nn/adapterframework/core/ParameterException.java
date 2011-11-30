/*
 * $Log: ParameterException.java,v $
 * Revision 1.3  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.2  2011/10/19 14:59:25  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * do not print versions anymore
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2004/10/05 10:03:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
