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
package org.frankframework.receivers;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;

/**
 * Wrapper for messages that are not serializable.
 *
 * @author  Gerrit van Brakel
 * @since   4.3
 */
@SuppressWarnings({"deprecation", "unchecked"})
@Log4j2
public class MessageWrapper<M> extends RawMessageWrapper<M> implements Serializable {

	@Serial private static final long serialVersionUID = -8251009650246241025L;

	private @Getter Message message;

	public MessageWrapper() {
		// For Serialisation
		super();
	}

	public MessageWrapper(@Nonnull Message message, @Nullable String messageId, @Nullable String correlationId) {
		// Ugly cast, but I don't think it is safe to leave it NULL
		super((M)message.asObject(), messageId, correlationId);
		this.message = message;
	}

	public MessageWrapper(@Nonnull RawMessageWrapper<M> rawMessageWrapper, @Nonnull Message message) {
		this(rawMessageWrapper, message, rawMessageWrapper.id, rawMessageWrapper.correlationId);
	}

	public MessageWrapper(@Nonnull RawMessageWrapper<M> rawMessageWrapper, @Nonnull Message message, @Nullable String messageId, @Nullable String correlationId) {
		super(rawMessageWrapper.getRawMessage(), messageId, correlationId);
		this.message = message;
		this.context.putAll(rawMessageWrapper.getContext());
		this.context.remove(PipeLineSession.ORIGINAL_MESSAGE_KEY);
	}

	/*
	 * this method is used by Serializable, to serialize objects to a stream.
	 */
	@Serial
	private void writeObject(ObjectOutputStream stream) throws IOException {
		Map<String, Serializable> serializableData = context.entrySet().stream()
				.filter(e -> {
					if (e.getValue() instanceof Serializable) return true;
					else {
						log.warn("Cannot write non-serializable MessageWrapper context entry to stream: [{}] -> [{}]", e::getKey, e::getValue);
						return false;
					}
				})
				.collect(Collectors.toMap(Map.Entry::getKey, e -> (Serializable)(e.getValue())));
		stream.writeObject(serializableData);
		stream.writeObject(id);
		stream.writeObject(message);
		stream.writeObject(correlationId);
	}

	@Serial
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		context = (Map<String, Object>) stream.readObject();
		id = (String) stream.readObject();
		message = (Message) stream.readObject();
		rawMessage = (M) message.asObject();
		try {
			correlationId = (String) stream.readObject();
		} catch (OptionalDataException | EOFException e) {
			// Correlation ID was not written in original serialised message
			correlationId = null;
		}

		// Synchronise ID / CID fields with context map.
		if (id == null) {
			id = (String) context.get(PipeLineSession.MESSAGE_ID_KEY);
		}
		if (correlationId == null) {
			correlationId = (String) context.get(PipeLineSession.CORRELATION_ID_KEY);
		}
		updateOrRemoveValue(PipeLineSession.MESSAGE_ID_KEY, id);
		updateOrRemoveValue(PipeLineSession.CORRELATION_ID_KEY, correlationId);
	}
}
