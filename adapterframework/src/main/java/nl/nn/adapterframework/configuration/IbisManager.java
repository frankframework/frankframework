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
