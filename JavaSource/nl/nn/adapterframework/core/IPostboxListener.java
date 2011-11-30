/*
 * $Log: IPostboxListener.java,v $
 * Revision 1.4  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2007/10/03 08:12:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map
 *
 */
package nl.nn.adapterframework.core;

import java.util.Map;

/**
 * The <code>IPostboxListener</code> is responsible for querying a message
 * from a postbox.
 *
 * @author  John Dekker
 * @version Id
  */
public interface IPostboxListener extends IPullingListener {
	/**
	 * Retrieves the first message found from queue or other channel, that matches the 
	 * specified <code>messageSelector</code>.
	 * <p>
	 *
	 * @param messageSelector search criteria for messages. Not that the format of the selector
	 * changes per listener, for example a JMSListener's messageSelector follows the JMS specification.
	 * @param threadContext context in which the method is called 
	 */ 
	Object retrieveRawMessage(String messageSelector, Map threadContext) throws ListenerException, TimeOutException;

}
