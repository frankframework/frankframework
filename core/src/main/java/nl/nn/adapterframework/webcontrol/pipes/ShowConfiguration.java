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

import java.util.List;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.util.AppConstants;
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
	protected String doGet(PipeLineSession session) throws PipeRunException {
		IbisManager ibisManager = retrieveIbisManager();

		String configurationName = retrieveConfigurationName(session);
		Configuration configuration = null;
		boolean configAll;
		if (configurationName == null || configurationName.equalsIgnoreCase(CONFIG_ALL)) {
			configAll = true;
		} else {
			configuration = ibisManager.getConfiguration(configurationName);
			if (configuration == null) {
				configAll = true;
			} else {
				configAll = false;
			}
		}

		List<Configuration> allConfigurations = ibisManager.getConfigurations();
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
}