/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.management.gateway;

import java.io.InputStream;
import java.io.Serializable;

import org.jspecify.annotations.Nullable;
import org.springframework.integration.support.BaseMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

public class OogaBooga<T> extends BaseMessageBuilder<T, OogaBooga<T>> {

	/**
	 * Private constructor to be invoked from the static factory methods only.
	 */
	private OogaBooga(T payload, @Nullable Message<T> originalMessage) {
		super(payload, originalMessage);
	}


	/**
	 * Ensure messages are safely exchanged between nodes and ensures that they are always Serializable.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T> OogaBooga<T> fromMessage(Message<T> message) {
		Assert.notNull(message, "message must not be null");

		if (message.getPayload() instanceof InputStream in && !(in instanceof Serializable)) {
			return new OogaBooga(new SerializableInputStream(in), message);
		}

		return new OogaBooga<>(message.getPayload(), message);
	}

	/**
	 * Set the authentication header.
	 */
	public OogaBooga<T> setAuthentication(String authentication) {
		setHeader(HazelcastConfig.AUTHENTICATION_HEADER_KEY, authentication);
		return this;
	}

	@Override
	public Message<T> build() {
		return new ImmutableMessage<>(getPayload(), getHeaders());
	}
}
