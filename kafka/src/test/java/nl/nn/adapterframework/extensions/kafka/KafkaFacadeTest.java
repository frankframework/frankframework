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

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import lombok.SneakyThrows;
import nl.nn.adapterframework.configuration.ConfigurationException;

public class KafkaFacadeTest {
	KafkaFacade facade;

	@BeforeEach
	@SneakyThrows
	void setUp() {
		facade = new KafkaFacade() {
			@Override
			public String getPhysicalDestinationName() {
				return "";
			}
		};
		facade.setClientId("test");
		facade.setBootstrapServers("example.com:9092"); //dummy, doesn't connect.
		facade.configure();
	}
	@ParameterizedTest
	@MethodSource
	void validateParameters(Consumer<KafkaFacade> consumer, boolean shouldSucceed, String name) {
		consumer.accept(facade);
		if(shouldSucceed) Assertions.assertDoesNotThrow(facade::configure, name);
		else Assertions.assertThrows(ConfigurationException.class, facade::configure, name);
	}
	public static Consumer<KafkaFacade> wrap(Consumer<KafkaFacade> function) {
		return function;
	}
	static Stream<Arguments> validateParameters() {
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
