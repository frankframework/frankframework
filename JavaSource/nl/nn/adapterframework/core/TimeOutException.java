/*
 * $Log: TimeOutException.java,v $
 * Revision 1.6  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.2  2011/10/19 14:59:25  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * do not print versions anymore
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2004/03/30 07:29:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 */
package nl.nn.adapterframework.core;

/**
 * Exception thrown to signal that a timeout occurred.
 * @version Id
 */
public class TimeOutException extends IbisException {
	
	public TimeOutException() {
		super();
	}
	public TimeOutException(String arg1) {
		super(arg1);
	}
	public TimeOutException(String arg1, Throwable arg2) {
		super(arg1, arg2);
	}
	public TimeOutException(Throwable arg1) {
		super(arg1);
	}
}
