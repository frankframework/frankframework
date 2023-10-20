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

import lombok.SneakyThrows;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;

import org.apache.kafka.clients.producer.MockProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class KafkaSenderTest {
	MockProducer<String, byte[]> mockProducer = new MockProducer<>(true, new StringSerializer(), new ByteArraySerializer());
	KafkaSender sender = new KafkaSender();
	@BeforeEach
	@SneakyThrows
	void setUp() {
		sender.setTopic("test.test2");
		sender.setClientId("test");
		sender.setBootstrapServers("example.com:9092"); //dummy, doesn't connect.
		sender.setProducer(mockProducer);
		sender.configure();
	}

	@Test
	@SneakyThrows
	public void test() {
		Message message = new Message("Hello World");
		PipeLineSession session = new PipeLineSession();
		sender.sendMessage(message, session);
		assertEquals(mockProducer.history().size(), 1, "One message should be sent");
		ProducerRecord<String, byte[]> test = mockProducer.history().get(0);
		assertEquals(test.topic(), "test.test2", "Topic should be test.test2");
		assertArrayEquals(test.value(), "Hello World".getBytes(), "Message should be set");
	}

	@ParameterizedTest
	@MethodSource
	void validateParameters(Consumer<KafkaFacade> consumer, boolean shouldSucceed, String name) {
		consumer.accept(sender);
		if(shouldSucceed) Assertions.assertDoesNotThrow(sender::configure, name);
		else Assertions.assertThrows(ConfigurationException.class, sender::configure, name);
	}
	public static Consumer<KafkaFacade> wrap(Consumer<KafkaFacade> function) {
		return function;
	}
	private static Stream<Arguments> validateParameters() {
		return Stream.of(
				Arguments.of(wrap(listener->listener.setBootstrapServers(null)), false, "null bootstrapServers"),
				Arguments.of(wrap(listener->listener.setBootstrapServers("")), false, "empty bootstrapServers"),
				Arguments.of(wrap(listener->listener.setBootstrapServers("example.com:9092")), true, "valid bootstrapServers"),
				Arguments.of(wrap(listener->listener.setClientId(null)), false, "null clientId"),
				Arguments.of(wrap(listener->listener.setClientId("")), false, "empty clientId"),
				Arguments.of(wrap(listener->listener.setClientId("test")), true, "valid clientId")
		);
	}
}
