/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public abstract class MonitorAdapterBase implements IMonitorAdapter, ApplicationContextAware {
	protected Logger log = LogUtil.getLogger(this);
	protected @Getter @Setter ApplicationContext applicationContext;

	private String name;
	private String hostname;

	protected MonitorAdapterBase() {
		log.debug("creating Destination ["+ClassUtils.nameOf(this)+"]");
	}

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getName())) {
			setName(ClassUtils.nameOf(this));
		}

		hostname = Misc.getHostname();
	}

	public String makeXml(String eventSource, EventType eventType, Severity severity, String message, Throwable t) {
		XmlBuilder eventXml = new XmlBuilder("event");
		eventXml.addAttribute("hostname", hostname);
		eventXml.addAttribute("source", eventSource);
		eventXml.addAttribute("type", eventType.name());
		eventXml.addAttribute("severity", severity.name());
		eventXml.addAttribute("message", message);
		return eventXml.toXML();
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
	public void setName(String string) {
		name = string;
	}
	@Override
	public String getName() {
		return name;
	}
}
