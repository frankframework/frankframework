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

import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import lombok.AccessLevel;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.stream.Message;

import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * This class should NOT be used outside of this kafka package.
 * This class isn't public to prevent generation of javadoc/frankdoc.
 * @param <T> Topic type.
 * @param <M> Message type.
 */
class KafkaInternalSender<T,M> extends KafkaFacade implements ISender {
	//setter is for testing purposes only.
	private @Setter(AccessLevel.PACKAGE) Producer<T, M> producer;
	private @Setter String topic;
	private final String keySerializer;
	private final String valueSerializer;
	private final KafkaType messageType;
	public KafkaInternalSender(KafkaType keyType, KafkaType messageType) {
		super();
		this.keySerializer = getSerializer(keyType);
		this.valueSerializer = getSerializer(messageType);
		this.messageType = messageType;
	}

	private String getSerializer(KafkaType kafkaType) {
		if (kafkaType == KafkaType.BYTEARRAY) return ByteArraySerializer.class.getName();
		if (kafkaType == KafkaType.STRING) return StringSerializer.class.getName();
		throw new IllegalArgumentException("Unknown KafkaType ["+kafkaType+"]");
	}

	private M getMessage(Message message) throws SenderException {
		try {
			if (messageType == KafkaType.BYTEARRAY) return (M) message.asByteArray();
			if (messageType == KafkaType.STRING) return (M) message.asString();
		} catch(Exception e) {
			throw new SenderException("Failed to convert message to message type:", e);
		}
		throw new SenderException("Unknown KafkaType ["+messageType+"]");
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(topic)) throw new ConfigurationException("topic must be specified");
		if(topic.contains(",")) throw new ConfigurationException("Only one topic is allowed to be used for sender.");
		if(topic.contains("*")) throw new ConfigurationException("Wildcards are not allowed to be used for sender.");
		properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
		properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
	}

	@Override
	public void open() throws SenderException {
		producer = new KafkaProducer<>(properties);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			throw new SenderException(e);
		}
		Double metric = (Double) producer.metrics().values().stream().filter(item -> item.metricName().name().equals("response-total")).findFirst().orElseThrow(() -> new SenderException("Failed to get response-total metric.")).metricValue();
		if (metric.intValue() == 0) throw new SenderException("Didn't get a response from Kafka while connecting for Sending.");
	}

	@Override
	public void close() {
		producer.close();
	}

	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException {
		ProducerRecord<T, M> producerRecord;
		M messageData;
		try {
			message.preserve();
			messageData = getMessage(message);
		} catch (Exception e) {
			throw new SenderException("Failed to convert message to message type:", e);
		}
		producerRecord = new ProducerRecord<>(topic, messageData);
		Future<RecordMetadata> future = producer.send(producerRecord);
		RecordMetadata metadata;
		try {
			metadata = future.get();
		} catch (Exception e) {
			throw new SenderException(e);
		}
		message.getContext().put("kafka.offset", metadata.offset());
		message.getContext().put("kafka.partition", metadata.partition());
		return new SenderResult(message);
	}

	@Override
	public String getPhysicalDestinationName() {
		return "TOPIC(" + topic + ") on ("+ getBootstrapServers() +")";
	}
}
