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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

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
import nl.nn.adapterframework.util.StringUtil;

public class KafkaListener extends KafkaFacade implements IPullingListener<ConsumerRecord<String, byte[]>> {
	//setter is for testing purposes only.
	private @Setter(AccessLevel.PACKAGE) Consumer<String, byte[]> consumer;
	private Iterator<ConsumerRecord<String, byte[]>> waiting = Collections.emptyIterator();
	/** The group id of the consumer */
	private @Setter String groupId;
	/** Whether to start reading from the beginning of the topic. */
	private @Setter boolean fromBeginning = false;
	/** How often to check for new topics when using Patterns. (in MS) */
	private @Setter int patternRecheckInterval = 5000;
	/** The topics to listen to, separated by `,`. Wildcards are supported with `example.*`. */
	private @Setter String topics;
	/** Warning: This ignores all other messages until the current one has finished. One bad message can take the system down. **/
	private @Setter boolean retry = false;
	private @Getter(AccessLevel.PACKAGE) List<Pattern> topicPatterns = new ArrayList<>();
	private final Duration pollDuration = Duration.ofMillis(1);
	private Map<TopicPartition, OffsetAndMetadata> offsetAndMetadataMap = new HashMap<>();

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(groupId)) throw new ConfigurationException("groupId must be specified");
		if (StringUtils.isEmpty(topics)) throw new ConfigurationException("topics must be specified");
		if(patternRecheckInterval < 10) throw new ConfigurationException("patternRecheckInterval should be at least 10");
		properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, fromBeginning ? "earliest" : "latest");
		properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
		properties.setProperty(ConsumerConfig.METADATA_MAX_AGE_CONFIG, String.valueOf(patternRecheckInterval));
		properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
		topicPatterns = StringUtil.splitToStream(topics)
				.map(Pattern::compile) //Convert the topic to a regex pattern, to allow for wildcards in topic names.
				.collect(Collectors.toList());
		if(topicPatterns.isEmpty()) throw new ConfigurationException("topics must contain at least one valid topic");
	}

	@Override
	public void open() {
		consumer = new KafkaConsumer<>(properties);
		topicPatterns.forEach(consumer::subscribe);
	}

	@Override
	public void close() {
		consumer.close();
	}

	@Override
	public Message extractMessage(
			@Nonnull RawMessageWrapper<ConsumerRecord<String, byte[]>> wrappedMessage, @Nonnull Map<String, Object> context) {
		Map<String, String> headers=new HashMap<>();
		ConsumerRecord<String, byte[]> rawMessage = wrappedMessage.getRawMessage();
		rawMessage.headers().forEach(header->{
			try {
				headers.put(header.key(), new String(header.value(), StandardCharsets.UTF_8));
			} catch(Exception e) {
				log.warn("Failed to convert header key [{}] to string. Bytearray value: [{}]", header.key(), header.value(), e);
			}
		});
		context.put("kafkaTopic", rawMessage.topic());
		context.put("kafkaKey", rawMessage.key());
		context.put("kafkaPartition", rawMessage.partition());
		context.put("kafkaOffset", rawMessage.offset());
		context.put("kafkaTimestamp", rawMessage.timestamp());
		context.put("kafkaHeaders", headers);
		return new Message(rawMessage.value(), new MessageContext(context));
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<ConsumerRecord<String, byte[]>> rawMessage, PipeLineSession pipeLineSession) throws ListenerException {
		ConsumerRecord<String,byte[]> record = rawMessage.getRawMessage();
		offsetAndMetadataMap.put(new TopicPartition(record.topic(), record.partition()), new OffsetAndMetadata(record.offset()));
		consumer.commitSync(offsetAndMetadataMap);
	}

	@Override
	public String getPhysicalDestinationName() {
		return "TOPICS(" + topics + ") on ("+ getBootstrapServers() +")";
	}

	@Nonnull
	@Override
	public Map<String, Object> openThread() {
		return new HashMap<>();
	}

	@Override
	public void closeThread(@Nonnull Map<String, Object> threadContext) {
		// nothing.
	}

	@Override
	public RawMessageWrapper<ConsumerRecord<String, byte[]>> getRawMessage(@Nonnull Map<String, Object> threadContext) {
		if (!waiting.hasNext()) waiting = consumer.poll(pollDuration).iterator();
		if(waiting.hasNext()) return new RawMessageWrapper<>(waiting.next());
		return null;
	}
}
