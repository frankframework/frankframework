/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020 WeAreFrank!

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
public interface ITransactionalStorage<S extends Serializable> extends IMessageBrowser<S>, INamedObject {

	public static final int MAXCOMMENTLEN=1000;

	public static final String TYPE_ERRORSTORAGE="E";
	public static final String TYPE_MESSAGESTORAGE="M";
	public static final String TYPE_MESSAGELOG_PIPE="L";
	public static final String TYPE_MESSAGELOG_RECEIVER="A";


	/**
	 * Prepares the object for operation. After this
	 * method is called the storeMessage() and retrieveMessage() methods may be called
	 */ 
	public void open() throws Exception;
	public void close();
	
	public void configure() throws ConfigurationException;

	/**
	 * Store the message, returns new messageId.
	 * 
	 * The messageId should be unique.
	 */
	public String storeMessage(String messageId, String correlationId, Date receivedDate, String comments, String label, S message) throws SenderException;
	
	/**
	 * Check if the storage contains message with the given original messageId 
	 * (as passed to storeMessage).
	 */
	public boolean containsMessageId(String originalMessageId) throws ListenerException;

	public boolean containsCorrelationId(String correlationId) throws ListenerException;

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
	
	public int getMessageCount() throws ListenerException;

}
