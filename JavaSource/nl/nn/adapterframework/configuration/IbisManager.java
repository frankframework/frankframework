/*
 * $Log: IbisManager.java,v $
 * Revision 1.3  2007-10-16 09:12:27  europe\M00035F
 * Merge with changes from EJB branch in preparation for creating new EJB brance
 *
 * Revision 1.1.2.5  2007/10/15 09:51:57  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add back transaction-management to BrowseExecute action
 *
 * Revision 1.1.2.4  2007/10/10 14:30:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.2  2007/10/09 15:29:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.core.IAdapter;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
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
    PlatformTransactionManager getTransactionManager();
}
