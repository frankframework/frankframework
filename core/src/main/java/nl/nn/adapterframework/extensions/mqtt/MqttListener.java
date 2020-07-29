/*
   Copyright 2017, 2020 WeAreFrank!

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

import java.util.Map;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.receivers.ReceiverAware;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.RunStateEnum;

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
 * @author Jaco de Groot
 * @author Niels Meijer
 */

public class MqttListener extends MqttFacade implements ReceiverAware<MqttMessage>, IPushingListener<MqttMessage>, MqttCallbackExtended {

	private ReceiverBase<MqttMessage> receiver;
	private IMessageHandler<MqttMessage> messageHandler;
	private IbisExceptionListener ibisExceptionListener;

	@Override
	public void setReceiver(IReceiver<MqttMessage> receiver) {
		this.receiver = (ReceiverBase<MqttMessage>)receiver;
	}

	@Override
	public IReceiver<MqttMessage> getReceiver() {
		return receiver;
	}

	@Override
	public void setHandler(IMessageHandler<MqttMessage> messageHandler) {
		this.messageHandler = messageHandler;
	}

	@Override
	public void setExceptionListener(IbisExceptionListener ibisExceptionListener) {
		this.ibisExceptionListener = ibisExceptionListener;
	}

	@Override
	public void configure() throws ConfigurationException {
		// See connectionLost(Throwable)
		receiver.setOnError(ReceiverBase.ONERROR_RECOVER);
		// Don't recreate client when trying to recover
		if (!receiver.isRecover() && !receiver.isRecoverAdapter()) {
			super.configure();
			client.setCallback(this);
		}
	}

	@Override
	public void open() throws ListenerException {
		try {
			super.open();
			client.subscribe(getTopic(), getQos());
		} catch (Exception e) {
			e.printStackTrace();
			throw new ListenerException("Could not subscribe to topic", e);
		}
	}

	@Override
	public void close() {
		// Prevent log.warn() when trying to recover. Recover will be triggered
		// when connectionLost was called or listener could not start in which
		// case client is already disconnected.
		if (!receiver.isRecover() && !receiver.isRecoverAdapter()) {
			super.close();
		}
	}

	@Override
	public void connectComplete(boolean reconnect, String brokerUrl) {
		String message = getLogPrefix() + "connection ";
		if (reconnect) {
			// Automatic reconnect by mqtt lib
			receiver.setRunState(RunStateEnum.STARTED);
			message = message + "restored";
		} else {
			message = message + "established";
		}
		receiver.getAdapter().getMessageKeeper().add(message);
		log.debug(message);
	}

	@Override
	public void connectionLost(Throwable throwable) {
		String message = getLogPrefix() + "connection lost";
		receiver.getAdapter().getMessageKeeper().add(message);
		log.debug(message);
		// Call receiver which will set status to error after which recover job
		// will try to recover. Note that at configure time
		// receiver.setOnError(ReceiverBase.ONERROR_RECOVER) was called. Also
		// note that mqtt lib will also try to recover (when automaticReconnect
		// is true) (see connectComplete also) which will probably recover
		// earlier because of it's smaller interval. When no connection was
		// available at startup the mqtt lib reconnect isn't started. So in this
		// case recovery will always be done by the recover job.
		ibisExceptionListener.exceptionThrown(this, throwable);
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		try {
			messageHandler.processRawMessage(this, message);
		} catch(Throwable t) {
			log.error("Could not process raw message", t);
		}
	}

	@Override
	public String getIdFromRawMessage(MqttMessage rawMessage, Map<String, Object> context) throws ListenerException {
		return "" + rawMessage.getId();
	}

	@Override
	public Message extractMessage(MqttMessage rawMessage, Map<String, Object> context) throws ListenerException {
		return new Message(rawMessage.getPayload(),getCharset());
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, Object rawMessageOrWrapper, Map<String, Object> context) throws ListenerException {
	}
}
