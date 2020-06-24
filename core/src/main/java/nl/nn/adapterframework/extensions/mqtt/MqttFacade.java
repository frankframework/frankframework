/*
   Copyright 2017 Integration Partners

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
package nl.nn.adapterframework.extensions.mqtt;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

public class MqttFacade implements HasPhysicalDestination {

	protected Logger log = LogUtil.getLogger(this);
	private String name;
	protected MqttClient client;
	protected MqttConnectOptions connectOptions;
	private int timeout = 3000;
	private int keepAliveInterval = 30000;
	private String brokerUrl;
	private String topic;
	private int qos = 2;
	private boolean cleanSession = false;
	private String persistenceDirectory;
	private boolean automaticReconnect = true;
	private String charset = "UTF-8";
	private String clientId;

	private String username;
	private String password;
	private String authAlias;

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

	public void open() throws Exception {
		try {
			client.connect(connectOptions);
		} catch (MqttSecurityException e) {
			throw new ListenerException("Could not connect", e);
		} catch (MqttException e) {
			throw new ListenerException("Could not connect", e);
		}
	}

	public void close() {
		try {
			client.disconnect();
		} catch (MqttException e) {
			log.warn(getLogPrefix() + "caught exception stopping listener", e);
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

	public void setName(String newName) {
		name = newName;
	}
	public String getName() {
		return name;
	}

	protected String getLogPrefix() {
		return "["+getName()+"] ";
	}

	public void setTimeout(int timeOut) {
		this.timeout = timeOut;
	}
	public int getTimeout() {
		return timeout;
	}

	public void setKeepAliveInterval(int keepAliveInterval) {
		this.keepAliveInterval = keepAliveInterval;
	}
	public int getKeepAliveInterval() {
		return keepAliveInterval;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getBrokerUrl() {
		return brokerUrl;
	}

	public void setBrokerUrl(String brokerUrl) {
		this.brokerUrl = brokerUrl;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public int getQos() {
		return qos;
	}

	public void setQos(int qos) {
		this.qos = qos;
	}

	public boolean isCleanSession() {
		return cleanSession;
	}

	public void setCleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}

	public String getPersistenceDirectory() {
		return persistenceDirectory;
	}

	public void setPersistenceDirectory(String persistenceDirectory) {
		this.persistenceDirectory = persistenceDirectory;
	}

	public boolean isAutomaticReconnect() {
		return automaticReconnect;
	}

	public void setAutomaticReconnect(boolean automaticReconnect) {
		this.automaticReconnect = automaticReconnect;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public void setUsername(String username) {
		this.username = username;
	}
	public String getUsername() {
		return username;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	public String getPassword() {
		return password;
	}

	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}
	public String getAuthAlias() {
		return authAlias;
	}
}
