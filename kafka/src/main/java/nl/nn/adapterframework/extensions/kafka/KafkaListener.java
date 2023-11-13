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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

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

/**
 * This class should NOT be used outside of this kafka package.
 * This class isn't public to prevent generation of javadoc/frankdoc.
 */
public class KafkaListener extends KafkaFacade implements IPullingListener<ConsumerRecord<?, ?>> {

	static Predicate<String> patternPredicate = Pattern.compile("^[a-zA-Z0-9._\\-*]*$").asPredicate();

	/** The group id of the consumer */
	private @Setter String groupId;

	/** Whether to start reading from the beginning of the topic. */
	private @Setter boolean fromBeginning = false;

	/** How often to check for new topics when using Patterns. (in MS) */
	private @Setter int patternRecheckInterval = 5000;

	/** The topics to listen to, separated by `,`. Wildcards are supported with `example.*`. */
	private @Setter String topics;

	/** The type of the key. Used for deserializing.
	 * @ff.default STRING **/
	private @Setter KafkaType keyType = KafkaType.STRING;

	/** The type of the message. Used for deserializing.
	 * @ff.default BYTEARRAY **/
	private @Setter KafkaType messageType = KafkaType.BYTEARRAY;

	//setter is for testing purposes only.
	private @Setter(AccessLevel.PACKAGE) Consumer<?, ?> consumer;
	private @Setter(AccessLevel.PACKAGE) Function<Properties, Consumer<?, ?>> consumerGenerator = KafkaConsumer::new;
	private Iterator<? extends ConsumerRecord<?, ?>> waiting;
	private final Duration pollDuration = Duration.ofMillis(1);
	private final Map<TopicPartition, OffsetAndMetadata> offsetAndMetadataMap = new HashMap<>();
	private BiFunction<Object, MessageContext, Message> converter;
	private @Getter(AccessLevel.PACKAGE) Pattern topicPattern;
	private final Lock lock = new ReentrantLock();

	private static String getDeserializer(KafkaType type) throws ConfigurationException {
		if(type==KafkaType.STRING) return StringDeserializer.class.getName();
		if(type==KafkaType.BYTEARRAY) return ByteArrayDeserializer.class.getName();
		throw new ConfigurationException("Unknown KafkaType ["+type+"]");
	}

	public static <M> BiFunction<M, MessageContext, Message> messageConverterFactory(KafkaType kafkaType) throws ConfigurationException {
		if(kafkaType == KafkaType.STRING) return (M message, MessageContext messageContext) -> new Message((String) message, messageContext);
		if(kafkaType == KafkaType.BYTEARRAY) return (M message, MessageContext messageContext) -> new Message((byte[]) message, messageContext);
		throw new ConfigurationException("Unknown KafkaType ["+kafkaType+"]");
	}
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		this.converter = messageConverterFactory(messageType);
		String keyDeserializer = getDeserializer(keyType);
		String valueDeserializer = getDeserializer(messageType);
		if (StringUtils.isEmpty(groupId)) throw new ConfigurationException("groupId must be specified");
		if (StringUtils.isEmpty(topics)) throw new ConfigurationException("topics must be specified");
		if(patternRecheckInterval < 10) throw new ConfigurationException("patternRecheckInterval should be at least 10");
		properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
		properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, fromBeginning ? "earliest" : "latest");
		properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
		properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
		properties.setProperty(ConsumerConfig.METADATA_MAX_AGE_CONFIG, String.valueOf(patternRecheckInterval));
		properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
		List<String> topicList = StringUtil.split(topics);
		for(String topic: topicList) {
			if(!patternPredicate.test(topic)) throw new ConfigurationException("topics contains invalid characters. Only a-zA-Z0-9._-* are allowed. (topic: ["+topic+"])");
		}
		String pattern = String.join("|", topicList);
		if(pattern.isEmpty()) throw new ConfigurationException("topics must contain at least one valid topic");
		topicPattern = Pattern.compile(pattern);
	}

	@Override
	public void open() throws ListenerException {
		lock.lock();
		try {
			consumer = consumerGenerator.apply(properties);
			consumer.subscribe(topicPattern);
			waiting = consumer.poll(Duration.ofMillis(100)).iterator();
			if (waiting.hasNext()) return;
			Double metric = (Double) consumer.metrics().values().stream().filter(item -> item.metricName().name().equals("response-total")).findFirst().orElseThrow(() -> new ListenerException("Failed to get response-total metric.")).metricValue();
			if (metric.intValue() == 0) throw new ListenerException("Didn't get a response from Kafka while connecting for Listening.");
		} catch(RuntimeException e) {
			throw new ListenerException(e);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void close() {
		lock.lock();
		try {
			consumer.close();
		} finally {
			lock.unlock();
		}

	}

	@Override
	public Message extractMessage(
			@Nonnull RawMessageWrapper<ConsumerRecord<?, ?>> wrappedMessage, @Nonnull Map<String, Object> context) {
		Map<String, String> headers=new HashMap<>();
		ConsumerRecord<?, ?> rawMessage = wrappedMessage.getRawMessage();
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
		return converter.apply(rawMessage.value(), new MessageContext(context));
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<ConsumerRecord<?, ?>> rawMessage, PipeLineSession pipeLineSession) {
		//nothing.
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
	public RawMessageWrapper<ConsumerRecord<?, ?>> getRawMessage(@Nonnull Map<String, Object> threadContext) {
		lock.lock();
		try {
			if (!waiting.hasNext()) waiting = consumer.poll(pollDuration).iterator();
			if (!waiting.hasNext()) return null;
			ConsumerRecord<?, ?> next = waiting.next();
			offsetAndMetadataMap.put(new TopicPartition(next.topic(), next.partition()), new OffsetAndMetadata(next.offset() + 1));
			consumer.commitAsync(offsetAndMetadataMap, (Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception) -> {
				if (exception != null) {
					log.error("Failed to commit offsets", exception);
				}
			});
			return new RawMessageWrapper<>(next);
		} finally {
			lock.unlock();
		}
	}
}
