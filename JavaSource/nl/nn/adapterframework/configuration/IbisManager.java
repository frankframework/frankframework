/*
   Copyright 2013 Nationale-Nederlanden

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
/*
 * $Log: IbisManager.java,v $
 * Revision 1.6  2011-11-30 13:51:56  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:48  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2007/11/22 08:26:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added shutdown method and javadoc
 *
 * Revision 1.3.2.2  2007/11/15 09:52:46  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add JavaDoc
 *
 * Revision 1.3.2.1  2007/10/25 08:36:58  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add shutdown method for IBIS which shuts down the scheduler too, and which unregisters all EjbJmsConfigurators from the ListenerPortPoller.
 * Unregister JmsListener from ListenerPortPoller during ejbRemove method.
 * Both changes are to facilitate more proper shutdown of the IBIS adapters.
 *
 * Revision 1.3  2007/10/16 09:12:27  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
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
 * An IBIS Manager gives various methods for the control of an IBIS instance.
 * 
 * A specific implementation of the interface should be retrieved from the
 * Spring Beans Factory.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version $Id$
 */
public interface IbisManager {
    public static final String DFLT_CONFIGURATION = "Configuration.xml";
    public static final int DEPLOYMENT_MODE_UNMANAGED = 0;
    public static final int DEPLOYMENT_MODE_EJB = 1;
    public static final String DEPLOYMENT_MODE_UNMANAGED_STRING = "Unmanaged (Legacy)";
    public static final String DEPLOYMENT_MODE_EJB_STRING = "EJB";
    
    /**
     * Get the Configuration, for querying and display of it's contents.
     * 
     * @return IBIS Configuration
     */
    Configuration getConfiguration();
    /**
     * Issue a command/action on the named adapter/receiver.
     * @param action
     * @param adapterName
     * @param receiverName
     * @param commandIssuedBy
     */
    void handleAdapter(String action, String adapterName, String receiverName, String commandIssuedBy);
    /**
     * Start an already configured IBIS instance.
     * 
     * Use {@link loadConfigurationFile} to configure the instance.
     */
    void startIbis();
    /**
     * Shut down the IBIS instance. After execution of this method, the IBIS
     * instance is not useable anymore: it will need to be recreated.
     */
    void shutdownIbis();
    /**
     * Start all adapters of the IBIS instance.
     */
    void startAdapters();
    /**
     * Stop all adapters of the IBIS instance.
     */
    void stopAdapters();
    /**
     * Start the given adapter.
     * 
     * @param adapter Adapter to start.
     */
    void startAdapter(IAdapter adapter);
    /**
     * Stop the given Adapter.
     * 
     * @param adapter Adapter to stop.
     */
    void stopAdapter(IAdapter adapter);

    /**
     * Load the configuration file, thus initializing the IBIS instance.
     * Afterwards, the IBIS is ready to be started.
     * 
     * @param configurationFile
     */
    void loadConfigurationFile(String configurationFile);
    
    /**
     * Get string representing the deployment mode: "Unmanaged" or "EJB".
     * 
     * @return
     */
    String getDeploymentModeString();
    /**
     * Get integer value for the Deployment Mode:
     * <dl>
     * <dt>0</dt><dd>Unmanaged (legacy) deployment mode, also known as Web Deployment mode.</dd>
     * <dt>1</dt><dd>EJB deployment mode</dd>
     * </dl>
     * @return
     */
    int getDeploymentMode();
    /**
     * Get the Spring Platform Transaction Manager, for use by
     * the Web Front End.
     * 
     * @return Instance of the Platform Transaction Manager.
     */
    PlatformTransactionManager getTransactionManager();
}
