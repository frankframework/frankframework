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
package nl.nn.adapterframework.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import com.ibm.websphere.management.AdminService;
import com.ibm.websphere.management.AdminServiceFactory;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * @author Michiel Meeuwissen
 * @since 5.0.29
 */
public class IbmMisc {
	private static final Logger LOG = LogUtil.getLogger(IbmMisc.class);
	public static final String GETCONNPOOLPROP_XSLT = "xml/xsl/getConnectionPoolProperties.xsl";
	public static final String GETJMSDEST_XSLT = "xml/xsl/getJmsDestinations.xsl";

	// Used in iaf-core Misc class
	public static String getApplicationDeploymentDescriptorPath() {
		final String appName = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getJ2EEName().getApplication();
		final AdminService adminService = AdminServiceFactory.getAdminService();
		final String cellName = adminService.getCellName();
		String appPath =
				System.getProperty("user.install.root")
						+ File.separator
						+ "config"
						+ File.separator
						+ "cells"
						+ File.separator
						+ cellName
						+ File.separator
						+ "applications"
						+ File.separator
						+ appName
						+ ".ear"
						+ File.separator
						+ "deployments"
						+ File.separator
						+ appName
						+ File.separator
						+ "META-INF";
		LOG.debug("applicationDeploymentDescriptorPath [" + appPath + "]");
		return appPath;
	}

	// Used in iaf-core Misc class
	public static String getConfigurationResourcePath() {
		final AdminService adminService = AdminServiceFactory.getAdminService();
		final String cellName = adminService.getCellName();
		final String nodeName = adminService.getNodeName();
		final String processName = adminService.getProcessName();
		String crFile =
				System.getProperty("user.install.root")
						+ File.separator
						+ "config"
						+ File.separator
						+ "cells"
						+ File.separator
						+ cellName
						+ File.separator
						+ "nodes"
						+ File.separator
						+ nodeName
						+ File.separator
						+ "servers"
						+ File.separator
						+ processName
						+ File.separator
						+ "resources.xml";
		LOG.debug("configurationResourcesPath [" + crFile + "]");
		return crFile;
	}

	// Used in iaf-core Misc class
	public static String getConfigurationServerPath() {
		final AdminService adminService = AdminServiceFactory.getAdminService();
		final String cellName = adminService.getCellName();
		final String nodeName = adminService.getNodeName();
		final String processName = adminService.getProcessName();
		String csFile =
				System.getProperty("user.install.root")
						+ File.separator
						+ "config"
						+ File.separator
						+ "cells"
						+ File.separator
						+ cellName
						+ File.separator
						+ "nodes"
						+ File.separator
						+ nodeName
						+ File.separator
						+ "servers"
						+ File.separator
						+ processName
						+ File.separator
						+ "server.xml";
		LOG.debug("configurationServerPath [" + csFile + "]");
		return csFile;
	}

	public static String getConnectionPoolProperties(String confResString, String providerType, String jndiName) throws IOException, TransformerException, SAXException {
		// providerType: 'JDBC' or 'JMS'
		URL url = ClassLoaderUtils.getResourceURL(GETCONNPOOLPROP_XSLT);
		if (url == null) {
			throw new IOException("cannot find resource ["
					+ GETCONNPOOLPROP_XSLT + "]");
		}
		Transformer t = XmlUtils.createTransformer(url, 2);
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("providerType", providerType);
		parameters.put("jndiName", jndiName);
		XmlUtils.setTransformerParameters(t, parameters);
		String connectionPoolProperties = XmlUtils.transformXml(t, confResString);
		if (LOG.isDebugEnabled()) {
			LOG.debug("connectionPoolProperties ["
					+ chomp(connectionPoolProperties, 100, true) + "]");
		}
		return connectionPoolProperties;
	}

	public static String getJmsDestinations(String confResString) throws IOException, TransformerException, SAXException {
		URL url = ClassLoaderUtils.getResourceURL(GETJMSDEST_XSLT);
		if (url == null) {
			throw new IOException("cannot find resource [" + GETJMSDEST_XSLT + "]");
		}
		Transformer t = XmlUtils.createTransformer(url, 2);
		String jmsDestinations = XmlUtils.transformXml(t, confResString);
		if (LOG.isDebugEnabled()) LOG.debug("jmsDestinations [" + chomp(jmsDestinations, 100, true) + "]");

		return jmsDestinations;
	}

	private static String chomp(String string, int length, boolean isEmpty) {
		if (isEmpty && StringUtils.isEmpty(string)) {
			return "[#EMPTY#]";
		} else {
			if (length > 0 && string.length() > length) {
				String chompedString;
				chompedString = string.substring(0, length) + "...("
						+ (string.length() - length) + " characters more)";
				return chompedString;
			}
		}
		return string;
	}
}
