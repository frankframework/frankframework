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

import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.ConfigurationException;

public class KafkaFacadeTest {
	AbstractKafkaFacade facade;

	@BeforeEach
	void setUp() throws Exception {
		facade = new AbstractKafkaFacade() {
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
	void validateParameters(KafkaFacadeConfigurer configurer, boolean shouldSucceed, String name) {
		configurer.configure(facade);
		if(shouldSucceed) Assertions.assertDoesNotThrow(facade::configure, name);
		else Assertions.assertThrows(ConfigurationException.class, facade::configure, name);
	}

	@FunctionalInterface
	interface KafkaFacadeConfigurer {
		void configure(AbstractKafkaFacade kafkaFacade);
	}

	static Stream<Arguments> validateParameters() {
		return Stream.of(
				Arguments.of((KafkaFacadeConfigurer)listener->listener.setBootstrapServers(null), false, "null bootstrapServers"),
				Arguments.of((KafkaFacadeConfigurer)listener->listener.setBootstrapServers(""), false, "empty bootstrapServers"),
				Arguments.of((KafkaFacadeConfigurer)listener->listener.setBootstrapServers("example.com:9092"), true, "valid bootstrapServers"),
				Arguments.of((KafkaFacadeConfigurer)listener->listener.setClientId(null), false, "null clientId"),
				Arguments.of((KafkaFacadeConfigurer)listener->listener.setClientId(""), false, "empty clientId"),
				Arguments.of((KafkaFacadeConfigurer)listener->listener.setClientId("test"), true, "valid clientId")
		);
	}
}
