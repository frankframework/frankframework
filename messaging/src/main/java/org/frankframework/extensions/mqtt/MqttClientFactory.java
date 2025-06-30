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
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import lombok.SneakyThrows;

import org.frankframework.jdbc.datasource.FrankResource;
import org.frankframework.jdbc.datasource.ObjectFactory;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.Misc;
import org.frankframework.util.UUIDUtil;

public class MqttClientFactory extends ObjectFactory<MqttClient, Object> {

	public MqttClientFactory() {
		super(null, "mqtt", "MQTT");
	}

	@SneakyThrows
	private MqttClient map(FrankResource resource) {
		Properties props = resource.getProperties();

		MqttConnectOptions connectOptions = new MqttConnectOptions();
		ClassUtils.invokeSetters(connectOptions, props);

		connectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_DEFAULT); // Default: 0, V3.1: 3, V3.1.1: 4

		CredentialFactory cf = resource.getCredentials();
		if (cf.getUsername() != null && cf.getPassword() != null) {
			connectOptions.setUserName(cf.getUsername());
			connectOptions.setPassword(cf.getPassword().toCharArray());
		}

		String clientId = props.getProperty("clientId");
		if (StringUtils.isEmpty(clientId)) {
			clientId = Misc.getHostname()+"-"+ UUIDUtil.createSimpleUUID();
		}

		MqttClient client = new MqttClient(resource.getUrl(), clientId, getMqttDataStore(props.getProperty("persistenceDirectory")));
		client.connect(connectOptions);
		return client;
	}

	@Override
	protected MqttClient augment(Object object, String objectName) {
		if (object instanceof MqttClient client) {
			return client;
		}
		if (object instanceof FrankResource resource) {
			return map(resource);
		}
		throw new IllegalArgumentException("resource ["+objectName+"] not of required type");
	}

	private MqttClientPersistence getMqttDataStore(String persistenceDirectory) {
		if (StringUtils.isEmpty(persistenceDirectory)) {
			return new MemoryPersistence();
		}

		return new MqttDefaultFilePersistence(persistenceDirectory);
	}

	public MqttClient getClient(String name) {
		return get(name, null);
	}

	@Override
	protected void destroyObject(MqttClient object) throws Exception {
		try {
			if (object.isConnected()) {
				object.disconnect();
			}

			object.close(true);
		} catch (MqttException e) {
			object.disconnectForcibly();
			object.close(true);
			throw e;
		}

	}
}
