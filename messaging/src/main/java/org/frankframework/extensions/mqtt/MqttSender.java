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
import lombok.extern.log4j.Log4j2;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ISenderWithParameters;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
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
		try {
			super.open();
		} catch (Exception e) {
			throw new SenderException("Could not publish to topic", e);
		}
	}

	@Override
	public void close() {
		super.close();
	}

	@Override
	public void addParameter(IParameter p) {
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
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		return new SenderResult(sendMessage(message, session, null));
	}

	public Message sendMessage(Message message, PipeLineSession session, String soapHeader) throws SenderException {
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
