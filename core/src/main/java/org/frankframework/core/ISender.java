/*
   Copyright 2013 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.stream.Message;

/**
 * Marks an implementation as responsible for sending a message to some destination.
 *
 * @author  Gerrit van Brakel
 */
@FrankDocGroup(FrankDocGroupValue.SENDER)
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.ENDPOINT)
public interface ISender extends IConfigurable, FrankElement, NameAware {

	/**
	 * This method will be called to start the sender. After this method is called the sendMessage method may be called.
	 * Purpose of this method is to reduce creating connections to databases etc. in the {@link #sendMessage(Message, PipeLineSession) sendMessage()} method.
	 */
	void start();

	/**
	 * Stop/close the sender and deallocate resources.
	 */
	void stop();

	/**
	 * When <code>true</code>, the result of sendMessage is the reply of the request.
	 */
	default boolean isSynchronous() {
		return true;
	}

	/**
	 * Send a message to some destination (as configured in the Sender object). This method may only be called after the <code>configure() </code>
	 * method is called.
	 * <p>
	 * The following table shows the difference between synchronous and a-synchronous senders:
	 * <table border="1">
	 * <tr><th>&nbsp;</th><th>synchronous</th><th>a-synchronous</th></tr>
	 * <tr><td>{@link #isSynchronous()} returns</td><td><code>true</code></td><td><code>false</code></td></tr>
	 * <tr><td>return value of <code>sendMessage()</code> is</td><td>the reply-message</td><td>the messageId of the message sent</td></tr>
	 * <tr><td>the correlationID specified with <code>sendMessage()</code></td><td>may be ignored</td><td>is sent with the message</td></tr>
	 * <tr><td>a {link TimeOutException}</td><td>may be thrown if a timeout occurs waiting for a reply</td><td>should not be expected</td></tr>
	 * </table>
	 * <p>
	 * Multiple objects may try to call this method at the same time, from different threads.
	 * Implementations of this method should therefore be thread-safe, or <code>synchronized</code>.
	 */
	@Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException;

	default @Nonnull Message sendMessageOrThrow(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		SenderResult senderResult = sendMessage(message, session);
		Message result = senderResult.getResult();

		if (!senderResult.isSuccess()) {
			if (StringUtils.isNotEmpty(senderResult.getErrorMessage())) {
				throw new SenderException(senderResult.getErrorMessage());
			}
			throw new SenderException("sender finished processing using undefined error forward");
		}
		return result;
	}

	/**
	 * returns <code>true</code> if the sender or one of its children use the named session variable.
	 * Callers can use this to determine if a message needs to be preserved.
	 */
	default boolean consumesSessionVariable(String sessionKey) {
		return false;
	}
}
