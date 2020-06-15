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
import nl.nn.adapterframework.doc.IbisDoc;

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
	 * Retrieves and deletes the message.
	 */
	public S getMessage(String messageId) throws ListenerException;


	/**
	 *  slotId allows using component to define a kind of 'subsection'.
	 */	
	@IbisDoc({"Optional identifier for this storage, to be able to share the physical storage between a number of receivers and pipes", ""})
	public String getSlotId();
	public void setSlotId(String string);


	@IbisDoc({"Possible values are E (error store), M (message store), L (message log for pipe) or A (message log for receiver). ReceiverBase will always set type to E for errorStorage and always set type to A for messageLog. GenericMessageSendingPipe will set type to L for messageLog (when type isn't specified). See {@link MessagestoreSender} for type M", "E for errorstorage on receiver, A for messageLog on receiver and L for messageLog on Pipe"})
	public void setType(String string);
	public String getType();
	
	public boolean isActive();
	

}
