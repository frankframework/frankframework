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

import static org.apache.kafka.clients.consumer.ConsumerRecord.NULL_SIZE;
import static org.mockito.Mockito.spy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.stats.Value;
import org.apache.kafka.common.record.TimestampType;
import org.apache.kafka.common.utils.Time;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;

@SuppressWarnings("deprecation")
public class KafkaListenerTest {
	final MockConsumer<String, byte[]> mockListener = spy(new MockConsumer<>(OffsetResetStrategy.EARLIEST));
	KafkaListener listener;

	@BeforeEach
	void setUp() throws Exception {
		listener = spy(new KafkaListener() {
			@Override
			protected org.apache.kafka.clients.consumer.Consumer<String, byte[]> buildConsumer() {
				return mockListener;
			}
		});
		listener.setTopics("test.*.test2, anothertopic");
		listener.setClientId("test");
		listener.setGroupId("testGroup");
		listener.setBootstrapServers("example.com:9092"); //dummy, doesn't connect.
		listener.configure();

		Map<MetricName, Metric> metrics = new HashMap<>();
		MetricName metricName = new MetricName("response-total", "consumer-node-metrics", "The total number of responses received", Collections.singletonMap("client-id", "test"));
		Value value = new Value();
		value.record(null, 1.0, 0);
		metrics.put(metricName, new KafkaMetric(
				new Object(),
				metricName,
				value,
				null,
				Time.SYSTEM
		));

		Mockito.doNothing().when(listener).checkConnection();
	}

	@ParameterizedTest
	@MethodSource
	void validateParameters(Consumer<KafkaListener> configurer, boolean shouldSucceed, String name) {
		configurer.accept(listener);
		if(shouldSucceed) Assertions.assertDoesNotThrow(listener::configure, name);
		else Assertions.assertThrows(ConfigurationException.class, listener::configure, name);
	}

	public static Consumer<KafkaListener> configure(Consumer<KafkaListener> function) {
		return function;
	}
	static Stream<Arguments> validateParameters() {
		return Stream.of(
				Arguments.of(configure(listener->listener.setTopics(null)), false, "null topics"),
				Arguments.of(configure(listener->listener.setTopics("")), false, "empty topics"),
				Arguments.of(configure(listener->listener.setTopics("test")), true, "valid topics 1"),
				Arguments.of(configure(listener->listener.setTopics("test,test2")), true, "valid topics 2"),
				Arguments.of(configure(listener->listener.setTopics("test.test2")), true, "valid topics 3"),
				Arguments.of(configure(listener->listener.setGroupId(null)), false, "null groupId"),
				Arguments.of(configure(listener->listener.setGroupId("")), false, "empty groupId"),
				Arguments.of(configure(listener->listener.setGroupId("test")), true, "valid groupId"),
				Arguments.of(configure(listener->listener.setPatternRecheckInterval(0)), false, "0 patternRecheckInterval"),
				Arguments.of(configure(listener->listener.setPatternRecheckInterval(100)), true, "valid patternRecheckInterval"),
				Arguments.of(configure(listener->listener.setOffsetStrategy(OffsetResetStrategy.EARLIEST)), true, "valid fromBeginning"),
				Arguments.of(configure(listener->listener.setOffsetStrategy(OffsetResetStrategy.LATEST)), true, "valid fromBeginning"),
				Arguments.of(configure(listener->listener.setPatternRecheckInterval(0)), false, "0 patternRecheckInterval"),
				Arguments.of(configure(listener->listener.setPatternRecheckInterval(9)), false, "9 patternRecheckInterval"),
				Arguments.of(configure(listener->listener.setPatternRecheckInterval(10)), true, "10 patternRecheckInterval")
		);
	}

	@ParameterizedTest
	@MethodSource
	public void test(String topic) throws Exception {
		listener.start();
		HashMap<TopicPartition, Long> startOffsets = new HashMap<>();
		TopicPartition topicPartition = new TopicPartition(topic, 0);
		startOffsets.put(topicPartition, 0L);
		mockListener.updateBeginningOffsets(startOffsets);
		mockListener.rebalance(Collections.singletonList(topicPartition));
		RecordHeaders headers = new RecordHeaders();
		headers.add("headerKey", "headerValue".getBytes());
		ConsumerRecord<String, byte[]> record = new ConsumerRecord<>(topic, 0, 0, ConsumerRecord.NO_TIMESTAMP, TimestampType.NO_TIMESTAMP_TYPE, NULL_SIZE, NULL_SIZE, "", "testtesttest".getBytes(),
				headers, Optional.empty());
		mockListener.addRecord(record);

		Assertions.assertNull(mockListener.committed(Set.of(topicPartition)).get(topicPartition));

		RawMessageWrapper<ConsumerRecord<String, byte[]>> wrapper = listener.getRawMessage(new HashMap<>());
		Assertions.assertEquals(1L, mockListener.committed(Set.of(topicPartition)).get(topicPartition).offset());
		Message message = listener.extractMessage(wrapper, new HashMap<>());

		Assertions.assertEquals(1L, mockListener.committed(Set.of(topicPartition)).get(topicPartition).offset());

		Assertions.assertEquals(topic, message.getContext().get("kafkaTopic"));
		Assertions.assertEquals("testtesttest",message.asString());
		Map<String, String> receivedHeaders = (Map<String, String>) message.getContext().get("kafkaHeaders");
		Assertions.assertEquals("headerValue", receivedHeaders.get("headerKey"));

		Assertions.assertEquals(1L, mockListener.committed(Set.of(topicPartition)).get(topicPartition).offset());

		Assertions.assertNull(listener.getRawMessage(new HashMap<>()));

		Assertions.assertEquals(1L, mockListener.committed(Set.of(topicPartition)).get(topicPartition).offset());
		listener.stop();
	}

	static Stream<Arguments> test() {
		return Stream.of(
				"test.test.test2",
				"anothertopic"
		).map(Arguments::of);
	}

	@Test
	void throwsErrorOnBadConnection() {
		Assertions.assertDoesNotThrow(listener::start, "shouldn't throw on valid connection");

		Mockito.reset(listener);

		Assertions.assertThrows(LifecycleException.class, listener::start, "should throw on (simulated) bad connection");
	}
}
