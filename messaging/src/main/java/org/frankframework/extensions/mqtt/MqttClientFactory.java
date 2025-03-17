/*
   Copyright 2024-2025 WeAreFrank!

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
package org.frankframework.extensions.mqtt;

import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import lombok.SneakyThrows;

import org.frankframework.jdbc.datasource.MqttClientSettings;
import org.frankframework.jdbc.datasource.ObjectFactory;
import org.frankframework.util.AppConstants;

public class MqttClientFactory extends ObjectFactory<MqttClient, MqttClientSettings> {

	public MqttClientFactory() {
		super(MqttClientSettings.class, "mqtt", "MQTT");
	}

	@SneakyThrows
	@Override
	protected MqttClient map(MqttClientSettings data) {
		MqttConnectOptions connectOptions = new MqttConnectOptions();
		connectOptions.setCleanSession(data.isCleanSession());
		connectOptions.setAutomaticReconnect(data.isAutomaticReconnect());

		if (data.getTimeout() != 0) {
			connectOptions.setConnectionTimeout(data.getTimeout());
		}
		if (data.getKeepAliveInterval() != 0) {
			connectOptions.setKeepAliveInterval(data.getKeepAliveInterval());
		}
		connectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_DEFAULT); // Default: 0, V3.1: 3, V3.1.1: 4

		if (data.getUsername() != null && data.getPassword() != null) {
			connectOptions.setUserName(data.getUsername());
			connectOptions.setPassword(data.getPassword().toCharArray());
		}

		String clientId = data.getClientId();
		if (StringUtils.isEmpty(clientId)) {
			clientId = AppConstants.getInstance().getProperty("transactionmanager.uid");
		}

		try (MqttClient client = new MqttClient(data.getUrl(), clientId, getMqttDataStore(data.getPersistenceDirectory()))) {
			client.connect(connectOptions);

			return client;
		}
	}

	private MqttClientPersistence getMqttDataStore(String persistenceDirectory) {
		if (StringUtils.isEmpty(persistenceDirectory)) {
			return new MemoryPersistence();
		}

		return new MqttDefaultFilePersistence(persistenceDirectory);
	}

	public MqttClient getClient(String name) {
		return getClient(name, null);
	}

	public MqttClient getClient(String name, Properties environment) {
		return get(name, environment);
	}

}
