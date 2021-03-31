/*
   Copyright 2013 Nationale-Nederlanden

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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

/**
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public abstract class MonitorAdapterBase implements IMonitorAdapter {
	protected Logger log = LogUtil.getLogger(this);

	private String SOURCE_ID_KEY="galm.source";

	private String name;
	private String hostname;
	private String sourceId;

	public MonitorAdapterBase() {
		super();
		log.debug("creating Destination ["+ClassUtils.nameOf(this)+"]");
	}

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getName())) {
			setName(ClassUtils.nameOf(this));
		}
		hostname=Misc.getHostname();
		AppConstants appConstants = AppConstants.getInstance();
		sourceId=appConstants.getResolvedProperty(SOURCE_ID_KEY);
		if (StringUtils.isEmpty(sourceId)) {
			throw new ConfigurationException("cannot read sourceId from ["+SOURCE_ID_KEY+"]");
		}
	}

	public String makeXml(String subSource, EventTypeEnum eventType, SeverityEnum severity, String message, Throwable t) {
		XmlBuilder eventXml = new XmlBuilder("event");
		eventXml.addAttribute("hostname",hostname);
		eventXml.addAttribute("source",sourceId);
		eventXml.addAttribute("subSource",subSource);
		eventXml.addAttribute("eventType",eventType.name());
		eventXml.addAttribute("severity",severity.name());
		eventXml.addAttribute("message",message);
		return eventXml.toXML();
	}

	@Override
	public XmlBuilder toXml() {
		XmlBuilder destinationXml=new XmlBuilder("destination");
		destinationXml.addAttribute("name",getName());
		destinationXml.addAttribute("className",getClass().getName());
		return destinationXml;
	}


	@Override
	public void setName(String string) {
		name = string;
	}
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void register(Object x) {
		MonitorManager.getInstance().registerDestination(this);
	}

}
