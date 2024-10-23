/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020, 2022-2024 WeAreFrank!

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
package org.frankframework.core;

import java.io.Serializable;
import java.util.Date;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.jdbc.MessageStoreSender;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.receivers.Receiver;

/**
 * Marks an implementation as responsible for storing and retrieving-back messages under transaction control.
 *
 * @see Receiver
 * @author  Gerrit van Brakel
 * @since   4.1
*/
@FrankDocGroup(FrankDocGroupValue.TRANSACTIONAL_STORAGE)
public interface ITransactionalStorage<S extends Serializable> extends IMessageBrowser<S>, INamedObject {

	int MAXCOMMENTLEN = 1000;

	/**
	 * Prepares the object for operation. After this
	 * method is called the storeMessage() and retrieveMessage() methods may be called
	 */
	void start() throws Exception;
	void stop();

	void configure() throws ConfigurationException;

	/**
	 * Store the message, returns storageKey.
	 *
	 * The messageId should be unique.
	 */
	String storeMessage(String messageId, String correlationId, Date receivedDate, String comments, String label, S message) throws SenderException;

	/**
	 * Retrieves and deletes the message.
	 */
	RawMessageWrapper<S> getMessage(String storageKey) throws ListenerException;

	/** Optional identifier for this storage, to be able to share the physical storage between a number of receivers and pipes. */
	void setSlotId(String string);
	String getSlotId();

	/**
	 * Possible values are <code>E</code> (error store), <code>M</code> (message store), <code>L</code> (message log for Pipe) or <code>A</code> (message log for Receiver).<br/>
	 * Receiver will always set type to <code>E</code> for errorStorage and always set type to <code>A</code> for messageLog. SenderPipe will set type to <code>L</code> for messageLog (when type isn't specified).<br/>
	 * See {@link MessageStoreSender} for type <code>M</code>.
	 * @ff.default <code>E</code> for errorStorage on Receiver<br/><code>A</code> for messageLog on Receiver<br/><code>L</code> for messageLog on Pipe
	 */
	void setType(String string);
	String getType();
}
