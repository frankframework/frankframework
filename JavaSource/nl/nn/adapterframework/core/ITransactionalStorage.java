/*
 * $Log: ITransactionalStorage.java,v $
 * Revision 1.1  2004-03-23 16:50:49  L190409
 * initial version
 *
 */
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 * The <code>ITransactionalStorage</code> is responsible for storing and 
 * retrieving-back messages under transaction control.
 * @see nl.nn.adapterframework.receivers.PullingReceiverBase
 * <p>$Id: ITransactionalStorage.java,v 1.1 2004-03-23 16:50:49 L190409 Exp $</p>
 * @author Gerrit van Brakel
 * @since  4.1
 */
public interface ITransactionalStorage extends INamedObject, IXAEnabled {
	public static final String version="$Id: ITransactionalStorage.java,v 1.1 2004-03-23 16:50:49 L190409 Exp $";

/**
 * Prepares the object for operation. After this
 * method is called the storeMessage() and retrieveMessage() methods may be called
 */ 
public void open() throws SenderException, ListenerException;
public void close() throws SenderException, ListenerException;

public void configure() throws ConfigurationException;

/**
 * Store the message.
 * 
 * The messageId must be unique
 */
public void storeMessage(String messageId, String message) throws SenderException;
/**
 * Delete the message under transaction control
 */
public void deleteMessage(String messageId) throws SenderException,ListenerException;

public void setName(String name);
}
