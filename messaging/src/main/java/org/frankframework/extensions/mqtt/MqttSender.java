/*
   Copyright 2017, 2022 WeAreFrank!

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

import jakarta.annotation.Nonnull;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ISenderWithParameters;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.doc.Optional;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.stream.Message;

/**
 * MQTT listener which will connect to a broker and subscribe to a topic.
 *
 * Links to <a href="https://www.eclipse.org/paho/files/javadoc" target="_blank">https://www.eclipse.org/paho/files/javadoc</a> are opened in a new window/tab because the response from eclipse.org contains header X-Frame-Options:SAMEORIGIN which will make the browser refuse to open the link inside this frame.
 *
 * @author Niels Meijer
 */

@Log4j2
public class MqttSender extends MqttFacade implements ISenderWithParameters {

	private static final String TOPIC_PARAMETER_NAME = "topic";

	protected @Nonnull ParameterList paramList = new ParameterList();

	@Override
	public void configure() throws ConfigurationException {
		paramList.configure();

		if (getTopic() == null && !paramList.hasParameter(TOPIC_PARAMETER_NAME)) {
			throw new ConfigurationException("topic must be specified");
		}

		super.configure();
	}

	@Override
	public void start() {
		super.start();
	}

	@Override
	public void stop() {
		super.stop();
	}

	@Override
	public void addParameter(IParameter p) {
		paramList.add(p);
	}

	@Override
	public @Nonnull ParameterList getParameterList() {
		return paramList;
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		try {
			if (!client.isConnected()) {
				super.start();
			}

			String topic = getTopic();
			IParameter topicParameter = getParameterList().findParameter(TOPIC_PARAMETER_NAME);

			if (topicParameter != null) {
				Message topicObj = Message.asMessage(topicParameter.getValue(null, message, session, false));
				topic = topicObj.asString();
			}

			if (topic == null) {
				throw new SenderException("Topic must not be null");
			}

			log.debug(message);
			MqttMessage mqttMessage = new MqttMessage();
			mqttMessage.setPayload(message.asByteArray());
			mqttMessage.setQos(getQos());
			client.publish(topic, mqttMessage);
		}
		catch (Exception e) {
			throw new SenderException(e);
		}
		return new SenderResult(message);
	}

	/**
	 * {@inheritDoc}
	 *
	 * Can be dynamically set using the {@value MqttSender#TOPIC_PARAMETER_NAME} parameter.
	 */
	@Override
	@Optional
	public void setTopic(String topic) {
		super.setTopic(topic);
	}

	@Override
	public boolean isSynchronous() {
		return false;
	}
}
