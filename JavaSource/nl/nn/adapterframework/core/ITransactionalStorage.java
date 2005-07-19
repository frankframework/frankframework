/*
 * $Log: ITransactionalStorage.java,v $
 * Revision 1.3  2005-07-19 12:20:44  europe\L190409
 * adapted to work extend IMessageBrowser
 *
 * Revision 1.2  2004/03/26 10:42:44  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.1  2004/03/23 16:50:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * initial version
 *
 */
package nl.nn.adapterframework.core;

import java.io.Serializable;
import java.util.Date;

import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 * The <code>ITransactionalStorage</code> is responsible for storing and 
 * retrieving-back messages under transaction control.
 * @see nl.nn.adapterframework.receivers.PullingReceiverBase
 * @author  Gerrit van Brakel
 * @since   4.1
 * @version Id
*/
public interface ITransactionalStorage extends IMessageBrowser, INamedObject {
	public static final String version = "$RCSfile: ITransactionalStorage.java,v $ $Revision: 1.3 $ $Date: 2005-07-19 12:20:44 $";

	/**
	 * Prepares the object for operation. After this
	 * method is called the storeMessage() and retrieveMessage() methods may be called
	 */ 
	public void open() throws SenderException, ListenerException;
	public void close() throws SenderException, ListenerException;
	
	public void configure() throws ConfigurationException;
	
	/**
	 * Store the message, returns new messageId.
	 * 
	 * The messageId should be unique.
	 */
	public String storeMessage(String messageId, String correlationId, Date receivedDate, String comments, Serializable message) throws SenderException;
	
	public void setName(String name);
}
