/*
 * $Log: ITransactionalStorage.java,v $
 * Revision 1.5.4.1  2007-09-21 13:23:34  europe\M00035F
 * * Add method to ITransactionalStorage to check if original message ID can be found in it
 * * Check for presence of original message id in ErrorStorage before processing, so it can be removed from queue if it has already once been recorded as unprocessable (but the TX in which it ran could no longer be committed).
 *
 * Revision 1.5  2007/06/12 11:19:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added set/getType
 *
 * Revision 1.4  2007/05/23 09:07:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attributes slotId and active
 *
 * Revision 1.3  2005/07/19 12:20:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
	public static final String version = "$RCSfile: ITransactionalStorage.java,v $ $Revision: 1.5.4.1 $ $Date: 2007-09-21 13:23:34 $";

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
	
    /**
     * Check if the storage contains message with the given original messageId
     * (as passed to storeMessage).
     */
    public boolean containsMessageId(String originalMessageId) throws ListenerException;
    
	public void setName(String name);

	/**
	 *  slotId allows using component to define a kind of 'subsection'.
	 */	
	public String getSlotId();
	public void setSlotId(String string);


	/**
	 *  type is one character: E for error, I for inprocessStorage, L for logging.
	 */	
	public String getType();
	public void setType(String string);
	
	public boolean isActive();

}
