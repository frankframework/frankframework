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
package org.frankframework.extensions.mqtt;

import java.util.Map;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.IMessageHandler;
import org.frankframework.core.IPushingListener;
import org.frankframework.core.IbisExceptionListener;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.lifecycle.events.AdapterMessageEvent;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.receivers.Receiver;
import org.frankframework.receivers.ReceiverAware;
import org.frankframework.stream.Message;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.RunState;

/**
 * MQTT listener which will connect to a broker and subscribe to a topic.
 *
 * Links to <a href="https://www.eclipse.org/paho/files/javadoc" target="_blank">https://www.eclipse.org/paho/files/javadoc</a> are opened in a new window/tab because the response from eclipse.org contains header X-Frame-Options:SAMEORIGIN which will make the browser refuse to open the link inside this frame.
 *
 * {@inheritClassDoc}
 *
 * @author Jaco de Groot
 * @author Niels Meijer
 */

@Log4j2
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

		if (StringUtils.isEmpty(getTopic())) {
			throw new ConfigurationException("topic must be specified");
		}
	}

	@Override
	public void start() {
		try {
			client = mqttClientFactory.getClientFactory(resourceName).createMqttClient();
			client.setCallback(this);
			client.subscribe(getTopic(), getQos());
		} catch (Exception e) {
			throw new LifecycleException("Could not subscribe to topic", e);
		}
	}

	@Override
	public void stop() {
		CloseUtils.closeSilently(client);
	}

	@Override
	public void connectComplete(boolean reconnect, String brokerUrl) {
		String message = "connection ";
		if (reconnect) {
			// Automatic reconnect by mqtt lib
			receiver.setRunState(RunState.STARTED);
			message = message + "restored";
		} else {
			message = message + "established";
		}

		Adapter adapter = getReceiver().getAdapter();
		adapter.publishEvent(new AdapterMessageEvent(adapter, this, message));
	}

	@Override
	public void connectionLost(Throwable throwable) {
		log.warn("Connection to MQTT server lost; trying to reconnect", throwable);
		try {
			client.reconnect();
		} catch (MqttException e) {
			throwable.addSuppressed(e);

			Adapter adapter = getReceiver().getAdapter();
			adapter.publishEvent(new AdapterMessageEvent(adapter, this, "connection lost; failed to reconnect"));

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
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// No-op
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) {
		try (PipeLineSession session = new PipeLineSession()) {
			messageHandler.processRawMessage(this, wrapRawMessage(message, session), session, false);
		} catch(Exception e) {
			log.error("Could not process raw message", e);
		}
	}

	@Override
	public RawMessageWrapper<MqttMessage> wrapRawMessage(MqttMessage message, PipeLineSession session) {
		return new RawMessageWrapper<>(message, String.valueOf(message.getId()), null);
	}

	@Override
	public Message extractMessage(@Nonnull RawMessageWrapper<MqttMessage> rawMessage, @Nonnull Map<String, Object> context) {
		return new Message(rawMessage.getRawMessage().getPayload(), getCharset());
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<MqttMessage> rawMessage, PipeLineSession pipeLineSession) {
		// No-op
	}
}
