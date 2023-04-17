/*
   Copyright 2017, 2020, 2023 WeAreFrank!

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
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.receivers.ReceiverAware;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.RunState;

/**
 * MQTT listener which will connect to a broker and subscribe to a topic.
 *
 * Links to <a href="https://www.eclipse.org/paho/files/javadoc" target="_blank">https://www.eclipse.org/paho/files/javadoc</a> are opened in a new window/tab because the response from eclipse.org contains header X-Frame-Options:SAMEORIGIN which will make the browser refuse to open the link inside this frame.
 *
 * @author Jaco de Groot
 * @author Niels Meijer
 */

public class MqttListener extends MqttFacade implements ReceiverAware<MqttMessage>, IPushingListener<MqttMessage>, MqttCallbackExtended {

	private Receiver<MqttMessage> receiver;
	private IMessageHandler<MqttMessage> messageHandler;
	private IbisExceptionListener ibisExceptionListener;

	@Override
	public void setReceiver(Receiver<MqttMessage> receiver) {
		this.receiver = receiver;
	}

	@Override
	public Receiver<MqttMessage> getReceiver() {
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
		receiver.setOnError(Receiver.OnError.RECOVER);
		// Recover will be triggered when connectionLost was called or listener
		// could not start in which case client is already disconnected.

		super.configure();
		client.setCallback(this);
	}

	@Override
	public void open() throws ListenerException {
		try {
			super.open();
			client.subscribe(getTopic(), getQos());
		} catch (Exception e) {
			throw new ListenerException("Could not subscribe to topic", e);
		}
	}

	@Override
	public void connectComplete(boolean reconnect, String brokerUrl) {
		String message = getLogPrefix() + "connection ";
		if (reconnect) {
			// Automatic reconnect by mqtt lib
			receiver.setRunState(RunState.STARTED);
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
		// receiver.setOnError(Receiver.ONERROR_RECOVER) was called. Also
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
	public void messageArrived(String topic, MqttMessage message) {
		try (PipeLineSession session = new PipeLineSession()) {
			messageHandler.processRawMessage(this, new RawMessageWrapper<>(message, getIdFromRawMessage(message, session)), session, false);
		} catch(Throwable t) {
			log.error("Could not process raw message", t);
		}
	}

	@Override
	public String getIdFromRawMessageWrapper(RawMessageWrapper<MqttMessage> rawMessage, Map<String, Object> context) throws ListenerException {
		return rawMessage.getId();
	}

	@Override
	public String getIdFromRawMessage(MqttMessage rawMessage, Map<String, Object> threadContext) throws ListenerException {
		return String.valueOf(rawMessage.getId());
	}

	@Override
	public Message extractMessage(RawMessageWrapper<MqttMessage> rawMessage, Map<String, Object> context) throws ListenerException {
		return new Message(rawMessage.getRawMessage().getPayload(),getCharset());
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<MqttMessage> rawMessage, Map<String, Object> context) throws ListenerException {
	}
}
