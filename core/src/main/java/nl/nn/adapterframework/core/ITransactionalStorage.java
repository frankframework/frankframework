/*
   Copyright 2013, 2016 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.core;

import java.io.Serializable;
import java.util.Date;

import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 * The <code>ITransactionalStorage</code> is responsible for storing and 
 * retrieving-back messages under transaction control.
 * @see nl.nn.adapterframework.receivers.ReceiverBase
 * @author  Gerrit van Brakel
 * @since   4.1
*/
public interface ITransactionalStorage extends IMessageBrowser, INamedObject {

	/**
	 * Prepares the object for operation. After this
	 * method is called the storeMessage() and retrieveMessage() methods may be called
	 * @throws Exception an Exception
	 */ 
	public void open() throws Exception;
	public void close();
	
	public void configure() throws ConfigurationException;

	/**
	 * Store the message, returns new messageId.
	 * 
	 * The messageId should be unique.
	 * @param messageId the id of the message
	 * @param correlationId the id the message is correlated with
	 * @param receivedDate the date the message is received
	 * @param comments comments concerning the message
	 * @param label the label of the message
	 * @param message the message to be stored
	 * @return a new messageId
	 * @throws SenderException thrown when storing the message fails
	 */
	public String storeMessage(String messageId, String correlationId, Date receivedDate, String comments, String label, Serializable message) throws SenderException;
	
    /**
     * Check if the storage contains message with the given original messageId
     * (as passed to storeMessage).
	 * @param originalMessageId the original id of the message
	 * @return whether the storage still contains the message
	 * @throws ListenerException thrown when listening to the message fails
     */
    public boolean containsMessageId(String originalMessageId) throws ListenerException;

    public boolean containsCorrelationId(String correlationId) throws ListenerException;


	/**
	 *  slotId allows using component to define a kind of 'subsection'.
	 *  @return the slotId
	 */	
	public String getSlotId();
	public void setSlotId(String string);


	/**
	 *  type is one character: E for error, I for inprocessStorage, L for logging.
	 *  @return the type
	 */	
	public String getType();
	public void setType(String string);
	
	public boolean isActive();
	
	public int getMessageCount() throws ListenerException;

}
