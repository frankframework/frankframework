/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.adapterframework.webcontrol.pipes;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.BaseConfigurationWarnings;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.extensions.esb.EsbJmsListener;
import nl.nn.adapterframework.extensions.esb.EsbUtils;
import nl.nn.adapterframework.http.RestListener;
import nl.nn.adapterframework.jdbc.JdbcSenderBase;
import nl.nn.adapterframework.jms.JmsListenerBase;
import nl.nn.adapterframework.jms.JmsMessageBrowser;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.MessageKeeperMessage;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Shows the configuration (with resolved variables).
 * 
 * @author Johan Verrips
 * @author Peter Leeuwenburgh
 */

public class ShowConfiguration extends ConfigurationBase {

	@Override
	protected String doGet(IPipeLineSession session) throws PipeRunException {
		String configurationName = retrieveConfigurationName(session);
		Configuration configuration = ibisContext.getIbisManager().getConfiguration(configurationName);
		boolean configAll;
		if (configurationName == null || configurationName.equalsIgnoreCase(CONFIG_ALL) || configuration == null) {
			configAll = true;
		} else {
			configAll = false;
		}

		List<Configuration> allConfigurations = ibisContext.getIbisManager().getConfigurations();
		XmlBuilder configurationsXml = toConfigurationsXml(allConfigurations);

		StringBuilder sb = new StringBuilder();
		boolean showConfigurationOriginal = AppConstants.getInstance().getBoolean("showConfiguration.original", false);
		if (configAll) {
			sb.append("<configurations>");
			for (Configuration configuration_iter : allConfigurations) {
				if (showConfigurationOriginal) {
					sb.append(XmlUtils.skipXmlDeclaration(configuration_iter.getOriginalConfiguration()));
				} else {
					sb.append(XmlUtils.skipXmlDeclaration(configuration_iter.getLoadedConfiguration()));
				}
			}
			sb.append("</configurations>");
		} else {
			if (showConfigurationOriginal) {
				sb.append(configuration.getOriginalConfiguration());
			} else {
				sb.append(configuration.getLoadedConfiguration());
			}
		}
		XmlBuilder configSource = new XmlBuilder("configSource");
		configSource.setCdataValue(sb.toString());
		configSource.addAttribute("original", showConfigurationOriginal);

		storeConfiguration(session, configAll, configuration);

		XmlBuilder root = new XmlBuilder("root");
		root.addSubElement(configurationsXml);
		root.addSubElement(configSource);

		return root.toXML();
	}

	private List<IAdapter> retrieveRegisteredAdapters(boolean configAll, Configuration configuration) {
		if (configAll) {
			return ibisContext.getIbisManager().getRegisteredAdapters();
		} else {
			return configuration.getRegisteredAdapters();
		}
	}

}