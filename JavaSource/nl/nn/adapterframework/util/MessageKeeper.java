/*
 * $Log: MessageKeeper.java,v $
 * Revision 1.4  2009-08-26 15:37:34  L190409
 * removed threading problem
 *
 */
package nl.nn.adapterframework.util;

import java.util.Date;

/**
 * Keeps a list of <code>MessageKeeperMessage</code>s.
 * <br/>
 * @version Id
 * @author  Johan Verrips IOS
 * @see MessageKeeperMessage
 */
public class MessageKeeper extends SizeLimitedVector {

	public MessageKeeper() {
		super();
	}

	public MessageKeeper(int maxSize) {
		super(maxSize);
	}
	
	public synchronized void add(String message) {
		super.add(new MessageKeeperMessage(message));
	}
	public synchronized void add(String message, Date date) {
		super.add(new MessageKeeperMessage(message, date));
	}
	/**
	 * Get a message by number
	 * @return MessageKeeperMessage the Message
	 * @see MessageKeeperMessage
	 */
	public MessageKeeperMessage getMessage(int i) {
		return (MessageKeeperMessage)super.get(i);
	}
}
