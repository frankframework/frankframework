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

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.transaction.PlatformTransactionManager;

import nl.nn.adapterframework.core.Adapter;

/**
 * An IBIS Manager gives various methods for the control of an IBIS instance.
 * 
 * A specific implementation of the interface should be retrieved from the
 * Spring Beans Factory.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 */
public interface IbisManager extends ApplicationEventPublisherAware {

    void setIbisContext(IbisContext ibisContext);

    IbisContext getIbisContext();

    void addConfiguration(Configuration configuration);

    List<Configuration> getConfigurations();

    Configuration getConfiguration(String configurationName);

	public enum IbisAction {
		STOPADAPTER, STARTADAPTER, STOPRECEIVER, STARTRECEIVER, RELOAD, FULLRELOAD, INCTHREADS, DECTHREADS
	}

	/**
	 * Utility function to give commands to Adapters and Receivers
     * @param action
     * @param adapterName
     * @param receiverName
     * @param commandIssuedBy
     */
    void handleAction(IbisAction action, String configurationName, String adapterName, String receiverName, String commandIssuedBy, boolean isAdmin);

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

    public Adapter getRegisteredAdapter(String name);

    public List<Adapter> getRegisteredAdapters();

    /**
     * Get the Spring Platform Transaction Manager, for use by
     * the Web Front End.
     * 
     * @return Instance of the Platform Transaction Manager.
     */
    PlatformTransactionManager getTransactionManager();

    public void dumpStatistics(int action);
    
    public ApplicationEventPublisher getApplicationEventPublisher();

}
