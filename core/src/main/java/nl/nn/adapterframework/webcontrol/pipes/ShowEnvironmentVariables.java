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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.http.HttpUtils;
import nl.nn.adapterframework.logging.IbisMaskingLayout;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Shows the environment variables.
 * 
 * @author Peter Leeuwenburgh
 */

public class ShowEnvironmentVariables extends ConfigurationBase {
	protected Logger secLog = LogUtil.getLogger("SEC");

	@Override
	public PipeRunResult doPipeWithTimeoutGuarded(Message input, PipeLineSession session) throws PipeRunException {
		String method = (String) session.get("method");
		if (method.equalsIgnoreCase("GET")) {
			return new PipeRunResult(getSuccessForward(), doGet(session));
		} else if (method.equalsIgnoreCase("POST")) {
			return new PipeRunResult(getSuccessForward(), doPost(session));
		} else {
			throw new PipeRunException(this,
					getLogPrefix(session) + "Illegal value for method [" + method + "], must be 'GET'");
		}
	}

	@Override
	protected String doGet(PipeLineSession session) throws PipeRunException {
		return retrieveFormInput(session, false);
	}

	private String doPost(PipeLineSession session) throws PipeRunException {
		Object formLogIntermediaryResultsObject = session.get("logIntermediaryResults");
		boolean formLogIntermediaryResults = ("on".equals((String) formLogIntermediaryResultsObject) ? true : false);
		String formLogLevel = (String) session.get("logLevel");
		Object formLengthLogRecordsObject = session.get("lengthLogRecords");
		int formLengthLogRecords = (formLengthLogRecordsObject != null
				? Integer.parseInt((String) formLengthLogRecordsObject) : -1);

		HttpServletRequest httpServletRequest = (HttpServletRequest) session.get(PipeLineSession.HTTP_REQUEST_KEY);
		String commandIssuedBy = HttpUtils.getCommandIssuedBy(httpServletRequest);

		String msg = "LogLevel changed from [" + retrieveLogLevel() + "] to [" + formLogLevel
				+ "], logIntermediaryResults from [" + retrieveLogIntermediaryResults() + "] to [" + ""
				+ formLogIntermediaryResults + "]  and logMaxMessageLength from [" + retrieveLengthLogRecords()
				+ "] to [" + "" + formLengthLogRecords + "] by" + commandIssuedBy;
		log.warn(msg);
		secLog.info(msg);

		IbisMaskingLayout.setMaxLength(formLengthLogRecords);

		AppConstants.getInstance().setProperty("log.logIntermediaryResults",
				Boolean.toString(formLogIntermediaryResults));
		Configurator.setLevel(LogUtil.getRootLogger().getName(), Level.toLevel(formLogLevel));

		return retrieveFormInput(session, true);
	}

	private String retrieveFormInput(PipeLineSession session, boolean updated) {
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
		
		if (configAll) {
			configuration = ibisManager.getConfigurations().get(0);
			configAll = false;
		}

		List<Configuration> allConfigurations = ibisManager.getConfigurations();
		XmlBuilder configurationsXml = toConfigurationsXml(allConfigurations, false);

		XmlBuilder dynamicParametersXml = new XmlBuilder("dynamicParameters");
		dynamicParametersXml.addAttribute("logLevel", retrieveLogLevel());
		dynamicParametersXml.addAttribute("logIntermediaryResults", retrieveLogIntermediaryResults());
		dynamicParametersXml.addAttribute("lengthLogRecords", retrieveLengthLogRecords());

		XmlBuilder environmentVariablesXml = new XmlBuilder("environmentVariables");
		List<String> propsToHide = new ArrayList<String>();

		if(configuration.getClassLoader() != null) {
			String propertiesHideString = AppConstants.getInstance(configuration.getClassLoader())
					.getString("properties.hide", null);
			if (propertiesHideString != null) {
				propsToHide.addAll(Arrays.asList(propertiesHideString.split("[,\\s]+")));
			}

			addPropertiesToXmlBuilder(environmentVariablesXml, AppConstants.getInstance(configuration.getClassLoader()),
					"Application Constants", propsToHide, true);
		}
		addPropertiesToXmlBuilder(environmentVariablesXml, System.getProperties(), "System Properties", propsToHide);

		try {
			addPropertiesToXmlBuilder(environmentVariablesXml, Misc.getEnvironmentVariables(), "Environment Variables");
		} catch (Throwable t) {
			log.warn("caught Throwable while getting EnvironmentVariables", t);
		}

		storeConfiguration(session, configAll, configuration);

		XmlBuilder root = new XmlBuilder("root");
		if (updated) {
			root.addAttribute("message", "Successfully updated dynamic parameters");
		}
		root.addSubElement(configurationsXml);
		root.addSubElement(dynamicParametersXml);
		root.addSubElement(environmentVariablesXml);

		return root.toXML();
	}

	private String retrieveLogLevel() {
		return LogUtil.getRootLogger().getLevel().toString();
	}

	private boolean retrieveLogIntermediaryResults() {
		boolean logIntermediaryResults = false;
		if (AppConstants.getInstance().getResolvedProperty("log.logIntermediaryResults") != null && AppConstants
				.getInstance().getResolvedProperty("log.logIntermediaryResults").equalsIgnoreCase("true")) {
			logIntermediaryResults = true;
		}
		return logIntermediaryResults;
	}

	private int retrieveLengthLogRecords() {
		return IbisMaskingLayout.getMaxLength();
	}

	private void addPropertiesToXmlBuilder(XmlBuilder container, Properties props, String setName,
			List<String> propsToHide) {
		addPropertiesToXmlBuilder(container, props, setName, propsToHide, false);
	}

	private void addPropertiesToXmlBuilder(XmlBuilder container, Properties props, String setName) {
		addPropertiesToXmlBuilder(container, props, setName, null);
	}

	private void addPropertiesToXmlBuilder(XmlBuilder container, Properties props, String setName, List<String> propsToHide, boolean skipResolve) {
		XmlBuilder propertySet = new XmlBuilder("propertySet");
		propertySet.addAttribute("name", setName);
		container.addSubElement(propertySet);

		if (props==null) {
			log.warn("properties for setName ["+setName+"] are null");
			return;
		}
		Enumeration<Object> enumeration = props.keys();
		while (enumeration.hasMoreElements()) {
			String propName = (String) enumeration.nextElement();
			XmlBuilder property = new XmlBuilder("property");
			property.addAttribute("name", XmlUtils.encodeCdataString(propName));
			String propValue;
			if (skipResolve && props instanceof AppConstants) {
				propValue = ((AppConstants) props).getUnresolvedProperty(propName);
			} else {
				propValue = props.getProperty(propName);
			}
			if (propsToHide != null && propsToHide.contains(propName)) {
				propValue = Misc.hide(propValue);
			}
			property.setCdataValue(XmlUtils.encodeCdataString(propValue));
			propertySet.addSubElement(property);
		}

	}
}