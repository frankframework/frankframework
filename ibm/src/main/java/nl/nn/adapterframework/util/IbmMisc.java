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
import java.util.Hashtable;
import java.util.Map;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;

import com.ibm.websphere.management.AdminService;
import com.ibm.websphere.management.AdminServiceFactory;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * @author Michiel Meeuwissen
 * @since 5.0.29
 */
public class IbmMisc {
    private static final Logger LOG = LogUtil.getLogger(Misc.class);
	public static final String GETCONNPOOLPROP_XSLT = "xml/xsl/getConnectionPoolProperties.xsl";
	public static final String GETJMSDEST_XSLT = "xml/xsl/getJmsDestinations.xsl";

    public static String getApplicationDeploymentDescriptorPath() throws IOException {

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
        return appPath;
    }

    public static String getConfigurationResources() throws IOException {
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
        LOG.debug("configurationResourcesFile [" + crFile + "]");
        return Misc.fileToString(crFile);
    }

    public static String getConfigurationServer() throws IOException {
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
        LOG.debug("configurationServerFile [" + csFile + "]");
        return Misc.fileToString(csFile);
    }

	public static String getConnectionPoolProperties(String confResString,
			String providerType, String jndiName) throws IOException,
			DomBuilderException, TransformerException {
		// providerType: 'JDBC' or 'JMS'
		URL url = ClassUtils
				.getResourceURL(IbmMisc.class, GETCONNPOOLPROP_XSLT);
		if (url == null) {
			throw new IOException("cannot find resource ["
					+ GETCONNPOOLPROP_XSLT + "]");
		}
		Transformer t = XmlUtils.createTransformer(url, true);
		Map parameters = new Hashtable();
		parameters.put("providerType", providerType);
		parameters.put("jndiName", jndiName);
		XmlUtils.setTransformerParameters(t, parameters);
		return XmlUtils.transformXml(t, confResString);
	}

	public static String getJmsDestinations(String confResString)
			throws IOException, DomBuilderException, TransformerException {
		URL url = ClassUtils.getResourceURL(Misc.class, GETJMSDEST_XSLT);
		if (url == null) {
			throw new IOException(
					"cannot find resource [" + GETJMSDEST_XSLT + "]");
		}
		Transformer t = XmlUtils.createTransformer(url, true);
		return XmlUtils.transformXml(t, confResString);
	}
}
