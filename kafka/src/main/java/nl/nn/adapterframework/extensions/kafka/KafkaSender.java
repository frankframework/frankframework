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

import java.io.IOException;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import lombok.AccessLevel;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;

public class KafkaSender extends KafkaFacade implements ISender {
	//setter is for testing purposes only.
	private @Setter(AccessLevel.PACKAGE) Producer<String, byte[]> producer;
	/** The topic to send messages to. Only one topic per sender. Wildcards are not supported. */
	private @Setter String topic;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(topic)) throw new ConfigurationException("topic must be specified");
		if(topic.contains(",")) throw new ConfigurationException("Only one topic is allowed to be used for sender.");
		if(topic.contains("*")) throw new ConfigurationException("Wildcards are not allowed to be used for sender.");
		properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
	}

	@Override
	public void open() throws SenderException {
		producer = new KafkaProducer<>(properties);
	}

	@Override
	public void close() throws SenderException {
		producer.close();
	}

	@Override
	public SenderResult sendMessage(nl.nn.adapterframework.stream.Message message, PipeLineSession session) throws SenderException {
		ProducerRecord<String, byte[]> producerRecord;
		byte[] messageBytes;
		try {
			messageBytes = message.asByteArray();
		} catch (IOException e) {
			throw new SenderException("Failed to convert message to byte array", e);
		}
		producerRecord = new ProducerRecord<>(topic, messageBytes);
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
