package nl.nn.adapterframework.extensions.kafka;

import nl.nn.adapterframework.configuration.ConfigurationException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Arrays;

public class KafkaPatternValidatorTest {
	KafkaListener listener;

	@BeforeEach
	void setUp() throws Exception {
		listener = new KafkaListener();
		listener.setClientId("test");
		listener.setGroupId("testGroup");
		listener.setBootstrapServers("example.com:9092"); //dummy, doesn't connect.
	}
	@ParameterizedTest
	@MethodSource
	public void validateTopics(String topics, boolean shouldThrow, List<String> expected) {
		listener.setTopics(topics);
		if(shouldThrow) {
			Assertions.assertThrows(ConfigurationException.class, listener::configure, topics);
			return;
		}
		Assertions.assertDoesNotThrow(listener::configure, topics);
		List<String> expectedPatterns = expected.stream().map(Pattern::compile).map(Pattern::pattern).collect(Collectors.toList());
		List<Pattern> actualPatterns = listener.getInternalListener().getTopicPatterns();
		List<String> actual = actualPatterns.stream().map(Pattern::pattern).collect(Collectors.toList());
		Assertions.assertEquals(expectedPatterns, actual, topics);
	}
	public static Stream<Arguments> validateTopics() {
		return Stream.of(
				Arguments.of(null, true, null),
				Arguments.of("", true, null),
				Arguments.of(" ", true, null),
				Arguments.of("test", false, Collections.singletonList("test")),
				Arguments.of("test,test2", false, Arrays.asList("test", "test2")),
				Arguments.of("Topic1, Topic2 , Topic3", false, Arrays.asList("Topic1", "Topic2", "Topic3"))

		);
	}
}
