/*
 * $Log: MessageBrowseException.java,v $
 * Revision 1.3  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.2  2011/10/19 14:59:25  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * do not print versions anymore
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2004/06/16 12:25:52  Johan Verrips <johan.verrips@ibissource.org>
 * Initial version of Queue browsing functionality
 *
 *
 */
package nl.nn.adapterframework.core;

/**
 * @version Id
 * @author Johan Verrips
 */
public class MessageBrowseException extends IbisException {

	public MessageBrowseException() {
		super();
	}

	public MessageBrowseException(String arg1) {
		super(arg1);
	}

	public MessageBrowseException(String arg1, Throwable arg2) {
		super(arg1, arg2);
	}

	public MessageBrowseException(Throwable arg1) {
		super(arg1);
	}
}