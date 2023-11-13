package nl.nn.adapterframework.extensions.kafka;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.Lists;

import nl.nn.adapterframework.configuration.ConfigurationException;

public class KafkaPatternValidatorTest {
	KafkaListener listener;

	@BeforeEach
	void setUp() {
		listener = new KafkaListener();
		listener.setClientId("test");
		listener.setGroupId("testGroup");
		listener.setBootstrapServers("example.com:9092"); //dummy, doesn't connect.
	}
	@ParameterizedTest
	@MethodSource
	public void validateValidTopics(String topics, String expected) {
		listener.setTopics(topics);
		Assertions.assertDoesNotThrow(listener::configure, topics);
	}
	public static Stream<Arguments> validateValidTopics() {
		return Stream.of(
				Arguments.of("test", "test"),
				Arguments.of("test,test2", "test|test2"),
				Arguments.of("Topic1, Topic2 , Topic3", "Topic1|Topic2|Topic3"),
				Arguments.of("test.*.test3,dat.test2.test.*", "test.*.test3|dat.test2.test.*")
		);
	}

	@ParameterizedTest
	@MethodSource
	public void validateInvalidTopics(String topics) {
		listener.setTopics(topics);
		Assertions.assertThrows(ConfigurationException.class, listener::configure, topics);
	}
	public static Stream<Arguments> validateInvalidTopics() {
		//Simply List.of() doesn't work, because it doesn't allow null values.
		List<String> data = Lists.newArrayList(
				null,
				"",
				" ",
				"test|test2"
		);
		return data.stream().map(Arguments::of);
	}
}
