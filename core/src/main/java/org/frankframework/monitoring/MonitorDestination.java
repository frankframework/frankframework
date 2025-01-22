/*
   Copyright 2013 Nationale-Nederlanden, 2021-2025 WeAreFrank!

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
package org.frankframework.monitoring;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.monitoring.events.MonitorEvent;
import org.frankframework.stream.Message;
import org.frankframework.util.XmlBuilder;

/**
 * IMonitorAdapter that uses a {@link ISender sender} to send its message.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class MonitorDestination extends AbstractMonitorDestination {

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
				getSender().stop();
			} catch (LifecycleException e) {
				log.error("cannot close sender",e);
			}
		}
		try {
			getSender().start();
		} catch (LifecycleException e) {
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
				session.scheduleCloseOnSessionExit(newMessage);
			}
			getSender().sendMessageOrThrow(new Message(makeXml(monitorName, eventType, severity, eventCode, event)), session); // close() disables unit testing Message result
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
