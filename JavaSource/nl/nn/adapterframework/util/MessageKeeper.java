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
	public static final String version="$Id: MessageKeeper.java,v 1.3 2004-03-26 10:42:41 NNVZNL01#L180564 Exp $";
	

/**
 * MessageKeeper constructor comment.
 */
public MessageKeeper() {
	super();
}
/**
 * MessageKeeper constructor comment.
 * @param maxSize int
 */
public MessageKeeper(int maxSize) {
	super(maxSize);
}
	public void add(String message){
		super.add(new MessageKeeperMessage(message));
	}
	public void add(String message, Date date) {
		super.add(new MessageKeeperMessage(message,date));
	}
    /**
     * Get a message by numer
     * @return MessageKeeperMessage the Message
     * @see MessageKeeperMessage
     */
	public MessageKeeperMessage getMessage(int i){
		return (MessageKeeperMessage) super.get(i);
	}
	/**
	 * for testing purposes a main method....
	 */
	public static void main (String arg[]) {
		MessageKeeper ms=new MessageKeeper(5);
		ms.add("just a message");
		ms.add("test 2");
		ms.add("met date ", new java.util.Date());
		ms.add("test 4");
		ms.add("test 5");
		ms.add("test 6");
		System.out.println(ms.toString());
		System.out.println(ms.getMessage(2).getMessageText());
		
	}
}
