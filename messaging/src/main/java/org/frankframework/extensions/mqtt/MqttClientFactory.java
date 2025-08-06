/*
   Copyright 2017-2025 WeAreFrank!

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
import org.springframework.beans.factory.DisposableBean;

import org.frankframework.jdbc.datasource.FrankResource;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.Misc;
import org.frankframework.util.UUIDUtil;

public class MqttClientFactory implements DisposableBean {

	private final String resourceName;
	private final FrankResource resource;

	private final String clientId;

	public MqttClientFactory(String resourceName, FrankResource resource) {
		this.resourceName = resourceName;
		this.resource = resource;

		this.clientId = getClientId(resource.getProperties());
	}

	private String getClientId(Properties props) {
		String configuredClientId = props.getProperty("clientId");
		if (StringUtils.isNotEmpty(configuredClientId)) {
			return configuredClientId;
		}
		return Misc.getHostname()+"-"+ UUIDUtil.createSimpleUUID();

	}

	public MqttClient createMqttClient() throws MqttException {
		Properties props = resource.getProperties();

		MqttConnectOptions connectOptions = new MqttConnectOptions();
		ClassUtils.invokeSetters(connectOptions, props);

		connectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_DEFAULT); // Default: 0, V3.1: 3, V3.1.1: 4

		CredentialFactory cf = resource.getCredentials();
		if (cf.getUsername() != null && cf.getPassword() != null) {
			connectOptions.setUserName(cf.getUsername());
			connectOptions.setPassword(cf.getPassword().toCharArray());
		}


		MqttClient client = new MqttClient(resource.getUrl(), clientId, getMqttDataStore(props.getProperty("persistenceDirectory")));
		try {
			client.connect(connectOptions);
		} catch (MqttException e) {
			closeMqttClient(client);
			throw e;
		}
		return client;
	}

	public void closeMqttClient(MqttClient client) throws MqttException {
		try {
			if (client.isConnected()) {
				client.disconnect();
			}

			client.close(true);
		} catch (MqttException e) {
			client.disconnectForcibly();
			client.close(true);
			throw e;
		}
	}

	private MqttClientPersistence getMqttDataStore(String persistenceDirectory) {
		if (StringUtils.isEmpty(persistenceDirectory)) {
			return new MemoryPersistence();
		}

		return new MqttDefaultFilePersistence(persistenceDirectory);
	}

	@Override
	public void destroy() throws Exception {
		// TODO
	}
}
