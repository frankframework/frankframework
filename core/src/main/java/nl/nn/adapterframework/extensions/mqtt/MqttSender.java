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

import org.eclipse.paho.client.mqttv3.MqttMessage;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.stream.Message;

/**
 * MQTT listener which will connect to a broker and subscribe to a topic.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.mqtt.MqttListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setClientId(String) clientId}</td><td>see <a href="https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttClient.html#MqttClient-java.lang.String-java.lang.String-org.eclipse.paho.client.mqttv3.MqttClientPersistence-" target="_blank">MqttClient(java.lang.String serverURI, java.lang.String clientId, MqttClientPersistence persistence)</a></td><td></td></tr>
 * <tr><td>{@link #setBrokerUrl(String) brokerUrl}</td><td>see <a href="https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttClient.html#MqttClient-java.lang.String-java.lang.String-org.eclipse.paho.client.mqttv3.MqttClientPersistence-" target="_blank">MqttClient(java.lang.String serverURI, java.lang.String clientId, MqttClientPersistence persistence)</a></td><td></td></tr>
 * <tr><td>{@link #setTopic(String) topic}</td><td>see <a href="https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttClient.html#subscribe-java.lang.String-" target="_blank">MqttClient.subscribe(java.lang.String topicFilter)</a></td><td></td></tr>
 * <tr><td>{@link #setQos(int) qos}</td><td>see <a href="https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttClient.html#MqttClient-java.lang.String-java.lang.String-org.eclipse.paho.client.mqttv3.MqttClientPersistence-" target="_blank">MqttClient(java.lang.String serverURI, java.lang.String clientId, MqttClientPersistence persistence)</a></td><td>2</td></tr>
 * <tr><td>{@link #setCleanSession(boolean) cleanSession}</td><td>see <a href="https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html#setCleanSession-boolean-" target="_blank">MqttConnectOptions.setCleanSession(boolean cleanSession)</a></td><td>true</td></tr>
 * <tr><td>{@link #setPersistenceDirectory(String) persistenceDirectory}</td><td>see <a href="https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/persist/MqttDefaultFilePersistence.html" target="_blank">MqttDefaultFilePersistence</a> and <a href="https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttClient.html" target="_blank">MqttClient</a></td><td>true</td></tr>
 * <tr><td>{@link #setAutomaticReconnect(boolean) automaticReconnect}</td><td>see <a href="https://www.eclipse.org/paho/files/javadoc/org/eclipse/paho/client/mqttv3/MqttConnectOptions.html#setAutomaticReconnect-boolean-" target="_blank">MqttConnectOptions.setAutomaticReconnect(boolean automaticReconnect)</a> (apart from this recover job will also try to recover)</td><td>true</td></tr>
 * <tr><td>{@link #setCharset(String) charset}</td><td>character encoding of received messages</td><td>UTF-8</td></tr>
 * </table>
 * 
 * Links to <a href="https://www.eclipse.org/paho/files/javadoc" target="_blank">https://www.eclipse.org/paho/files/javadoc</a> are opened in a new window/tab because the response from eclipse.org contains header X-Frame-Options:SAMEORIGIN which will make the browser refuse to open the link inside this frame.
 * 
 * @author Niels Meijer
 */

public class MqttSender extends MqttFacade implements ISenderWithParameters {
	protected ParameterList paramList = null;

	@Override
	public void configure() throws ConfigurationException {
		if (paramList!=null) {
			paramList.configure();
		}

		super.configure();
	}

	@Override
	public void open() throws SenderException {
//		try {
//			super.open();
//		} catch (Exception e) {
//			throw new SenderException("Could not publish to topic", e);
//		}
	}

	@Override
	public void close() {
		super.close();
	}

	@Override
	public void addParameter(Parameter p) { 
		if (paramList==null) {
			paramList = new ParameterList();
		}
		paramList.add(p);
	}

	@Override
	public ParameterList getParameterList() {
		return paramList;
	}

	@Override
	public Message sendMessage(Message message, PipeLineSession session) throws SenderException, TimeOutException {
		return sendMessage(message, session, null);
	}

	public Message sendMessage(Message message, PipeLineSession session, String soapHeader) throws SenderException, TimeOutException {
		try {
			if(!client.isConnected()) {
				super.open();
			}

			log.debug(message);
			MqttMessage MqttMessage = new MqttMessage();
			MqttMessage.setPayload(message.asByteArray());
			MqttMessage.setQos(getQos());
			client.publish(getTopic(), MqttMessage);
		}
		catch (Exception e) {
			throw new SenderException(e);
		}
		return message;
	}

	@Override
	public boolean isSynchronous() {
		return false;
	}
}
