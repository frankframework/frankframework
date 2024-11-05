/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2021-2024 WeAreFrank!

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
package org.frankframework.senders;

import jakarta.annotation.Nonnull;
import lombok.Setter;
import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.IbisContext;
import org.frankframework.configuration.IbisManager;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.XmlUtils;

/**
 * Performs a reload on database configuration.
 *
 * <p>It is possible to set the name of the configuration with the parameter 'name'.</p>
 * <p>You can dynamically set 'forceReload' attribute with the parameter 'forceReload'.</p>
 *
 * @author	Lars Sinke
 * @author	Niels Meijer
 */
public class ReloadSender extends AbstractSenderWithParameters {

	private boolean forceReload = false;
	private @Setter IbisManager ibisManager;

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {

		String configName = null;
		String newVersion = null;
		boolean forceReload = getForceReload();

		ParameterValueList pvl = getParameterValueList(message, session);
		if(pvl != null) {
			if(pvl.contains("name"))
				configName = pvl.get("name").asStringValue();
			if(pvl.contains("forceReload"))
				forceReload = pvl.get("forceReload").asBooleanValue(false);
		}

		try {
			if(configName == null)
				configName = XmlUtils.evaluateXPathNodeSetFirstElement(message.asString(), "row/field[@name='NAME']");
		} catch (Exception e) {
			throw new SenderException("error evaluating Xpath expression configName", e);
		}

		try {
			if(!forceReload) {
				newVersion = XmlUtils.evaluateXPathNodeSetFirstElement(message.asString(), "row/field[@name='VERSION']");
			}
		} catch (Exception e) {
			throw new SenderException("error evaluating Xpath expression activeVersion", e);
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
			log.warn("Configuration [{}] not loaded yet", configName);
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
