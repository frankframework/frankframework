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
package nl.nn.adapterframework.receivers;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Getter;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;

public class RawMessageWrapper<M> {

	protected @Getter M rawMessage;
	protected @Getter String id;
	protected @Getter String correlationId;
	protected @Getter Map<String,Object> context = new LinkedHashMap<>();

	protected RawMessageWrapper() {
		// For Serialisation
		this(null, null, null);
	}

	/**
	 * Create a new instance with just message data, and no ID or Correlation ID.
	 *
	 * @param rawMessage The raw message data.
	 */
	public RawMessageWrapper(M rawMessage) {
		this(rawMessage, null, null);
	}

	/**
	 * Create new instance with raw message, id and correlation ID.
	 *
	 * @param rawMessage The raw message.
	 * @param id The ID of the message. May be null. If not null, will be copied to the message context with the
	 *           key {@link PipeLineSession#messageIdKey}.
	 * @param correlationId The Correlation ID of the message. May be null. If not null, will be copied to the
	 *                       message context with the key {@link PipeLineSession#correlationIdKey}.
	 */
	public RawMessageWrapper(@Nonnull M rawMessage, @Nullable String id, @Nullable String correlationId) {
		this.rawMessage = rawMessage;
		this.id = id;
		this.correlationId = correlationId;
		updateOrRemoveValue(PipeLineSession.messageIdKey, id);
		updateOrRemoveValue(PipeLineSession.correlationIdKey, correlationId);
	}

	/**
	 * Create a new instance with raw message data and existing context. Message ID and Correlation ID are
	 * taken from this context, if present.
	 * All values from the given context are copied into the message context.
	 *
	 * @param rawMessage The raw message data.
	 * @param context Context for the message. If containing the keys {@link PipeLineSession#messageIdKey} and / or
	 *                {@link PipeLineSession#correlationIdKey}, these will be copied to their respective fields.
	 */
	public RawMessageWrapper(M rawMessage, @Nonnull Map<String, Object> context) {
		this(rawMessage);
		this.context.putAll(context);
		this.id = (String) context.get(PipeLineSession.messageIdKey);
		this.correlationId = (String) context.get(PipeLineSession.correlationIdKey);
	}

	protected void updateOrRemoveValue(String key, String value) {
		if (value != null) {
			this.context.put(key, value);
		} else {
			this.context.remove(key);
		}
	}

	/**
	 * Get message for the raw message data. Does not call listener to extract message.
	 *
	 * @return The {@link Message}.
	 */
	public Message getMessage() {
		Message message = Message.asMessage(rawMessage);
		message.getContext().putAll(this.context);
		return message;
	}
}
