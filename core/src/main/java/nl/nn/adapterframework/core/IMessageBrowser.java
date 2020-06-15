/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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

import nl.nn.adapterframework.doc.IbisDoc;


/**
 * @author  Gerrit van Brakel
 * @since   4.3
 */
public interface IMessageBrowser<M> extends IXAEnabled {

	enum SortOrder { NONE, ASC, DESC };

	/**
	 * Gets an enumeration of messages. This includes setting up connections, sessions etc.
	 */
	IMessageBrowsingIterator getIterator() throws ListenerException;
	IMessageBrowsingIterator getIterator(Date startTime, Date endTime, SortOrder order) throws ListenerException;

	/**
	 * Retrieves the message context as an iteratorItem.
	 * The result can be used in the methods above that use an iteratorItem as 
	 */
	IMessageBrowsingIteratorItem getContext(String messageId) throws ListenerException;

	/**
	 * Check if the storage contains message with the given original messageId 
	 * (as passed to storeMessage).
	 */
	public boolean containsMessageId(String originalMessageId) throws ListenerException;
	public boolean containsCorrelationId(String correlationId) throws ListenerException;

	/**
	 * Retrieves the message, but does not delete. 
	 */
	public M browseMessage(String messageId) throws ListenerException;
	/**
	 * Deletes the message.
	 */
	public void deleteMessage(String messageId) throws ListenerException;
	public int getMessageCount() throws ListenerException;

	@IbisDoc({"Regular expression to mask strings in the errorStore/logStore. Every character between to the strings in this expression will be replaced by a '*'. For example, the regular expression (?&lt;=&lt;party&gt;).*?(?=&lt;/party&gt;) will replace every character between keys<party> and </party> ", ""})
	public void setHideRegex(String hideRegex);
	public String getHideRegex();

	@IbisDoc({"(Only used when hideRegex is not empty) either <code>all</code> or <code>firstHalf</code>. When <code>firstHalf</code> only the first half of the string is masked, otherwise (<code>all</code>) the entire string is masked", "all"})
	public void setHideMethod(String hideMethod);
	public String getHideMethod();


}

