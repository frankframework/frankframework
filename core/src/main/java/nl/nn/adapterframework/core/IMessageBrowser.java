/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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
public interface IMessageBrowser<M> extends IXAEnabled {

	public enum SortOrder { NONE, ASC, DESC }

	public enum StorageType {
		NONE(null),
		ERRORSTORAGE("E"),
		MESSAGELOG_PIPE("L"),
		MESSAGELOG_RECEIVER("A"),
		MESSAGESTORAGE("M"),
		HOLDSTORAGE("H");

		private String code;

		private StorageType(String code) {
			this.code = code;
		}

		public String getCode() {
			return code;
		}
	}

	public enum HideMethod {
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
	public boolean containsMessageId(String originalMessageId) throws ListenerException;
	public boolean containsCorrelationId(String correlationId) throws ListenerException;

	/**
	 * Retrieves the message, but does not delete.
	 */
	public M browseMessage(String storageKey) throws ListenerException;
	/**
	 * Deletes the message.
	 */
	public void deleteMessage(String storageKey) throws ListenerException;
	public int getMessageCount() throws ListenerException; // may return -1 when the count cannot be determined

	/** Regular expression to mask strings in the errorStore/logStore. Every character between to the strings in this expression will be replaced by a '*'. For example, the regular expression (?&lt;=&lt;party&gt;).*?(?=&lt;/party&gt;) will replace every character between keys &lt;party&gt; and &lt;/party&gt; */
	public void setHideRegex(String hideRegex);
	public String getHideRegex();

	/**
	 * (Only used when hideRegex is not empty) Specifies the way to hide
	 * @ff.default ALL
	 */
	public void setHideMethod(HideMethod hideMethod);
	public HideMethod getHideMethod();


}

