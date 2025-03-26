/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2022-2023 WeAreFrank!

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

import java.util.Date;
import java.util.List;

import org.frankframework.receivers.RawMessageWrapper;


/**
 * @author  Gerrit van Brakel
 * @since   4.3
 */
public interface IMessageBrowser<M> extends IXAEnabled {

	enum SortOrder { NONE, ASC, DESC }

	enum StorageType {
		NONE(null),
		ERRORSTORAGE("E"),
		MESSAGELOG_PIPE("L"),
		MESSAGELOG_RECEIVER("A"),
		MESSAGESTORAGE("M"),
		HOLDSTORAGE("H");

		private final String code;

		StorageType(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}
	}

	enum HideMethod {
		/** to mask the entire string */
		ALL,
		/** to only mask the first half of the string */
		FIRSTHALF
	}

	/**
	 * Gets an enumeration of messages. This includes setting up connections, sessions etc.
	 */
	IMessageBrowsingIterator getIterator() throws ListenerException;
	IMessageBrowsingIterator getIterator(Date startTime, Date endTime, SortOrder order) throws ListenerException;

	/**
	 * Retrieves the message context as an iteratorItem.
	 * The result can be used in the methods above that use an iteratorItem. Use this method as try-with-resources to close the connections.
	 */
	IMessageBrowsingIteratorItem getContext(String storageKey) throws ListenerException;

	/**
	 * Check if the storage contains message with the given original messageId
	 * (as passed to storeMessage).
	 */
	boolean containsMessageId(String originalMessageId) throws ListenerException;
	boolean containsCorrelationId(String correlationId) throws ListenerException;

	/**
	 * Retrieves the message, but does not delete.
	 *
	 */
	RawMessageWrapper<M> browseMessage(String storageKey) throws ListenerException;
	/**
	 * Deletes the message.
	 */
	void deleteMessage(String storageKey) throws ListenerException;
	int getMessageCount() throws ListenerException; // may return -1 when the count cannot be determined

	/**
	 * Regular expression to mask strings in the errorStore/logStore.
	 * Every character between to the strings in this expression will be replaced by a '*'.
	 * <br/>
	 * For example, the regular expression (?&lt;=&lt;party&gt;).*?(?=&lt;/party&gt;) will replace every
	 * character between keys &lt;party&gt; and &lt;/party&gt;
	 * <br/>
	 * When no hideRegex is configured on the errorStore / logStore but is configured on the {@link org.frankframework.receivers.Receiver#setHideRegex(String)},
	 * then the Receiver's hideRegex is used for the errorStore / logStore.
	 */
	void setHideRegex(String hideRegex);
	String getHideRegex();

	/**
	 * (Only used when hideRegex is not empty) Specifies the way to hide
	 * @ff.default ALL
	 */
	void setHideMethod(HideMethod hideMethod);
	HideMethod getHideMethod();

	/**
	 * Retrieves the field names of the storage
	 */
	List<MessageBrowserField> getStorageFields();

}
