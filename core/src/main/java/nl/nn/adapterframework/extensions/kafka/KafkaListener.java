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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import lombok.AccessLevel;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.stream.Message;

public class KafkaListener extends KafkaFacade implements IPushingListener<ConsumerRecord<String, byte[]>> {
	private IMessageHandler<ConsumerRecord<String, byte[]>> messageHandler;
	private IbisExceptionListener ibisExceptionListener;
	//setter is for testing purposes only.
	private @Setter(AccessLevel.PACKAGE) Consumer<String, byte[]> consumer;
	/** The group id of the consumer */
	private @Setter String groupId;
	/** Whether to start reading from the beginning of the topic. */
	private @Setter boolean fromBeginning = false;
	/** How often to check for new topics when using Patterns. (in MS) */
	private @Setter int patternRecheckInterval = 5000;
	/** The topics to listen to, separated by `,`. Wildcards are supported with `example.*`. */
	private @Setter String topics;
	/** Kafka internal poll timeout (in MS) */
	private @Setter int pollTimeout = 100;
	private KafkaInternalConsumer poller;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(groupId)) throw new ConfigurationException("groupId must be specified");
		if (StringUtils.isEmpty(topics)) throw new ConfigurationException("topics must be specified");
		if(pollTimeout < 10) throw new ConfigurationException("pollTimeout should be at least 10");
		if (pollTimeout > 1000) log.warn("pollTimeout is set to a high value, this may cause the listener to be slow to stop.");
		if(patternRecheckInterval < 10) throw new ConfigurationException("patternRecheckInterval should be at least 10");
		properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, fromBeginning ? "earliest" : "latest");
		properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.METADATA_MAX_AGE_CONFIG, String.valueOf(patternRecheckInterval));
		consumer = new KafkaConsumer<>(properties);
	}
	private void onMessage(ConsumerRecord<String, byte[]> record) {
		try (PipeLineSession session = new PipeLineSession()) {
			messageHandler.processRawMessage(this, wrapRawMessage(record, session), session, false);
		} catch (Exception e) {
			ibisExceptionListener.exceptionThrown(this, e);
		}
	}

	@Override
	public void open() throws ListenerException {
		Arrays.stream(topics.split(","))
				.filter(StringUtils::isNotEmpty) //if topics is `abc` java will split it into `abc` and ``. The latter is not a valid topic.
				.map(Pattern::compile) //Convert the topic to a regex pattern, to allow for wildcards in topic names.
				.forEach(consumer::subscribe);
		poller = new KafkaInternalConsumer(consumer, pollTimeout, this::onMessage);
	}

	@Override
	public void close() throws ListenerException {
		poller.kill();
	}

	@Override
	public Message extractMessage(
			@Nonnull RawMessageWrapper<ConsumerRecord<String, byte[]>> rawMessage, @Nonnull Map<String, Object> context) throws ListenerException {
		return new Message(rawMessage.getRawMessage().value());
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<ConsumerRecord<String, byte[]>> rawMessage, PipeLineSession pipeLineSession) throws ListenerException {
		// no-op
	}

	@Override
	public void setHandler(IMessageHandler<ConsumerRecord<String, byte[]>> handler) {
		this.messageHandler = handler;
	}

	@Override
	public void setExceptionListener(IbisExceptionListener listener) {
		this.ibisExceptionListener = listener;
	}

	@Override
	public RawMessageWrapper<ConsumerRecord<String, byte[]>> wrapRawMessage(ConsumerRecord<String, byte[]> rawMessage, PipeLineSession session) throws ListenerException {
		Map<String, String> headers=new HashMap<>();
		Arrays.stream(rawMessage.headers().toArray()).forEach(header -> {
			try {
				headers.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
			} catch(Exception e) {
				log.warn("Failed to convert header key [{}] to string. Bytearray value: [{}]", header.key(), header.value(), e);
			}
		});
		Map<String, Object> context = new HashMap<>();
		context.put("kafkaTopic", rawMessage.topic());
		context.put("kafkaKey", rawMessage.key());
		context.put("kafkaPartition", rawMessage.partition());
		context.put("kafkaOffset", rawMessage.offset());
		context.put("kafkaTimestamp", rawMessage.timestamp());
		context.put("kafkaHeaders", headers);
		return new RawMessageWrapper<>(rawMessage, context);
	}

	@Override
	public String getPhysicalDestinationName() {
		return "TOPICS(" + topics + ") on ("+ getBootstrapServers() +")";
	}
}
