/*
   Copyright 2016 Nationale-Nederlanden

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

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.http.RestListenerUtils;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.TimeoutGuardPipe;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;

/**
 * GenerateFlowDiagram.
 * 
 * @author Peter Leeuwenburgh
 * @version $Id$
 */

public class GenerateFlowDiagram extends TimeoutGuardPipe {

	public String doPipeWithTimeoutGuarded(Object input,
			IPipeLineSession session) throws PipeRunException {
		String method = (String) session.get("method");
		if (method.equalsIgnoreCase("GET")) {
			return doGet(session);
		} else {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "illegal value for method [" + method
					+ "], must be 'GET'");
		}
	}

	private String doGet(IPipeLineSession session) throws PipeRunException {
		IbisManager ibisManager = RestListenerUtils
				.retrieveIbisManager(session);
		String uri = (String) session.get("uri");
		if (StringUtils.isEmpty(uri)) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "sessionKey [uri] must be filled");
		}
		String[] split = uri.split("/");
		String adapterName = null;
		if (split.length > 2) {
			adapterName = split[2];
		}
		if (StringUtils.isNotEmpty(adapterName)) {
			adapterName = java.net.URLDecoder.decode(adapterName);
			IAdapter iAdapter = ibisManager.getRegisteredAdapter(adapterName);
			if (iAdapter == null) {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "adapter [" + adapterName + "] not found");
			}
			try {
				return iAdapter.getAdapterConfigurationAsString();
			} catch (ConfigurationException e) {
				throw new PipeRunException(
						this,
						getLogPrefix(session)
								+ "exception on getting adapter configuration as string",
						e);
			}
		} else {
			String result;
			String configurationName = (String) session.get("configuration");
			if (StringUtils.isNotEmpty(configurationName)) {
				configurationName = java.net.URLDecoder
						.decode(configurationName);
			}
			if (StringUtils.isEmpty(configurationName)
					|| configurationName.equalsIgnoreCase("*ALL*")) {
				result = "<configs>";
				for (Configuration configuration : ibisManager
						.getConfigurations()) {
					result = result
							+ XmlUtils.skipXmlDeclaration(configuration
									.getLoadedConfiguration());
				}
				result = result + "</configs>";
			} else {
				Configuration configuration = ibisManager
						.getConfiguration(configurationName);
				if (configuration == null) {
					throw new PipeRunException(this, getLogPrefix(session)
							+ "configuration [" + configurationName
							+ "] not found");
				}
				result = configuration.getLoadedConfiguration();
			}
			return result;
		}
	}
}
