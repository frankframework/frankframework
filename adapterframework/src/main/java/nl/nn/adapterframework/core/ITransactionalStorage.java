/*
   Copyright 2013 Nationale-Nederlanden

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
 * @see nl.nn.adapterframework.receivers.PullingReceiverBase
 * @author  Gerrit van Brakel
 * @since   4.1
 * @version $Id$
*/
public interface ITransactionalStorage extends IMessageBrowser, INamedObject {
	public static final String version = "$RCSfile: ITransactionalStorage.java,v $ $Revision: 1.12 $ $Date: 2011-11-30 13:51:55 $";

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
	public String storeMessage(String messageId, String correlationId, Date receivedDate, String comments, String label, Serializable message) throws SenderException;
	
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
	
	public int getMessageCount() throws ListenerException;

}
