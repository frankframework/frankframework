/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2021, 2022 WeAreFrank!

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

import lombok.Setter;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Performs a reload on database configuration.
 *
 * <p>It is possible to set the name of the configuration with the parameter 'name'.</p>
 * <p>You can dynamically set 'forceReload' attribute with the parameter 'forceReload'.</p>
 *
 * @author	Lars Sinke
 * @author	Niels Meijer
 */
public class ReloadSender extends SenderWithParametersBase {

	private boolean forceReload = false;
	private @Setter IbisManager ibisManager;

	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {

		String configName = null;
		String newVersion = null;
		boolean forceReload = getForceReload();

		ParameterValueList pvl = null;
		try {
			if (paramList != null) {
				pvl = paramList.getValues(message, session);
				if(pvl.get("name") != null)
					configName = pvl.get("name").asStringValue();
				if(pvl.get("forceReload") != null)
					forceReload = pvl.get("forceReload").asBooleanValue(false);
			}
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix()+"Sender ["+getName()+"] caught exception evaluating parameters",e);
		}

		try {
			if(configName == null)
				configName = XmlUtils.evaluateXPathNodeSetFirstElement(message.asString(), "row/field[@name='NAME']");
		} catch (Exception e) {
			throw new SenderException(getLogPrefix()+"error evaluating Xpath expression configName", e);
		}

		try {
			if(!forceReload) {
				newVersion = XmlUtils.evaluateXPathNodeSetFirstElement(message.asString(), "row/field[@name='VERSION']");
			}
		} catch (Exception e) {
			throw new SenderException(getLogPrefix()+"error evaluating Xpath expression activeVersion", e);
		}

		Configuration configuration = ibisManager.getConfiguration(configName);

		if (configuration != null) {
			String currentVersion = configuration.getVersion();
			if (forceReload || (currentVersion != null && !newVersion.equals(currentVersion))) {
				IbisContext ibisContext = ibisManager.getIbisContext();
				ibisContext.reload(configName);
				return new SenderResult("Reload " + configName + " succeeded");
			}
		} else {
			log.warn("Configuration [" + configName + "] not loaded yet");
		}
		return new SenderResult(true, new Message("Reload " + configName + " skipped"), null, "skipped");
	}

	/**
	 * reload the configuration regardless of the version
	 * @ff.default false
	 */
	public void setForceReload(boolean forceReload) {
		this.forceReload  = forceReload;
	}
	public boolean getForceReload() {
		return this.forceReload;
	}
}