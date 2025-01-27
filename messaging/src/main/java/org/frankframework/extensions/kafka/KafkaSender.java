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
package org.frankframework.extensions.kafka;

import java.util.concurrent.Future;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import lombok.AccessLevel;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.stream.Message;

/**
 * Experimental {@link ISender} for sending messages to a Kafka instance.
 * The Kafka integration is still under development so do not
 * currently use unless you wish to participate in this development.
 */
@Deprecated(forRemoval = false)
@ConfigurationWarning("Experimental and under development. Do not use unless you wish to participate in this development.")
@Log4j2
public class KafkaSender extends AbstractKafkaFacade implements ISender {

	//setter is for testing purposes only.
	private @Setter(AccessLevel.PACKAGE) Producer<String, byte[]> producer;

	/** The topic to send messages to. Only one topic per sender. Wildcards are not supported. */
	private @Setter String topic;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(topic))
			throw new ConfigurationException("topic must be specified");
		if (topic.contains(","))
			throw new ConfigurationException("Only one topic is allowed to be used for sender.");
		if (topic.contains("*"))
			throw new ConfigurationException("Wildcards are not allowed to be used for sender.");
	}

	@Override
	public void start() {
		producer = new KafkaProducer<>(properties, new StringSerializer(), new ByteArraySerializer());

		// TODO find a better alternative, perhaps attempting to create (and close) a transaction? Definitely don't use Thread.sleep!
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new LifecycleException(e);
		}

		Double metric = (Double) producer.metrics().values().stream().filter(item -> "response-total".equals(item.metricName().name())).findFirst().orElseThrow(() -> new LifecycleException("Failed to get response-total metric.")).metricValue();
		if (metric.intValue() == 0) throw new LifecycleException("Didn't get a response from Kafka while connecting for Sending.");
	}

	@Override
	public void stop() {
		producer.close();
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException {
		byte[] messageData;
		try {
			message.preserve();
			messageData = message.asByteArray();
		} catch (Exception e) {
			throw new SenderException("Failed to convert message to message type:", e);
		}
		ProducerRecord<String, byte[]> producerRecord = new ProducerRecord<>(topic, messageData);
		Future<RecordMetadata> future = producer.send(producerRecord);
		RecordMetadata metadata;
		try {
			metadata = future.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SenderException(e);
		} catch (Exception e) {
			throw new SenderException(e);
		}
		message.getContext().put("kafka.offset", metadata.offset());
		message.getContext().put("kafka.partition", metadata.partition());
		return new SenderResult(message);
	}

	@Override
	public String getPhysicalDestinationName() {
		return "TOPIC(" + topic + ") on (" + getBootstrapServers() + ")";
	}
}
