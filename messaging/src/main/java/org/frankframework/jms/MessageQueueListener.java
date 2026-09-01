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
package org.frankframework.jms;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.util.Map;

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.ObjectMessage;
import jakarta.jms.StreamMessage;
import jakarta.jms.TextMessage;

import org.jspecify.annotations.NonNull;

import org.frankframework.core.ListenerException;
import org.frankframework.doc.Protected;
import org.frankframework.receivers.MessageWrapper;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;

/**
 * Listener that receives messages from a {@link MessageQueueSender} in another {@link org.frankframework.core.Adapter}.
 * Any {@link org.frankframework.core.PipeLineSession} keys sent by the source adapter will be in the receiving {@code PipeLineSession}.
 * This listener is the JMS-based equivalent of the {@link org.frankframework.jdbc.MessageStoreListener}.
 * <p>
 *     If the message received is not in the expected format produced by the {@link MessageQueueSender}, the listener will still do
 *     its best to convert it to a regular input-message, so it can also be used with a regular {@link JmsSender}, or other JMS sources.
 * </p>
 */
public class MessageQueueListener extends PullingJmsListener {

	@Override
	public Message extractMessage(@NonNull RawMessageWrapper<jakarta.jms.Message> rawMessage, @NonNull Map<String, Object> context) throws ListenerException {
		MessageWrapper<?> messageWrapper;
		try {
			messageWrapper = extractMessageWrapper(rawMessage.getRawMessage());
		} catch (JMSException | IOException | ClassNotFoundException e) {
			throw new ListenerException(e);
		}

		context.putAll(messageWrapper.getContext());

		return messageWrapper.getMessage();
	}

	private MessageWrapper<?> extractMessageWrapper(jakarta.jms. @NonNull Message rawMessage) throws JMSException, IOException, ClassNotFoundException, ListenerException {
		return getMessageWrapper(rawMessage);
	}

	private MessageWrapper<?> getMessageWrapper(jakarta.jms.@NonNull Message rawMessage) throws JMSException, IOException, ClassNotFoundException, ListenerException {
		return switch (rawMessage) {
			case ObjectMessage objectMessage -> objectToMessageWrapper(objectMessage.getObject());
			case BytesMessage bytesMessage -> getMessageWrapperFromBytesMessage(bytesMessage);
			case StreamMessage streamMessage -> objectToMessageWrapper(streamMessage.readObject());
			case TextMessage textMessage -> objectToMessageWrapper(textMessage.getText());
			default -> throw new ListenerException("Unsupported JMS Message type [" + rawMessage.getClass().getName() + "]");
		};
	}

	private MessageWrapper<?> getMessageWrapperFromBytesMessage(BytesMessage bytesMessage) throws IOException, ClassNotFoundException, JMSException {
		BytesMessageInputStream in = new BytesMessageInputStream(bytesMessage);
		try (ObjectInputStream ois = new ObjectInputStream(in)) {
			return objectToMessageWrapper(ois.readObject());
		} catch (StreamCorruptedException e) {
			in.resetMessage();
			return objectToMessageWrapper(Message.asMessage(in));
		}
	}

	private MessageWrapper<?> objectToMessageWrapper(Object object) {
		return switch (object) {
			case MessageWrapper<?> messageWrapper -> messageWrapper;
			case org.frankframework.stream.Message message -> new MessageWrapper<>(message, null, null);
			case null, default -> new MessageWrapper<>(org.frankframework.stream.Message.asMessage(object), null, null);
		};
	}

	@Protected
	@Override
	public void setMessageClass(MessageClass messageClass) {
		super.setMessageClass(messageClass);
	}
}
