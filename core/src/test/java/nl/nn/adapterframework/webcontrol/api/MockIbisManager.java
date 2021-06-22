/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.webcontrol.api;

import java.util.ArrayList;
import java.util.List;

import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;

public class MockIbisManager extends Mockito implements IbisManager {
	private IbisContext ibisContext = spy(new IbisContext());
	private List<Configuration> configurations = new ArrayList<Configuration>();

	public MockIbisManager() {
	}

	@Override
	public void setIbisContext(IbisContext ibisContext) {
		this.ibisContext = ibisContext;
	}

	@Override
	public IbisContext getIbisContext() {
		return ibisContext;
	}

	@Override
	public void addConfiguration(Configuration configuration) {
		configurations.add(configuration);
	}

	@Override
	public List<Configuration> getConfigurations() {
		return configurations;
	}

	@Override
	public Configuration getConfiguration(String configurationName) {
		for (Configuration configuration : configurations) {
			if (configurationName.equals(configuration.getName())) {
				return configuration;
			}
		}
		return null;
	}

	@Override
	public void handleAction(IbisAction action, String configurationName, String adapterName, String receiverName, String commandIssuedBy, boolean isAdmin) {
		// Ignore all actions
	}

	@Override
	public void startConfiguration(Configuration configuration) {
		// ignore start
	}

	@Override
	public void unload(String configurationName) {
		// ignore unload
	}

	@Override
	public void shutdown() {
		// ignore shutdown
	}

	@Override
	public Adapter getRegisteredAdapter(String name) {
		List<Adapter> adapters = getRegisteredAdapters();
		for (Adapter adapter : adapters) {
			if (name.equals(adapter.getName())) {
				return adapter;
			}
		}
		return null;
	}

	@Override
	public List<Adapter> getRegisteredAdapters() {
		List<Adapter> registeredAdapters = new ArrayList<Adapter>();
		for (Configuration configuration : configurations) {
			registeredAdapters.addAll(configuration.getAdapterManager().getAdapterList());
		}
		return registeredAdapters;
	}

	@Override
	public PlatformTransactionManager getTransactionManager() {
		return null;
	}

	@Override
	public void dumpStatistics(int action) {
		//There are no statistics to dump
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		//Unused Spring event publisher
	}

	@Override
	public ApplicationEventPublisher getApplicationEventPublisher() {
		return null;
	}
}
