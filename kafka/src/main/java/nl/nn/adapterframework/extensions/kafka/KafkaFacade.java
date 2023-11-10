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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IConfigurable;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import java.util.Properties;

public abstract class KafkaFacade implements HasPhysicalDestination, IConfigurable {
	private final @Getter(onMethod = @__(@Override)) String domain = "KAFKA";
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;
	protected final Logger log = LogUtil.getLogger(this);

	private @Setter @Getter String name;
	/** The bootstrap servers to connect to, as a comma separated list. */
	private @Setter @Getter(AccessLevel.PACKAGE) String bootstrapServers;
	/** The client id to use when connecting to the Kafka cluster. */
	private @Setter @Getter(AccessLevel.PACKAGE) String clientId;
	protected Properties properties;

	@Override
	public void configure() throws ConfigurationException {
		if(StringUtils.isEmpty(bootstrapServers)) throw new ConfigurationException("bootstrapServers must be specified");
		if(StringUtils.isEmpty(clientId)) throw new ConfigurationException("clientId must be specified");

		properties = new Properties();
		properties.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
		properties.setProperty(CommonClientConfigs.CLIENT_ID_CONFIG, clientId);
	}
}
