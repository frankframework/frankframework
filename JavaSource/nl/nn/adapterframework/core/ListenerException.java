/*
 * $Log: ListenerException.java,v $
 * Revision 1.6  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.2  2011/10/19 14:58:23  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * do not print versions anymore
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2004/03/30 07:29:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.3  2004/03/26 10:42:45  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.core;

/**
 * Exception thrown by implementations of methods of listeners.
 * 
 * @version Id
 * @author Gerrit van Brakel
 */
public class ListenerException extends IbisException {

	public ListenerException() {
		super();
	}
	public ListenerException(String errMsg) {
		super(errMsg);
	}
	public ListenerException(String errMsg, Throwable t) {
		super(errMsg, t);
	}
	public ListenerException(Throwable t) {
		super(t);
	}
}
