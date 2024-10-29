/*
   Copyright 2017 - 2023 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.IConfigurable;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.util.CredentialFactory;

@Log4j2
public class MqttFacade implements HasPhysicalDestination, IConfigurable {
	private final @Getter String domain = "MQTT";
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	private @Getter String name;
	private @Getter int timeout = 3000;
	private @Getter int keepAliveInterval = 30000;
	private @Getter String brokerUrl;
	private @Getter String topic;
	private @Getter int qos = 2;
	private @Getter boolean cleanSession = false;
	private @Getter String persistenceDirectory;
	private @Getter boolean automaticReconnect = true;
	private @Getter String charset = "UTF-8";
	private @Getter String clientId;

	private @Getter String username;
	private @Getter String password;
	private @Getter String authAlias;

	protected MqttClient client;
	protected MqttConnectOptions connectOptions;

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getClientId())) {
			throw new ConfigurationException("clientId must be specified");
		}
		if (StringUtils.isEmpty(getBrokerUrl())) {
			throw new ConfigurationException("brokerUrl must be specified");
		}
		if (StringUtils.isEmpty(getTopic())) {
			throw new ConfigurationException("topic must be specified");
		}
		if (StringUtils.isEmpty(getPersistenceDirectory())) {
			throw new ConfigurationException("persistenceDirectory must be specified");
		}
		connectOptions = new MqttConnectOptions();
		connectOptions.setCleanSession(isCleanSession());
		connectOptions.setAutomaticReconnect(isAutomaticReconnect());
		connectOptions.setConnectionTimeout(getTimeout());
		connectOptions.setKeepAliveInterval(getKeepAliveInterval());
		connectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_DEFAULT); //Default: 0, V3.1: 3, V3.1.1: 4

		if(!StringUtils.isEmpty(getAuthAlias()) || (!StringUtils.isEmpty(getUsername()) && !StringUtils.isEmpty(getPassword()))) {
			CredentialFactory credentialFactory = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
			connectOptions.setUserName(credentialFactory.getUsername());
			connectOptions.setPassword(credentialFactory.getPassword().toCharArray());
		}

		MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(getPersistenceDirectory());
		try {
			client = new MqttClient(brokerUrl, clientId, dataStore);
		} catch (MqttException e) {
			throw new ConfigurationException("Could not create client", e);
		}
	}

	public void start() {
		try {
			client.connect(connectOptions);
		} catch (MqttException e) {
			throw new LifecycleException("Could not connect", e);
		}
	}

	public void stop() {
		try {
			client.disconnect();
		} catch (MqttException e) {
			log.warn("{}caught exception stopping listener", getLogPrefix(), e);
		}
	}

	@Override
	public String getPhysicalDestinationName() {
		return "TOPIC(" + getTopic() + ") on (" + getBrokerUrl() + ")";
	}

	@Override
	public String toString() {
		ToStringBuilder ts = new ToStringBuilder(this);
		ts.append("topic", getTopic());
		ts.append("broker", getBrokerUrl());
		ts.append("clientId", getClientId());
		ts.append("qos", getQos());
		ts.append("timeout", getTimeout());
		return ts.toString();
	}

	@Override
	public void setName(String newName) {
		name = newName;
	}

	protected String getLogPrefix() {
		return "["+getName()+"] ";
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public void setKeepAliveInterval(int keepAliveInterval) {
		this.keepAliveInterval = keepAliveInterval;
	}

	/** see <a href="https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttClient.html#MqttClient-java.lang.String-java.lang.String-org.eclipse.paho.client.mqttv3.MqttClientPersistence-" target="_blank">MqttClient(java.lang.String serverURI, java.lang.String clientId, MqttClientPersistence persistence)</a> */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/** see <a href="https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttClient.html#MqttClient-java.lang.String-java.lang.String-org.eclipse.paho.client.mqttv3.MqttClientPersistence-" target="_blank">MqttClient(java.lang.String serverURI, java.lang.String clientId, MqttClientPersistence persistence)</a> */
	public void setBrokerUrl(String brokerUrl) {
		this.brokerUrl = brokerUrl;
	}

	/** see <a href="https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttClient.html#subscribe-java.lang.String-" target="_blank">MqttClient.subscribe(java.lang.String topicFilter)</a> */
	public void setTopic(String topic) {
		this.topic = topic;
	}

	/** see <a href="https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttClient.html#MqttClient-java.lang.String-java.lang.String-org.eclipse.paho.client.mqttv3.MqttClientPersistence-" target="_blank">MqttClient(java.lang.String serverURI, java.lang.String clientId, MqttClientPersistence persistence)</a>
	 * @ff.default 2
	 */
	public void setQos(int qos) {
		this.qos = qos;
	}

	/** see <a href="https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html#setCleanSession-boolean-" target="_blank">MqttConnectOptions.setCleanSession(boolean cleanSession)</a>
	 * @ff.default true
	 */
	public void setCleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}

	/** see <a href="https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/persist/MqttDefaultFilePersistence.html" target="_blank">MqttDefaultFilePersistence</a> and <a href="https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttClient.html" target="_blank">MqttClient</a> */
	public void setPersistenceDirectory(String persistenceDirectory) {
		this.persistenceDirectory = persistenceDirectory;
	}


	/** see <a href="https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html#setAutomaticReconnect-boolean-" target="_blank">MqttConnectOptions.setAutomaticReconnect(boolean automaticReconnect)</a> (apart from this recover job will also try to recover)
	 * @ff.default true
	 */
	public void setAutomaticReconnect(boolean automaticReconnect) {
		this.automaticReconnect = automaticReconnect;
	}

	/** character encoding of received messages
	 * @ff.default UTF-8
	 */
	public void setCharset(String charset) {
		this.charset = charset;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}
}
