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
package nl.nn.adapterframework.receivers;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.util.Map;

import lombok.Getter;
import nl.nn.adapterframework.core.IMessageWrapper;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;

/**
 * Wrapper for messages that are not serializable.
 *
 * @author  Gerrit van Brakel
 * @since   4.3
 */
@SuppressWarnings({"deprecation", "unchecked"})
public class MessageWrapper<M> extends RawMessageWrapper<M> implements Serializable, IMessageWrapper {

	private  static final long serialVersionUID = -8251009650246241025L;

	private @Getter Message message;

	public MessageWrapper() {
		super();
	}

	public MessageWrapper(Message message, String messageId) {
		this(message, messageId, null);
	}

	public MessageWrapper(Message message, String messageId, String correlationId) {
		// Ugly cast, but I don't think it is safe to leave it NULL
		super((M)message.asObject(), messageId, correlationId);
		this.message = message;
	}

	public MessageWrapper(RawMessageWrapper<M> rawMessageWrapper, Message message) throws ListenerException {
		super(rawMessageWrapper.rawMessage, rawMessageWrapper.id, rawMessageWrapper.correlationId, rawMessageWrapper.context);
		this.message = message;
		this.context.remove("originalRawMessage"); //PushingIfsaProviderListener.THREAD_CONTEXT_ORIGINAL_RAW_MESSAGE_KEY
	}

	// TODO: Sort out if we need this extra constructor with correlationId (we probably do, but only once)
	public MessageWrapper(RawMessageWrapper<M> messageWrapper, Message message, String correlationId) {
		super(messageWrapper.getRawMessage(), messageWrapper.getId(), correlationId, messageWrapper.getContext());
		this.message = message;
		context.remove("originalRawMessage"); //PushingIfsaProviderListener.THREAD_CONTEXT_ORIGINAL_RAW_MESSAGE_KEY)
	}

	@Deprecated
	public void setMessage(Message message) {
		this.message = message;
	}

	/*
	 * this method is used by Serializable, to serialize objects to a stream.
	 */
	private void writeObject(ObjectOutputStream stream) throws IOException {
		message.preserve();
		if (message.isBinary()) {
			if (!(message.asObject() instanceof byte[])) {
				message = new Message(message.asByteArray(), message.getContext());
			}
		} else {
			if (!(message.asObject() instanceof String)) {
				message = new Message(message.asString(), message.getContext());
			}
		}
		stream.writeObject(context);
		stream.writeObject(id);
		stream.writeObject(message);
		stream.writeObject(correlationId);
	}

	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		context = (Map<String, Object>) stream.readObject();
		id = (String) stream.readObject();
		message = (Message) stream.readObject();
		rawMessage = (M) message.asObject();
		try {
			correlationId = (String) stream.readObject();
		} catch (OptionalDataException | EOFException e) {
			// Correlation ID was not written
			correlationId = null;
		}

		// Synchronise ID / CID fields with context map.
		if (id == null) {
			id = (String) context.get(PipeLineSession.messageIdKey);
		}
		if (correlationId == null) {
			correlationId = (String) context.get(PipeLineSession.correlationIdKey);
		}
		this.context.put(PipeLineSession.messageIdKey, id);
		this.context.put(PipeLineSession.correlationIdKey, correlationId);
	}
}
