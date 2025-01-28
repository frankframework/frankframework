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
package org.frankframework.monitoring;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.doc.Mandatory;
import org.frankframework.monitoring.events.MonitorEvent;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.Misc;
import org.frankframework.util.XmlBuilder;

/**
 * Base class for Monitor Destination implementations.
 *
 * @author  Gerrit van Brakel
 */
public abstract class AbstractMonitorDestination implements IMonitorDestination, ApplicationContextAware {
	protected Logger log = LogUtil.getLogger(this);
	private @Getter @Setter ApplicationContext applicationContext;

	private boolean started = false;
	private @Getter String name;
	private String hostname;

	protected AbstractMonitorDestination() {
		log.debug("creating Destination [{}]", ()->ClassUtils.nameOf(this));
	}

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getName())) {
			throw new ConfigurationException("name is required");
		}

		hostname = Misc.getHostname();
	}

	protected String makeXml(String monitorName, EventType eventType, Severity severity, String eventCode, MonitorEvent event) {
		XmlBuilder eventXml = new XmlBuilder("event");
		eventXml.addAttribute("hostname", hostname);
		eventXml.addAttribute("monitor", monitorName);
		eventXml.addAttribute("source", event.getEventSourceName());
		eventXml.addAttribute("type", eventType.name());
		eventXml.addAttribute("severity", severity.name());
		eventXml.addAttribute("event", eventCode);
		return eventXml.asXmlString();
	}

	@Override
	public XmlBuilder toXml() {
		XmlBuilder destinationXml=new XmlBuilder("destination");
		destinationXml.addAttribute("name", getName());
		destinationXml.addAttribute("className", getUserClass(this).getCanonicalName());
		return destinationXml;
	}

	protected Class<?> getUserClass(Object clazz) {
		return org.springframework.util.ClassUtils.getUserClass(clazz);
	}

	@Override
	@Mandatory
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void start() {
		started = true;
	}

	@Override
	public void stop() {
		started = false;
	}

	@Override
	public boolean isRunning() {
		return started;
	}
}
