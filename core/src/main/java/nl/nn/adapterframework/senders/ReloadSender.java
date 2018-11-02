/*
   Copyright 2013, 2018 Nationale-Nederlanden

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
package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Performs a reload on database config .
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setForceReload(String) forceReload}</td><td>reload the configuration regardless of the version</td><td>false</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * <p>It is possible to set the name of the configuration with the parameter 'name'.</p>
 * <p>You can dynamically set 'forceReload' attribute with the parameter 'forceReload'.</p>
 * @author	Lars Sinke
 * @author	Niels Meijer
 */
public class ReloadSender extends SenderWithParametersBase implements ConfigurationAware {

	private Configuration configuration;
	private boolean forceReload = false;

	@Override
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws TimeOutException, SenderException {

		String configName = null;
		String activeVersion = null;

		ParameterValueList pvl = null;
		try {
			if (prc != null && paramList != null) {
				pvl = prc.getValues(paramList);
				if(pvl.getParameterValue("name") != null)
					configName = (String) pvl.getParameterValue("name").getValue();
				if(pvl.getParameterValue("forceReload") != null)
					setForceReload((Boolean) pvl.getParameterValue("forceReload").getValue());
			}
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix()+"Sender ["+getName()+"] caught exception evaluating parameters",e);
		}

		try {
			if(configName == null)
				configName = XmlUtils.evaluateXPathNodeSetFirstElement(message,
					"row/field[@name='NAME']");
		} catch (Exception e) {
			throw new SenderException(getLogPrefix()+"error evaluating Xpath expression configName", e);
		}

		try {
			if(!getForceReload())
				activeVersion = XmlUtils.evaluateXPathNodeSetFirstElement(message,
					"row/field[@name='VERSION']");
		} catch (Exception e) {
			throw new SenderException(getLogPrefix()+"error evaluating Xpath expression activeVersion", e);
		}

		Configuration configuration = getConfiguration().getIbisManager().getConfiguration(configName);

		if (configuration != null) {
			String latestVersion = configuration.getVersion();
			if (getForceReload() || (latestVersion != null && !activeVersion.equals(latestVersion))) {
				IbisContext ibisContext = configuration.getIbisManager().getIbisContext();
				ibisContext.reload(configName);
				return "Reload " + configName + " succeeded";
			}
			return "Reload " + configName + " skipped";
		}
		log.warn("Configuration [" + configName + "] not loaded yet");
		return "Reload " + configName + " skipped"; 
	}

	@Override
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	@Override
	public Configuration getConfiguration() {
		return configuration;
	}

	public void setForceReload(boolean forceReload) {
		this.forceReload  = forceReload;
	}
	public boolean getForceReload() {
		return this.forceReload;
	}
}