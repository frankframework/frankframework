/*
   Copyright 2018, 2020 Nationale-Nederlanden

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

import javax.servlet.http.HttpServletRequest;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.http.RestListenerUtils;
import nl.nn.adapterframework.pipes.TimeoutGuardPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Base class for pages with configuration tabs.
 * 
 * @author Peter Leeuwenburgh
 */

public abstract class ConfigurationBase extends TimeoutGuardPipe {
	protected static final String CONFIG_ALL = "*ALL*";

	@Override
	public PipeRunResult doPipeWithTimeoutGuarded(Message input, PipeLineSession session) throws PipeRunException {
		String method = (String) session.get("method");
		if (method.equalsIgnoreCase("GET")) {
			return new PipeRunResult(getForward(), doGet(session));
		} else {
			throw new PipeRunException(this,
					getLogPrefix(session) + "Illegal value for method [" + method + "], must be 'GET'");
		}
	}

	/**
	 * When extending this class, copy and extend the doGet method in the new
	 * child class.
	 */
	protected String doGet(PipeLineSession session) throws PipeRunException {
		IbisManager ibisManager = RestListenerUtils.retrieveIbisManager(session);

		String configurationName = null;

		if (configurationName == null) {
			configurationName = retrieveConfigurationName(session);
		}
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

		storeConfiguration(session, configAll, configuration);

		XmlBuilder root = new XmlBuilder("root");
		root.addSubElement(configurationsXml);
		// root.addSubElement(...);

		return root.toXML();
	}

	protected String retrieveConfigurationName(PipeLineSession session) {
		String configurationName = (String) session.get("configuration");
		if (configurationName == null) {
			HttpServletRequest httpServletRequest = (HttpServletRequest) session.get(PipeLineSession.HTTP_REQUEST_KEY);
			configurationName = (String) httpServletRequest.getSession().getAttribute("configurationName");
		}
		return configurationName;
	}

	protected void storeConfiguration(PipeLineSession session, boolean configAll, Configuration configuration) {
		HttpServletRequest httpServletRequest = (HttpServletRequest) session.get(PipeLineSession.HTTP_REQUEST_KEY);
		if (configAll) {
			session.put("configurationName", CONFIG_ALL);
			httpServletRequest.getSession().setAttribute("configurationName", CONFIG_ALL);
			session.put("classLoaderType", null);
			httpServletRequest.getSession().setAttribute("classLoaderType", null);
		} else {
			session.put("configurationName", configuration.getName());
			httpServletRequest.getSession().setAttribute("configurationName", configuration.getName());
			session.put("classLoaderType", configuration.getClassLoaderType());
			httpServletRequest.getSession().setAttribute("classLoaderType", configuration.getClassLoaderType());
		}
	}

	protected XmlBuilder toConfigurationsXml(List<Configuration> configurations) {
		return toConfigurationsXml(configurations, true);
	}

	protected XmlBuilder toConfigurationsXml(List<Configuration> configurations, boolean includeAll) {
		XmlBuilder configurationsXml = new XmlBuilder("configurations");
		if (includeAll) {
			XmlBuilder configurationAllXml = new XmlBuilder("configuration");
			configurationAllXml.setValue(CONFIG_ALL);
			configurationAllXml.addAttribute("nameUC", "0" + Misc.toSortName(CONFIG_ALL));
			configurationsXml.addSubElement(configurationAllXml);
		}
		for (Configuration configuration : configurations) {
			XmlBuilder configurationXml = new XmlBuilder("configuration");
			configurationXml.setValue(configuration.getName());
			configurationXml.addAttribute("nameUC", "1" + Misc.toSortName(configuration.getName()));
			configurationsXml.addSubElement(configurationXml);
		}
		return configurationsXml;
	}

	protected IbisManager retrieveIbisManager() {
		Adapter adapter = getAdapter();
		if (adapter==null) {
			throw new IllegalStateException("Adapter is null");
		}
		Configuration configuration = adapter.getConfiguration();
		if (configuration==null) {
			throw new IllegalStateException("Configuration of Adapter ["+adapter.getName()+"] is null");
		}
		return configuration.getIbisManager();
	}
}