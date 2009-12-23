/*
 * $Log: IMessageBrowsingIteratorItem.java,v $
 * Revision 1.1  2009-12-23 17:05:16  L190409
 * modified MessageBrowsing interface to reenable and improve export of messages
 *
 */
package nl.nn.adapterframework.core;

import java.util.Date;

/**
 * Iterator item for messagebrowsers. 
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public interface IMessageBrowsingIteratorItem {

	String getId() throws ListenerException;
	String getOriginalId() throws ListenerException;
	String getCorrelationId() throws ListenerException;
	Date   getInsertDate() throws ListenerException;
	Date   getExpiryDate() throws ListenerException;
	String getType() throws ListenerException;
	String getHost() throws ListenerException;
	String getCommentString() throws ListenerException;
	
	/*
	 * release must be called, in a finally clause, after the item is not used anymore, 
	 * to allow to free resources.
	 */
	void release();

}
