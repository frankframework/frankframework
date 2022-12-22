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

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.statistics.HasStatistics.Action;

/**
 * An IBIS Manager gives various methods for the control of an IBIS instance.
 * 
 * A specific implementation of the interface should be retrieved from the
 * Spring Beans Factory.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 */
public interface IbisManager extends ApplicationEventPublisherAware, ApplicationContextAware {
	public static String ALL_CONFIGS_KEY = "*ALL*";

	void setIbisContext(IbisContext ibisContext);

	IbisContext getIbisContext();

	ApplicationContext getApplicationContext();

	void addConfiguration(Configuration configuration);

	List<Configuration> getConfigurations();

	Configuration getConfiguration(String configurationName);

	public enum IbisAction {
		STOPADAPTER, STARTADAPTER, STOPRECEIVER, STARTRECEIVER, RELOAD, FULLRELOAD, INCTHREADS, DECTHREADS
	}

	/**
	 * Utility function to give commands to Adapters and Receivers
	 * 
	 * @param action
	 * @param adapterName
	 * @param receiverName
	 * @param commandIssuedBy
	 */
	void handleAction(IbisAction action, String configurationName, String adapterName, String receiverName, String commandIssuedBy, boolean isAdmin);

	/**
	 * Unload specified configuration.
	 */
	void unload(String configurationName);

	/**
	 * Shut down the IBIS instance. After execution of this method, the IBIS
	 * instance is not usable anymore: it will need to be recreated.
	 */
	void shutdown();

	@Deprecated
	public Adapter getRegisteredAdapter(String name);

	@Deprecated
	public List<Adapter> getRegisteredAdapters();

	public void dumpStatistics(Action action);

	public ApplicationEventPublisher getApplicationEventPublisher();

}
