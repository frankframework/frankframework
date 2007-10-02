/*
 * Created on 6-sep-07
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.core.IAdapter;

/**
 * @author m00035f
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public interface IbisManager {
    public static final String DFLT_CONFIGURATION = "Configuration.xml";
    public static final int DEPLOYMENT_MODE_UNMANAGED = 0;
    public static final int DEPLOYMENT_MODE_EJB = 1;
    public static final String DEPLOYMENT_MODE_UNMANAGED_STRING = "Unmanaged (Legacy)";
    public static final String DEPLOYMENT_MODE_EJB_STRING = "EJB";
    
    Configuration getConfiguration();
    void handleAdapter(String action, String adapterName, String receiverName, String commandIssuedBy);
    void startIbis();
    void startAdapters();
    void stopAdapters();
    void startAdapter(IAdapter adapter);
    void stopAdapter(IAdapter adapter);

    void loadConfigurationFile(String configurationFile);
    
    String getDeploymentModeString();
    int getDeploymentMode();
}
