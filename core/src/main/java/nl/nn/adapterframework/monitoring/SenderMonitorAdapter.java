/*
   Copyright 2013 Nationale-Nederlanden, 2021-2023 WeAreFrank!

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
package nl.nn.adapterframework.monitoring;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.monitoring.events.MonitorEvent;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * IMonitorAdapter that uses a {@link ISender sender} to send its message.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class SenderMonitorAdapter extends MonitorDestinationBase {

	private @Getter @Setter ISender sender;
	private boolean senderConfigured=false;

	@Override
	public void configure() throws ConfigurationException {
		if (getSender()==null) {
			throw new ConfigurationException("No sender found");
		}
		if (StringUtils.isEmpty(getSender().getName())) {
			getSender().setName("sender of "+getName());
		}

		super.configure();

		if (!senderConfigured) {
			getSender().configure();
			senderConfigured=true;
		} else {
			try {
				getSender().close();
			} catch (SenderException e) {
				log.error("cannot close sender",e);
			}
		}
		try {
			getSender().open();
		} catch (SenderException e) {
			throw new ConfigurationException("cannot open sender",e);
		}
	}

	@Override
	public void fireEvent(String monitorName, EventType eventType, Severity severity, String eventCode, MonitorEvent event) {
		try (PipeLineSession session = new PipeLineSession()) {
			Message message = event.getEventMessage();
			if(!Message.isNull(message)) {
				Message newMessage = message.copyMessage();
				session.put(PipeLineSession.ORIGINAL_MESSAGE_KEY, newMessage);
				session.scheduleCloseOnSessionExit(newMessage, "Event fired by "+ monitorName);
			}
			getSender().sendMessageOrThrow(new Message(makeXml(monitorName, eventType, severity, eventCode, event)), session);
		} catch (Exception e) {
			log.error("Could not signal event", e);
		}
	}

	@Override
	public XmlBuilder toXml() {
		XmlBuilder result=super.toXml();
		XmlBuilder senderXml=new XmlBuilder("sender");
		senderXml.addAttribute("className", getUserClass(getSender()).getCanonicalName());
		result.addSubElement(senderXml);
		return result;
	}
}
