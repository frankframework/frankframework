package nl.nn.adapterframework.util;

import java.io.File;
import java.io.IOException;


import org.apache.log4j.Logger;

import com.ibm.websphere.management.AdminService;
import com.ibm.websphere.management.AdminServiceFactory;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

/**
 * @author Michiel Meeuwissen
 * @since
 */
public class IbmMisc {
    private static final Logger LOG = LogUtil.getLogger(Misc.class);


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
        String appFile =
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
        LOG.debug("configurationResourcesFile [" + appFile + "]");
        return Misc.fileToString(appFile);
    }
}
