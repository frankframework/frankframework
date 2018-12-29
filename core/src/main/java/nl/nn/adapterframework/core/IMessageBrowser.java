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

import java.util.Date;


/**
 * @author  Gerrit van Brakel
 * @since   4.3
 */
public interface IMessageBrowser extends IXAEnabled {

	/**
	 * Gets an enumeration of messages. This includes setting up connections, sessions etc.
	 * @return enumeration of messages
	 * @throws ListenerException thrown when listening to the message fails
	 */
	IMessageBrowsingIterator getIterator() throws ListenerException;
	IMessageBrowsingIterator getIterator(Date startTime, Date endTime, boolean forceDescending) throws ListenerException;
	
	/**
	 * Retrieves the message context as an iteratorItem.
	 * The result can be used in the methods above that use an iteratorItem as
	 * @param messageId the message from which the context is retrieved
	 * @return the context of the message
	 * @throws ListenerException thrown when listening to the message fails
	 */
	IMessageBrowsingIteratorItem getContext(String messageId) throws ListenerException;

	/**
	 * Retrieves the message, but does not delete.
	 * @param messageId the message to retrieve the message from
	 * @return the message
	 * @throws ListenerException thrown when listening to the message fails
	 */
	Object browseMessage(String messageId) throws ListenerException;
	/**
	 * Retrieves and deletes the message.
	 * @param messageId the message to retrieve the message from
	 * @return the message
	 * @throws ListenerException thrown when listening to the message fails
	 */
	Object getMessage(String messageId) throws ListenerException;
	/**
	 * Deletes the message.
	 * @param messageId the message to delete
	 * @throws ListenerException thrown when listening to the message fails
	 */
	void   deleteMessage(String messageId) throws ListenerException;

}

