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
package org.frankframework.configuration;

import java.util.List;

import jakarta.annotation.Nullable;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;

import org.frankframework.core.Adapter;
import org.frankframework.management.Action;

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

	void setIbisContext(IbisContext ibisContext);

	IbisContext getIbisContext();

	ApplicationContext getApplicationContext();

	void addConfiguration(Configuration configuration);

	List<Configuration> getConfigurations();

	@Nullable
	Configuration getConfiguration(String configurationName);

	/**
	 * Utility function to give commands to Adapters and Receivers
	 */
	void handleAction(Action action, String configurationName, String adapterName, String receiverName, String commandIssuedBy, boolean isAdmin);

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
	Adapter getRegisteredAdapter(String name);

	@Deprecated
	List<Adapter> getRegisteredAdapters();

	ApplicationEventPublisher getApplicationEventPublisher();

}
