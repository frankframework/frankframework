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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;

public class KafkaSenderTest {
	final MockProducer<String, byte[]> mockProducer = new MockProducer<>(true, new StringSerializer(), new ByteArraySerializer());
	final KafkaSender sender = new KafkaSender();

	@BeforeEach
	void setUp() throws Exception {
		sender.setTopic("test.test2");
		sender.setClientId("test");
		sender.setBootstrapServers("example.com:9092"); //dummy, doesn't connect.
		sender.configure();
		sender.setProducer(mockProducer);
	}

	@Test
	public void test() throws Exception {
		Message message = new Message("Hello World");
		PipeLineSession session = new PipeLineSession();
		sender.sendMessage(message, session);
		assertEquals(1, mockProducer.history().size(), "One message should be sent");
		ProducerRecord<?, ?> test = mockProducer.history().get(0);
		assertEquals("test.test2", test.topic(), "Topic should be test.test2");
		assertArrayEquals("Hello World".getBytes(), (byte[]) test.value(), "Message should be set");
	}

	@ParameterizedTest
	@MethodSource
	void validateInvalidTopic(String topic, String name) {
		sender.setTopic(topic);
		Assertions.assertThrows(ConfigurationException.class, sender::configure, name);
	}
	static Stream<Arguments> validateInvalidTopic() {
		return Stream.of(
				Arguments.of(null, "null topic"),
				Arguments.of("", "empty topic"),
				Arguments.of("test,test2", "multiple topics"),
				Arguments.of("test*", "wildcard topic")
		);
	}

	@Test
	void validateValidTopic() {
		sender.setTopic("test");
		Assertions.assertDoesNotThrow(sender::configure, "valid topic");
	}
}
