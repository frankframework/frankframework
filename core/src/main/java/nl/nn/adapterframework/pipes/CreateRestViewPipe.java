/*
   Copyright 2015-2018 Nationale-Nederlanden

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
package nl.nn.adapterframework.pipes;

import java.util.Hashtable;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.ProcessMetrics;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.webcontrol.ConfigurationServlet;

/**
 * Create a view for {@link nl.nn.adapterframework.http.RestListener}.
 *
 * 
 * @author Peter Leeuwenburgh
 */

public class CreateRestViewPipe extends XsltPipe {
	private static final String CONTENTTYPE = "contentType";
	private static final String SRCPREFIX = "srcPrefix";

	private String contentType = "text/html";

	AppConstants appConstants;

	@Override
	public void configure() throws ConfigurationException {
		ParameterList parameterList = getParameterList();
		if (parameterList==null || parameterList.findParameter(SRCPREFIX) == null) {
			Parameter p = new Parameter();
			p.setName(SRCPREFIX);
			p.setSessionKey(SRCPREFIX);
			addParameter(p);
		}
		appConstants = AppConstants.getInstance(getConfigurationClassLoader());
		super.configure();
	}

	@Override
	public PipeRunResult doPipe(Object input, IPipeLineSession session, MessageOutputStream target) throws PipeRunException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) session.get(IPipeLineSession.HTTP_REQUEST_KEY);
		String requestURL = httpServletRequest.getRequestURL().toString();
		String servletPath = httpServletRequest.getServletPath();
		String uri = StringUtils.substringAfter(requestURL, servletPath);
		int countSrcPrefix = StringUtils.countMatches(uri, "/");
		String srcPrefix = StringUtils.repeat("../", countSrcPrefix);
		session.put(SRCPREFIX, srcPrefix);
		log.debug(getLogPrefix(session) + "stored [" + srcPrefix
				+ "] in pipeLineSession under key [" + SRCPREFIX + "]");

		PipeRunResult prr = super.doPipe(input, session, target);
		String result = (String) prr.getResult();

		log.debug("transforming page [" + result + "] to view");

		String newResult = null;
		ServletContext servletContext = (ServletContext) session.get(IPipeLineSession.SERVLET_CONTEXT_KEY);

		try {
			Map<String,Object> parameters = retrieveParameters(httpServletRequest, servletContext, srcPrefix);
			newResult = XmlUtils.getAdapterSite(result, parameters);
		} catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ " Exception on transforming page to view", e);
		}

		session.put(CONTENTTYPE, getContentType());
		log.debug(getLogPrefix(session) + "stored [" + getContentType()
				+ "] in pipeLineSession under key [" + CONTENTTYPE + "]");

		return new PipeRunResult(getForward(), newResult);
	}

	private Map<String,Object> retrieveParameters(HttpServletRequest httpServletRequest,
			ServletContext servletContext, String srcPrefix)
			throws DomBuilderException {
		String attributeKey = AppConstants.getInstance().getProperty(ConfigurationServlet.KEY_CONTEXT);
		IbisContext ibisContext = (IbisContext) servletContext.getAttribute(attributeKey);
		Map<String,Object> parameters = new Hashtable<String,Object>();
		String requestInfoXml = "<requestInfo>" + "<servletRequest>"
				+ "<serverInfo><![CDATA[" + servletContext.getServerInfo()
				+ "]]></serverInfo>" + "<serverName>"
				+ httpServletRequest.getServerName() + "</serverName>"
				+ "</servletRequest>" + "</requestInfo>";
		parameters.put("requestInfo", XmlUtils.buildNode(requestInfoXml));
		parameters.put("upTime", XmlUtils.buildNode("<upTime>" + (ibisContext==null?"null":ibisContext.getUptime()) + "</upTime>"));
		String machineNameXml = "<machineName>" + Misc.getHostname()
				+ "</machineName>";
		parameters.put("machineName", XmlUtils.buildNode(machineNameXml));
		String fileSystemXml = "<fileSystem>" + "<totalSpace>"
				+ Misc.getFileSystemTotalSpace() + "</totalSpace>"
				+ "<freeSpace>" + Misc.getFileSystemFreeSpace()
				+ "</freeSpace>" + "</fileSystem>";
		parameters.put("fileSystem", XmlUtils.buildNode(fileSystemXml));
		String applicationConstantsXml = appConstants.toXml(true);
		parameters.put("applicationConstants",
				XmlUtils.buildNode(applicationConstantsXml));
		String processMetricsXml = ProcessMetrics.toXml();
		parameters.put("processMetrics", XmlUtils.buildNode(processMetricsXml));
		parameters.put("menuBar",
				XmlUtils.buildNode(retrieveMenuBarParameter(srcPrefix)));
		parameters.put(SRCPREFIX, srcPrefix);

		return parameters;
	}

	private String retrieveMenuBarParameter(String srcPrefix) {
		XmlBuilder menuBar = new XmlBuilder("menuBar");
		XmlBuilder imagelinkMenu = new XmlBuilder("imagelinkMenu");
		imagelinkMenu.addSubElement(createImagelinkElement(srcPrefix,
				"rest/showConfigurationStatus", "configurationStatus",
				"Show Configuration Status"));
		imagelinkMenu.addSubElement(createImagelinkElement(srcPrefix,
				"rest/showConfiguration", "configuration", "Show Configuration"));
		imagelinkMenu.addSubElement(createImagelinkElement(srcPrefix,
				"showLogging.do", "logging", "Show Logging"));
		imagelinkMenu.addSubElement(createImagelinkElement(srcPrefix,
				"sendJmsMessage.do", "jms-message", "Send a message with JMS"));
		if (appConstants.getBoolean("active.ifsa", false)) {
			imagelinkMenu.addSubElement(createImagelinkElement(srcPrefix,
					"rest/testIfsaService", "ifsa-message", "Call an IFSA Service"));
		}
		imagelinkMenu.addSubElement(createImagelinkElement(srcPrefix,
				"browseQueue.do", "browsejms", "Browse a queue with JMS"));
		imagelinkMenu.addSubElement(createImagelinkElement(srcPrefix,
				"rest/testPipeLine", "testPipeLine",
				"Test a PipeLine of an Adapter"));
		imagelinkMenu.addSubElement(createImagelinkElement(srcPrefix,
				"testService.do", "service", "Test a ServiceListener"));
		imagelinkMenu.addSubElement(createImagelinkElement(srcPrefix,
				"rest/webservices", "wsdl", "Webservices"));
		imagelinkMenu
				.addSubElement(createImagelinkElement(srcPrefix,
						"showSchedulerStatus.do", "scheduler",
						"Show Scheduler status"));
		imagelinkMenu.addSubElement(createImagelinkElement(srcPrefix,
				"rest/showEnvironmentVariables", "properties",
				"Show Environment Variables"));
		imagelinkMenu.addSubElement(createImagelinkElement(srcPrefix,
				"executeJdbcQuery.do", "execquery", "Execute a Jdbc Query"));
		imagelinkMenu.addSubElement(createImagelinkElement(srcPrefix,
				"browseJdbcTable.do", "browsetable", "Browse a Jdbc Table"));
		imagelinkMenu.addSubElement(createImagelinkElement(srcPrefix,
				"DumpIbisConsole", "dump", "Dump Ibis Console"));
		imagelinkMenu.addSubElement(createImagelinkElement(srcPrefix,
				"showSecurityItems.do", "security", "Show Security Items"));
		if (MonitorManager.getInstance().isEnabled()) {
			imagelinkMenu.addSubElement(createImagelinkElement(srcPrefix,
					"showMonitors.do", "monitoring", "Show Monitors"));
		}
		imagelinkMenu.addSubElement(createImagelinkElement(srcPrefix,
				"showIbisstoreSummary.do", "showsummary",
				"Show Ibisstore Summary"));
		imagelinkMenu.addSubElement(createImagelinkElement(srcPrefix, "larva",
				"larva", "Larva Test Tool"));
		imagelinkMenu.addSubElement(createImagelinkElement(srcPrefix,
				"testtool", "ladybug", "Ladybug Test Tool"));
		imagelinkMenu.addSubElement(createImagelinkElement(srcPrefix,
				"javascript:void(0)", "info", "Information"));
		if (appConstants.getBoolean("console.active", false)) {
			imagelinkMenu.addSubElement(createImagelinkElement(srcPrefix,
					"iaf/gui", "theme", "Try our new GUI 3.0 and leave feedback!"));
		}
		menuBar.addSubElement(imagelinkMenu);
		return menuBar.toXML();
	}

	private XmlBuilder createImagelinkElement(String srcPrefix, String href,
			String type, String alt) {
		XmlBuilder imagelink = new XmlBuilder("imagelink");
		if (StringUtils.startsWithIgnoreCase(href, "javascript:")
				|| StringUtils.startsWithIgnoreCase(href, "?")) {
			imagelink.addAttribute("href", href);
		} else {
			imagelink.addAttribute("href", srcPrefix + href);
		}
		imagelink.addAttribute("type", type);
		imagelink.addAttribute("alt", alt);
		return imagelink;
	}

	@IbisDoc({"content type of the servlet response", "text/html"})
	public void setContentType(String string) {
		contentType = string;
	}

	public String getContentType() {
		return contentType;
	}
}