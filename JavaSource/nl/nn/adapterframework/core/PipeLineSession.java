/*
 * $Log: PipeLineSession.java,v $
 * Revision 1.3  2004-03-23 17:51:15  L190409
 * added methods for Transaction control
 *
 */
package nl.nn.adapterframework.core;

import java.util.Hashtable;

/**
 * The <code>PipeLineSession</code> is an object similar to
 * a <code>session</code> object in a web-application. It stores
 * data, so that the individual <i>pipes</i> may communicate with
 * one another.
 * <p>The object is cleared each time a new message is processed,
 * and the original message (as it arrived on the <code>PipeLine</code>
 * is stored in the key identified by <code>originalMessageKey</code>.
 * The messageId is stored under the key identified by <code>messageId</code>.
 * </p>
 * <p>$Id: PipeLineSession.java,v 1.3 2004-03-23 17:51:15 L190409 Exp $</p>
 *
 * @author Johan Verrips IOS
 * @since version 3.2.2
 */
public class PipeLineSession extends Hashtable implements IXAEnabled {
	public static final String version="$Id: PipeLineSession.java,v 1.3 2004-03-23 17:51:15 L190409 Exp $";

	public static final String originalMessageKey="originalMessage";
	public static final String messageIdKey="messageId";
	private boolean transacted=false;

public PipeLineSession() {
	super();
}
public PipeLineSession(int initialCapacity) {
	super(initialCapacity);
}
public PipeLineSession(int initialCapacity, float loadFactor) {
	super(initialCapacity, loadFactor);
}
public PipeLineSession(java.util.Map t) {
	super(t);
}
	/**
	 * @return the messageId that was passed to the <code>PipeLine</code>
	 */
	public String getMessageId() {
		return (String) get(messageIdKey);
	}
	/**
	 * @return the message that was passed to the <code>PipeLine</code>
	 */
	public String getOriginalMessage() {
		return (String) get(originalMessageKey);
	}
	/**
	 * This method is exclusively to be called by the <code>PipeLine</code>.
	 * It clears the contents of the session, and stores the message that was
	 * passed to the <code>PipeLine</code> under the key <code>orininalMessageKey</code>
	 * 
	 */
	protected void reset(String message, String messageId) {
		clear();
		put(originalMessageKey, message);
	    put(messageIdKey, messageId);
	}
	
	/**
	 * Indicates the processing of this pipeline is either commited in one transactions, or 
	 * rolled back to the situation prior to starting the pipeline.
	 */
	public boolean isTransacted() {
		return transacted;
	}

	public void setTransacted(boolean transacted) {
		this.transacted = transacted;
	}
}
