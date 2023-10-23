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

import java.util.Collections;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import lombok.SneakyThrows;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.stream.Message;

public class KafkaReceiverTest {
	MockConsumer<String, byte[]> mockListener = new MockConsumer<>(OffsetResetStrategy.EARLIEST);
	KafkaListener listener;

	@BeforeEach
	@SneakyThrows
	void setUp() {
		listener = new KafkaListener();
		listener.setTopics("test.*.test2");
		listener.setClientId("test");
		listener.setGroupId("testGroup");
		listener.setBootstrapServers("example.com:9092"); //dummy, doesn't connect.
		listener.configure();
		listener.setConsumer(mockListener);
	}

	@ParameterizedTest
	@MethodSource
	void validateParameters(Consumer<KafkaListener> consumer, boolean shouldSucceed, String name) {
		consumer.accept(listener);
		if(shouldSucceed) Assertions.assertDoesNotThrow(listener::configure, name);
		else Assertions.assertThrows(ConfigurationException.class, listener::configure, name);
	}
	public static Consumer<KafkaListener> wrap(Consumer<KafkaListener> function) {
		return function;
	}
	private static Stream<Arguments> validateParameters() {
		return Stream.of(
				Arguments.of(wrap(listener->listener.setTopics(null)), false, "null topics"),
				Arguments.of(wrap(listener->listener.setTopics("")), false, "empty topics"),
				Arguments.of(wrap(listener->listener.setTopics("test")), true, "valid topics 1"),
				Arguments.of(wrap(listener->listener.setTopics("test,test2")), true, "valid topics 2"),
				Arguments.of(wrap(listener->listener.setTopics("test.test2")), true, "valid topics 3"),
				Arguments.of(wrap(listener->listener.setGroupId(null)), false, "null groupId"),
				Arguments.of(wrap(listener->listener.setGroupId("")), false, "empty groupId"),
				Arguments.of(wrap(listener->listener.setGroupId("test")), true, "valid groupId"),
				Arguments.of(wrap(listener->listener.setPatternRecheckInterval(0)), false, "0 patternRecheckInterval"),
				Arguments.of(wrap(listener->listener.setPatternRecheckInterval(100)), true, "valid patternRecheckInterval"),
				Arguments.of(wrap(listener->listener.setFromBeginning(true)), true, "valid fromBeginning"),
				Arguments.of(wrap(listener->listener.setFromBeginning(false)), true, "valid fromBeginning")
		);
	}

	@Test
	@SneakyThrows
	public void test() {
		String topic="test.test.test2";
		listener.open();
		HashMap<TopicPartition, Long> startOffsets = new HashMap<>();
		TopicPartition topicPartition = new TopicPartition(topic, 0);
		startOffsets.put(topicPartition, 0L);
		mockListener.updateBeginningOffsets(startOffsets);
		mockListener.rebalance(Collections.singletonList(topicPartition));
		mockListener.addRecord(new ConsumerRecord<>(topic, 0, 0, "", "testtesttest".getBytes()));

		RawMessageWrapper<ConsumerRecord<String, byte[]>> wrapper = listener.getRawMessage(new HashMap<>());
		Assertions.assertEquals(topic, wrapper.getContext().get("kafkaTopic"));

		Message message = listener.extractMessage(wrapper, new HashMap<>());
		Assertions.assertEquals("testtesttest",message.asString());

		Assertions.assertNull(listener.getRawMessage(new HashMap<>()));
		listener.close();
	}
}
