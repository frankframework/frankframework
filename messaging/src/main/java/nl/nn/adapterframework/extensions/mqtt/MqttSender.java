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

package nl.nn.adapterframework.extensions.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import lombok.extern.log4j.Log4j2;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.stream.Message;

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
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
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
