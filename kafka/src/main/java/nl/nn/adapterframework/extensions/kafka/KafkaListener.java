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
package nl.nn.adapterframework.extensions.kafka;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;

public class KafkaListener extends KafkaFacade implements IPullingListener<ConsumerRecord> {

	/** The group id of the consumer */
	private @Setter String groupId;
	/** Whether to start reading from the beginning of the topic. */
	private @Setter boolean fromBeginning = false;
	/** How often to check for new topics when using Patterns. (in MS) */
	private @Setter int patternRecheckInterval = 5000;
	/** The topics to listen to, separated by `,`. Wildcards are supported with `example.*`. */
	private @Setter String topics;
	/** The type of the key. **/
	private @Setter KafkaType keyType = KafkaType.STRING;
	/** The type of the message. **/
	private @Setter KafkaType messageType = KafkaType.BYTEARRAY;
	private @Getter(AccessLevel.PACKAGE) KafkaInternalListener internalListener;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		internalListener = generateInternalListener(keyType, messageType, properties);
		internalListener.setBootstrapServers(getBootstrapServers());
		internalListener.setClientId(getClientId());
		internalListener.setGroupId(groupId);
		internalListener.setFromBeginning(fromBeginning);
		internalListener.setPatternRecheckInterval(patternRecheckInterval);
		internalListener.setTopics(topics);
		internalListener.configure();
	}

	@Override
	public void open() throws ListenerException {
		internalListener.open();
	}

	@Override
	public void close() throws ListenerException {
		internalListener.close();
	}

	@Override
	public Message extractMessage(
			@Nonnull RawMessageWrapper<ConsumerRecord> rawMessage, @Nonnull Map<String, Object> context) throws ListenerException {
		return internalListener.extractMessage(rawMessage, context);
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<ConsumerRecord> rawMessage, PipeLineSession pipeLineSession) throws ListenerException {
		internalListener.afterMessageProcessed(processResult, rawMessage, pipeLineSession);
	}

	public static <M> BiFunction<M, MessageContext, Message> messageConverterFactory(KafkaType kafkaType) {
		if(kafkaType == KafkaType.STRING) return (M message, MessageContext messageContext) -> new Message((String) message, messageContext);
		if(kafkaType == KafkaType.BYTEARRAY) return (M message, MessageContext messageContext) -> new Message((byte[]) message, messageContext);
		throw new IllegalArgumentException("Unknown KafkaType ["+kafkaType+"]");
	}

	public static KafkaInternalListener generateInternalListener(KafkaType keyType, KafkaType messageType, Properties properties) {
		if(keyType == KafkaType.STRING && messageType == KafkaType.STRING) return new KafkaInternalListener<String, String>(properties, messageConverterFactory(messageType));
		if(keyType == KafkaType.STRING && messageType == KafkaType.BYTEARRAY) return new KafkaInternalListener<String, byte[]>(properties, messageConverterFactory(messageType));
		if(keyType == KafkaType.BYTEARRAY && messageType == KafkaType.STRING) return new KafkaInternalListener<byte[], String>(properties, messageConverterFactory(messageType));
		if(keyType == KafkaType.BYTEARRAY && messageType == KafkaType.BYTEARRAY) return new KafkaInternalListener<byte[], byte[]>(properties, messageConverterFactory(messageType));
		throw new IllegalArgumentException("Unknown KafkaType combination ["+keyType+"-"+messageType+"]");
	}

	@Override
	public String getPhysicalDestinationName() {
		return internalListener.getPhysicalDestinationName();
	}

	@Nonnull
	@Override
	public Map<String, Object> openThread() throws ListenerException {
		return internalListener.openThread();
	}

	@Override
	public void closeThread(@Nonnull Map<String, Object> threadContext) throws ListenerException {
		internalListener.closeThread(threadContext);
	}

	@Override
	public RawMessageWrapper<ConsumerRecord> getRawMessage(@Nonnull Map<String, Object> threadContext) throws ListenerException {
		return internalListener.getRawMessage(threadContext);
	}
}
