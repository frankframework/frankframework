/*
 * $Log: MessageBrowseException.java,v $
 * Revision 1.1  2004-06-16 12:25:52  NNVZNL01#L180564
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

	public static final String version = "$Id: MessageBrowseException.java,v 1.1 2004-06-16 12:25:52 NNVZNL01#L180564 Exp $";
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