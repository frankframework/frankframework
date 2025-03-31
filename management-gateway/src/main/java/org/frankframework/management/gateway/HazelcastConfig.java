/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.management.gateway;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import com.hazelcast.config.Config;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.instance.impl.DefaultNodeContext;
import com.hazelcast.instance.impl.HazelcastInstanceFactory;
import com.hazelcast.instance.impl.MobyNames;

import lombok.extern.log4j.Log4j2;

import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.util.Environment;
import org.frankframework.util.PropertyLoader;

@Log4j2
public class HazelcastConfig {
	private static final AtomicInteger FACTORY_ID_GEN = new AtomicInteger();

	public static final String ATTRIBUTE_TYPE_KEY = "type";
	public static final String ATTRIBUTE_NAME_KEY = "name";
	public static final String ATTRIBUTE_APPLICATION_KEY = "application";
	public static final String ATTRIBUTE_VERSION_KEY = "version";

	public static final String REQUEST_TOPIC_NAME = "frank_integration_request_topic";
	public static final String AUTHENTICATION_HEADER_KEY = BusMessageUtils.HEADER_PREFIX+"user";
	private static final String VERSION = Environment.getModuleVersion("frankframework-management-gateway");

	public static final String FRANK_APPLICATION_CONFIG = "frank-configuration";
	public static final String FRANK_APPLICATION_KEYSET = "jwks";

	static Config createHazelcastConfig() {
		System.setProperty("hazelcast.config.schema.validation.enabled", "false");
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		String resource = "ff-hazelcast.xml";
		PropertyLoader properties = new PropertyLoader(classLoader, "hazelcast.properties");

		String hazelcastNetworkConfig = properties.getString("hazelcast.network.file", null);
		if (StringUtils.isBlank(hazelcastNetworkConfig)) {
			throw new IllegalStateException("property [hazelcast.network.file] not specified");
		}
		log.info("Starting Hazelcast with network configuration: {}", hazelcastNetworkConfig);

		Config config = Config.loadFromClasspath(classLoader, resource, properties);

		if (properties.getBoolean("hazelcast.liteMember", false)) {
			config.setLiteMember(true);
		}

		// Not recommended for production environments, and frankly better to configure one's cluster properly to begin with.
		config.getNetworkConfig().getJoin().getAutoDetectionConfig().setEnabled(false);

		return config;
	}

	enum InstanceType {
		/** outbound / controller */
		CONTROLLER,
		/** inbound / executor */
		WORKER
	}

	private static String computeName() {
		int instanceNum = FACTORY_ID_GEN.incrementAndGet();
		return MobyNames.getRandomName(instanceNum);
	}

	/**
	 * Type such as console, worker or flow
	 */
	static HazelcastInstance newHazelcastInstance(@Nonnull InstanceType type, @Nonnull Map<String, String> attributes) {
		Objects.requireNonNull(attributes);

		Config config = HazelcastConfig.createHazelcastConfig();
		String name = computeName();

		config.getMemberAttributeConfig().setAttribute(ATTRIBUTE_TYPE_KEY, type.name());
		config.getMemberAttributeConfig().setAttribute(ATTRIBUTE_NAME_KEY, name);
		if(VERSION != null) { // this value will be present once the artifact has been created, tests will fail otherwise.
			config.getMemberAttributeConfig().setAttribute(ATTRIBUTE_VERSION_KEY, VERSION);
		}

		attributes.entrySet().stream()
			.filter(e -> StringUtils.isNotBlank(e.getValue()))
			.forEach(e -> config.getMemberAttributeConfig().setAttribute(e.getKey(), e.getValue())
		);

		return HazelcastInstanceFactory.newHazelcastInstance(config, name, new DefaultNodeContext());
	}
}
