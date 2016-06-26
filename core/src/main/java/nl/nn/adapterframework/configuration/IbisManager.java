/*
   Copyright 2013, 2016 Nationale-Nederlanden

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

import java.util.List;

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
 */
public interface IbisManager {

    void setIbisContext(IbisContext ibisContext);

    IbisContext getIbisContext();

    void addConfiguration(Configuration configuration);

    List<Configuration> getConfigurations();

    Configuration getConfiguration(String configurationName);

    /**
     * Issue a command/action on the named adapter/receiver.
     * @param action
     * @param adapterName
     * @param receiverName
     * @param commandIssuedBy
     */
    void handleAdapter(String action, String configurationName, String adapterName, String receiverName, String commandIssuedBy, boolean isAdmin);
    /**
     * Start an already configured Configuration
     */
    void startConfiguration(Configuration configuration);
    /**
     * Unload specified configuration.
     */
    void unload(String configurationName);
    /**
     * Shut down the IBIS instance. After execution of this method, the IBIS
     * instance is not useable anymore: it will need to be recreated.
     */
    void shutdown();
    /**
     * Start all adapters of configuration.
     */
    void startAdapters(Configuration configuration);
    /**
     * Stop all adapters of the IBIS instance.
     */
    void stopAdapters();
    /**
     * Stop all adapters of configuration.
     */
    void stopAdapters(Configuration configuration);
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

    public IAdapter getRegisteredAdapter(String name);

    public List<String> getSortedStartedAdapterNames();

    public List<IAdapter> getRegisteredAdapters();

    /**
     * Get the Spring Platform Transaction Manager, for use by
     * the Web Front End.
     * 
     * @return Instance of the Platform Transaction Manager.
     */
    PlatformTransactionManager getTransactionManager();

    public void dumpStatistics(int action);

}
