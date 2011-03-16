/*
 * $Log: IMessageBrowser.java,v $
 * Revision 1.6  2011-03-16 16:36:59  L190409
 * added getIterator() with time and order parameters
 *
 * Revision 1.5  2009/12/23 17:03:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified MessageBrowsing interface to reenable and improve export of messages
 *
 * Revision 1.4  2009/03/13 14:23:22  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added method GetExpiryDate
 *
 * Revision 1.3  2005/12/28 08:33:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.2  2005/07/19 12:14:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of IMessageBrowsingIterator
 *
 * Revision 1.1  2004/06/16 12:25:52  Johan Verrips <johan.verrips@ibissource.org>
 * Initial version of Queue browsing functionality
 *
 *
 */
package nl.nn.adapterframework.core;

import java.util.Date;


/**
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version Id
 */
public interface IMessageBrowser extends IXAEnabled {
	public static final String version = "$RCSfile: IMessageBrowser.java,v $ $Revision: 1.6 $ $Date: 2011-03-16 16:36:59 $";

	/**
	 * Gets an enumeration of messages. This includes setting up connections, sessions etc.
	 */
	IMessageBrowsingIterator getIterator() throws ListenerException;
	IMessageBrowsingIterator getIterator(Date startTime, Date endTime, boolean forceDescending) throws ListenerException;
	
	/**
	 * Retrieves the message context as an iteratorItem.
	 * The result can be used in the methods above that use an iteratorItem as 
	 */
	IMessageBrowsingIteratorItem getContext(String messageId) throws ListenerException;

	/**
	 * Retrieves the message, but does not delete. 
	 */
	Object browseMessage(String messageId) throws ListenerException;
	/**
	 * Retrieves and deletes the message.
	 */
	Object getMessage(String messageId) throws ListenerException;
	/**
	 * Deletes the message.
	 */
	void   deleteMessage(String messageId) throws ListenerException;

}

