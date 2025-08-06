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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.DestinationType;
import org.frankframework.core.DestinationType.Type;
import org.frankframework.core.FrankElement;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.IConfigurable;
import org.frankframework.core.NameAware;
import org.frankframework.doc.Mandatory;

/**
 * Requires a resource to be configured. Example {@literal resources.yml}:
 * <pre>{@code
 * mqtt:
 *   - name: "my-connection"
 *     url: "tcp://host:port"
 *     username: ""
 *     password: ""
 *     authalias: "${property.name.here}"
 *     properties:
 *       automaticReconnect: "true"
 *       cleanSession: "false"
 * }</pre>
 * <br>
 * The clientId is automatically determined from {@code transactionmanager.uid}, but can optionally be overwritten. Be aware that the clientId must be unique
 * for each instance of the framework.
 * <br><br>
 * Inbound and outbound messages are persisted while they are in flight to prevent data loss. The default is an in memory store, but the {@literal persistenceDirectory}
 * flag can be used to set the disk storage location.
 */
@DestinationType(Type.MQTT)
public abstract class MqttFacade implements HasPhysicalDestination, IConfigurable, NameAware, FrankElement {

	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	protected @Setter @Getter MqttClientFactoryFactory mqttClientFactory = null; // Spring should wire this!

	private @Getter String name;
	protected @Getter String resourceName;
	private @Getter String topic;
	private @Getter int qos = 2;
	private @Getter String charset = "UTF-8";

	protected MqttClient client;

	@Override
	public void configure() throws ConfigurationException {
		if (resourceName == null) {
			throw new ConfigurationException("resourceName is required");
		}
	}

	@Override
	public String getPhysicalDestinationName() {
		return "TOPIC(" + getTopic() + ") on (" + client.getServerURI() + ")";
	}

	@Override
	public String toString() {
		ToStringBuilder ts = new ToStringBuilder(this);
		ts.append("name", getName());
		ts.append("topic", getTopic());
		return ts.toString();
	}

	@Override
	public void setName(String newName) {
		name = newName;
	}

	protected String getLogPrefix() {
		return "["+getName()+"] ";
	}

	/** see <a href="https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttClient.html#subscribe-java.lang.String-" target="_blank">MqttClient.subscribe(java.lang.String topicFilter)</a> */
	@Mandatory
	public void setTopic(String topic) {
		this.topic = topic;
	}

	/** see <a href="https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttClient.html#MqttClient-java.lang.String-java.lang.String-org.eclipse.paho.client.mqttv3.MqttClientPersistence-" target="_blank">MqttClient(java.lang.String serverURI, java.lang.String clientId, MqttClientPersistence persistence)</a>
	 * @ff.default 2
	 */
	public void setQos(int qos) {
		this.qos = qos;
	}

	/** character encoding of received messages
	 * @ff.default UTF-8
	 */
	public void setCharset(String charset) {
		this.charset = charset;
	}

	/**
	 * Name of the MqttClientSettings configuration in the `resources.yml`.
	 */
	@Mandatory
	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

}
