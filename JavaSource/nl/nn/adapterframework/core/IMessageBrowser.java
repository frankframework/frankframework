/*
 * $Log: IMessageBrowser.java,v $
 * Revision 1.4  2009-03-13 14:23:22  m168309
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
	public static final String version = "$RCSfile: IMessageBrowser.java,v $ $Revision: 1.4 $ $Date: 2009-03-13 14:23:22 $";

	/**
	 * Gets an enumeration of messages. This includes setting up connections, sessions etc.
	 */
	public IMessageBrowsingIterator getIterator() throws ListenerException;
	
	public String getId(Object iteratorItem) throws ListenerException;
	public String getOriginalId(Object iteratorItem) throws ListenerException;
	public String getCorrelationId(Object iteratorItem) throws ListenerException;
	public Date   getInsertDate(Object iteratorItem) throws ListenerException;
	public Date   getExpiryDate(Object iteratorItem) throws ListenerException;
	public String getCommentString(Object iteratorItem) throws ListenerException;
	
	/**
	 * Retrieves the message, but does not delete. 
	 */
	public Object browseMessage(String messageId) throws ListenerException;
	/**
	 * Retrieves and deletes the message.
	 */
	public Object getMessage(String messageId) throws ListenerException;
	/**
	 * Deletes the message.
	 */
	public void   deleteMessage(String messageId) throws ListenerException;

}

