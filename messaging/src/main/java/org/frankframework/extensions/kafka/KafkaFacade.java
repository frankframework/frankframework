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

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.ApplicationContext;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.IConfigurable;

@Log4j2
public abstract class KafkaFacade implements HasPhysicalDestination, IConfigurable {
	private final @Getter String domain = "KAFKA";
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	private @Setter @Getter String name;
	/** The bootstrap servers to connect to, as a comma separated list. */
	private @Setter @Getter(AccessLevel.PACKAGE) String bootstrapServers;
	/** The client id to use when connecting to the Kafka cluster. */
	private @Setter @Getter(AccessLevel.PACKAGE) String clientId;

	protected final Properties properties = new Properties();

	@Override
	public void configure() throws ConfigurationException {
		if(StringUtils.isEmpty(bootstrapServers)) throw new ConfigurationException("bootstrapServers must be specified");
		if(StringUtils.isEmpty(clientId)) throw new ConfigurationException("clientId must be specified");

		properties.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		properties.setProperty(CommonClientConfigs.CLIENT_ID_CONFIG, clientId);
		properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getCanonicalName());
		properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getCanonicalName());
	}
}
