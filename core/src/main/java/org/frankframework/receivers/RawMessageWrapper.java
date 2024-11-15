/*
   Copyright 2023 WeAreFrank!

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
package org.frankframework.receivers;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import lombok.Getter;

import org.frankframework.core.PipeLineSession;

public class RawMessageWrapper<M> {

	protected @Getter M rawMessage;
	protected @Getter String id;
	protected @Getter String correlationId;
	protected @Getter Map<String,Object> context = new LinkedHashMap<>();

	protected RawMessageWrapper() {
		// For Serialisation
	}

	/**
	 * Create a new instance with just message data, and no ID or Correlation ID.
	 *
	 * @param rawMessage The raw message data.
	 */
	public RawMessageWrapper(@Nonnull M rawMessage) {
		this(rawMessage, null, null);
	}

	/**
	 * Create new instance with raw message, id and correlation ID.
	 *
	 * @param rawMessage The raw message.
	 * @param id The ID of the message. May be null. If not null, will be copied to the message context with the
	 *           key {@link PipeLineSession#MESSAGE_ID_KEY}.
	 * @param correlationId The Correlation ID of the message. May be null. If not null, will be copied to the
	 *                       message context with the key {@link PipeLineSession#CORRELATION_ID_KEY}.
	 */
	public RawMessageWrapper(@Nonnull M rawMessage, @Nullable String id, @Nullable String correlationId) {
		this.rawMessage = rawMessage;
		this.id = id;
		this.correlationId = correlationId;
		updateOrRemoveValue(PipeLineSession.MESSAGE_ID_KEY, id);
		updateOrRemoveValue(PipeLineSession.CORRELATION_ID_KEY, correlationId);
	}

	/**
	 * Create a new instance with raw message data and existing context. Message ID and Correlation ID are
	 * taken from this context, if present.
	 * All values from the given context are copied into the message context.
	 *
	 * @param rawMessage The raw message data.
	 * @param context Context for the message. If containing the keys {@link PipeLineSession#MESSAGE_ID_KEY} and / or
	 *                {@link PipeLineSession#CORRELATION_ID_KEY}, these will be copied to their respective fields.
	 */
	public RawMessageWrapper(M rawMessage, @Nonnull Map<String, Object> context) {
		this(rawMessage);
		this.context.putAll(context);
		this.id = (String) context.get(PipeLineSession.MESSAGE_ID_KEY);
		this.correlationId = (String) context.get(PipeLineSession.CORRELATION_ID_KEY);
	}

	protected void updateOrRemoveValue(String key, String value) {
		if (value != null) {
			this.context.put(key, value);
		} else {
			this.context.remove(key);
		}
	}

	@Override
	public String toString() {
		return "ID[%s] CID[%s] context[%s] contents[%s]".formatted(id, correlationId, context, rawMessage);
	}
}
