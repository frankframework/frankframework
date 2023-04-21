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
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageWrapper;
import nl.nn.adapterframework.core.ListenerException;
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
	// TODO: Move up to RawMessageWrapper
	private @Getter String correlationId;

	public MessageWrapper() {
		super();
	}

	public MessageWrapper(Message message, String messageId) {
		this(message, messageId, null);
	}

	public MessageWrapper(Message message, String messageId, String correlationId) {
		// Ugly cast, but I don't think it is safe to leave it NULL
		super((M)message.asObject(), messageId);
		this.message = message;
		this.correlationId = correlationId;
	}

	public MessageWrapper(RawMessageWrapper<M> rawMessageWrapper, IListener<M> listener) throws ListenerException {
		super(rawMessageWrapper.getRawMessage(), rawMessageWrapper.getId(), rawMessageWrapper.getContext());
		message = listener.extractMessage(rawMessageWrapper, getContext());
		context.remove("originalRawMessage"); //PushingIfsaProviderListener.THREAD_CONTEXT_ORIGINAL_RAW_MESSAGE_KEY
		if (id == null) {
			if (context.containsKey("mid")) {
				id = (String) context.get("mid");
			} else {
				id = listener.getIdFromRawMessage(rawMessage, context);
			}
		}
		correlationId = (String) context.get("cid");
	}

	public MessageWrapper(RawMessageWrapper<M> messageWrapper, Message message, String correlationId) {
		super(messageWrapper.getRawMessage(), messageWrapper.getId(), messageWrapper.getContext());
		this.message = message;
		context.remove("originalRawMessage"); //PushingIfsaProviderListener.THREAD_CONTEXT_ORIGINAL_RAW_MESSAGE_KEY);
		this.correlationId = correlationId;
	}

	@Deprecated
	public void setMessage(Message message) {
		this.message = message;
	}

	@Deprecated
	void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
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
	}
}
