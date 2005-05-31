/*
 * $Log: PipeLineSession.java,v $
 * Revision 1.7  2005-05-31 09:10:10  europe\L190409
 * cosmetic changes
 *
 * Revision 1.6  2005/03/07 11:06:26  Johan Verrips <johan.verrips@ibissource.org>
 * PipeLineSession became a extension of HashMap
 *
 * Revision 1.5  2005/02/10 07:49:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed clearing of pipelinesession a start of pipeline
 *
 * Revision 1.4  2004/03/30 07:29:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.3  2004/03/23 17:51:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added methods for Transaction control
 *
 */
package nl.nn.adapterframework.core;

import java.util.HashMap;
import java.util.Map;


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
 * 
 * @version Id
 * @author  Johan Verrips IOS
 * @since   version 3.2.2
 */
public class PipeLineSession extends HashMap implements IXAEnabled {
	public static final String version="$RCSfile: PipeLineSession.java,v $ $Revision: 1.7 $ $Date: 2005-05-31 09:10:10 $";

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
	public PipeLineSession(Map t) {
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
	
	protected void set(String message, String messageId) {
		// clear(); Dat moet niet meer!
		put(originalMessageKey, message);
	    put(messageIdKey, messageId);
	}

	
	/**
	 * Indicates the processing of this pipeline is either commited in one transaction, or 
	 * rolled back to the situation prior to starting the pipeline, using XA-transactions.
	 */
	public boolean isTransacted() {
		return transacted;
	}
	public void setTransacted(boolean transacted) {
		this.transacted = transacted;
	}
}
