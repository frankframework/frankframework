/*
   Copyright 2017, 2020-2026 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.jspecify.annotations.NonNull;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IMessageHandler;
import org.frankframework.core.IPushingListener;
import org.frankframework.core.IbisExceptionListener;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.receivers.Receiver;
import org.frankframework.stream.Message;
import org.frankframework.util.CloseUtils;

/**
 * MQTT listener which will connect to a broker and subscribe to a topic.
 *
 * Links to <a href="https://www.eclipse.org/paho/files/javadoc" target="_blank">https://www.eclipse.org/paho/files/javadoc</a> are opened in a new window/tab because the response from eclipse.org contains header X-Frame-Options:SAMEORIGIN which will make the browser refuse to open the link inside this frame.
 *
 * {@inheritClassDoc}
 *
 * {@code AutomaticReconnect} works differently on listeners as the Frank!Framework handles this. Its therefor always false regardless of what is configured on the driver. See {@link Receiver#setOnError(Receiver.OnError) onError} for more information.
 *
 * @author Jaco de Groot
 * @author Niels Meijer
 */

@Log4j2
public class MqttListener extends MqttFacade implements IPushingListener<MqttMessage>, MqttCallback {

	private IMessageHandler<MqttMessage> messageHandler;
	private IbisExceptionListener ibisExceptionListener;

	@Override
	public void setHandler(@NonNull IMessageHandler<MqttMessage> messageHandler) {
		this.messageHandler = messageHandler;
	}

	@Override
	public void setExceptionListener(@NonNull IbisExceptionListener ibisExceptionListener) {
		this.ibisExceptionListener = ibisExceptionListener;
	}

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getTopic())) {
			throw new ConfigurationException("topic must be specified");
		}

		super.configure();
	}

	@Override
	public void start() {
		try {
			client = getClientFactory().createMqttClient();
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
	public void connectionLost(Throwable throwable) {
		log.warn("Connection to MQTT server lost; trying to reconnect", throwable);
		try {
			// Attempt once
			client.reconnect();
		} catch (MqttException e) {
			log.warn("Failed to reconnect to MQTT server", e);
			throwable.addSuppressed(e);

			// Call receiver which will set status to error after which the recover job will try to recover.
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
	public RawMessageWrapper<MqttMessage> wrapRawMessage(@NonNull MqttMessage message, @NonNull PipeLineSession session) {
		return new RawMessageWrapper<>(message, String.valueOf(message.getId()), null);
	}

	@Override
	public Message extractMessage(@NonNull RawMessageWrapper<MqttMessage> rawMessage, @NonNull Map<String, Object> context) {
		return new Message(rawMessage.getRawMessage().getPayload(), getCharset());
	}

	@Override
	public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<MqttMessage> rawMessage, PipeLineSession pipeLineSession) {
		// No-op
	}
}
