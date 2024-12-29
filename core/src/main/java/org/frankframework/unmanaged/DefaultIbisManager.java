/*
   Copyright 2013, 2016, 2019 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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
package org.frankframework.unmanaged;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.Logger;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.RunState;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.Assert;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.cache.IbisCacheManager;
import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.IbisContext;
import org.frankframework.configuration.IbisManager;
import org.frankframework.core.Adapter;
import org.frankframework.management.Action;
import org.frankframework.receivers.Receiver;

/**
 * Implementation of IbisManager which does not use EJB for
 * managing IBIS Adapters.
 *
 * @author  Tim van der Leeuw
 * @since   4.8
 */
public class DefaultIbisManager implements IbisManager {
	protected Logger log = LogUtil.getLogger(this);
	protected Logger secLog = LogUtil.getLogger("SEC");

	private IbisContext ibisContext;
	private final List<Configuration> configurations = new ArrayList<>();
	private ApplicationEventPublisher applicationEventPublisher;
	private @Getter @Setter ApplicationContext applicationContext;

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
		return Collections.unmodifiableList(configurations);
	}

	@Override
	@Nullable
	public Configuration getConfiguration(String configurationName) {
		for (Configuration configuration : configurations) {
			if (configurationName.equals(configuration.getName())) {
				return configuration;
			}
		}
		return null;
	}

	/**
	 * Stop and remove the Configuration
	 */
	@Override
	public void unload(String configurationName) {
		if (configurationName == null) {
			while (!configurations.isEmpty()) {
				remove(configurations.get(0));
			}
		} else {
			remove(getConfiguration(configurationName));
		}
	}

	private void remove(Configuration configuration) {
		Assert.notNull(configuration, "no configuration provided");
		log.info("removing configuration [{}]", configuration);
		configuration.close();

		configurations.remove(configuration);
	}

	/**
	 * Shut down the IBIS instance and clean up.
	 */
	@Override
	public void shutdown() {
		unload(null);
		IbisCacheManager.shutdown();
	}

	@Override
	public void handleAction(Action action, String configurationName, String adapterName, String receiverName, String commandIssuedBy, boolean isAdmin) {
		switch (action) {
		case STOPADAPTER:
			Assert.notNull(adapterName, "no adapterName provided");
			Assert.notNull(configurationName, "no configurationName provided");

			if (true || adapterName.equals(BusMessageUtils.ALL_CONFIGS_KEY)) {
				if (configurationName.equals(BusMessageUtils.ALL_CONFIGS_KEY)) {
					log.info("Stopping all adapters on request of [{}]", commandIssuedBy);
					for (Configuration configuration : configurations) {
						stopAdapters(configuration);
					}
				} else {
					log.info("Stopping all adapters for configuration [{}] on request of [{}]", configurationName, commandIssuedBy);
					stopAdapters(getConfiguration(configurationName));
				}
			} else {
				Configuration configuration = getConfiguration(configurationName);
				Assert.notNull(configuration, ()->"configuration ["+configuration+"] not found");
				Adapter adapter = configuration.getRegisteredAdapter(adapterName);
				Assert.notNull(adapter, ()->"adapter ["+adapterName+"] not found");

				log.info("Stopping adapter [{}], on request of [{}]", adapterName, commandIssuedBy);
				configuration.getRegisteredAdapter(adapterName).stop();
			}
			break;

		case STARTADAPTER:
			Assert.notNull(adapterName, "no adapterName provided");
			Assert.notNull(configurationName, "no configurationName provided");

			if (true || adapterName.equals(BusMessageUtils.ALL_CONFIGS_KEY)) {
				if (configurationName.equals(BusMessageUtils.ALL_CONFIGS_KEY)) {
					log.info("Starting all adapters on request of [{}]", commandIssuedBy);
					for (Configuration configuration : configurations) {
						startAdapters(configuration);
					}
				} else {
					log.info("Starting all adapters for configuration [{}] on request of [{}]", configurationName, commandIssuedBy);
					startAdapters(getConfiguration(configurationName));
				}
			} else {
				try {
					Configuration configuration = getConfiguration(configurationName);
					Assert.notNull(configuration, ()->"configuration ["+configuration+"] not found");
					Adapter adapter = configuration.getRegisteredAdapter(adapterName);
					Assert.notNull(adapter, ()->"adapter ["+adapterName+"] not found");

					log.info("Starting adapter [{}] on request of [{}]", adapterName, commandIssuedBy);
					configuration.getRegisteredAdapter(adapterName).start();
				} catch (Exception e) {
					log.error("error in execution of command [{}] for adapter [{}]", action, adapterName, e);
				}
			}
			break;

		case STOPRECEIVER:
			stopReceiver(configurationName, adapterName, receiverName, commandIssuedBy);
			break;

		case STARTRECEIVER:
			startReceiver(configurationName, adapterName, receiverName, commandIssuedBy);
			break;

		case RELOAD:
			String msg = "Reload configuration [" + configurationName + "] on request of [" + commandIssuedBy+"]";
			log.info(msg);
			secLog.info(msg);
			ibisContext.reload(configurationName);
			break;

		case FULLRELOAD:
			if (isAdmin) {
				String msg2 = "Full reload on request of [" + commandIssuedBy+"]";
				log.info(msg2);
				secLog.info(msg2);
				ibisContext.fullReload();
			} else {
				log.warn("Full reload not allowed for [{}]", commandIssuedBy);
			}
			break;

		case INCTHREADS:
			try {
				Adapter adapter = getAdapterByName(configurationName, adapterName);

				Assert.notNull(receiverName, "no receiverName provided");
				Receiver<?> receiver = adapter.getReceiverByName(receiverName);
				Assert.notNull(receiver, ()->"receiver ["+receiverName+"] not found");
				if (receiver.isThreadCountControllable()) {
					receiver.increaseThreadCount();
				}
				log.info("receiver [{}] increased threadcount on request of [{}]", receiverName, commandIssuedBy);
			} catch (Exception e) {
				log.error("error increasing threadcount for receiver [{}]", receiverName, e);
			}
			break;

		case DECTHREADS:
			try {
				Adapter adapter = getAdapterByName(configurationName, adapterName);

				Assert.notNull(receiverName, "no receiverName provided");
				Receiver<?> receiver = adapter.getReceiverByName(receiverName);
				Assert.notNull(receiver, ()->"receiver ["+receiverName+"] not found");
				if (receiver.isThreadCountControllable()) {
					receiver.decreaseThreadCount();
				}
				log.info("receiver [{}] decreased threadcount on request of [{}]", receiverName, commandIssuedBy);
			} catch (Exception e) {
				log.error("error decreasing threadcount for receiver [{}]", receiverName, e);
			}
			break;


		default:
			throw new NotImplementedException("action ["+action.name()+"] not implemented");
		}
	}

	@Nonnull
	private Adapter getAdapterByName(String configurationName, String adapterName) {
		Assert.notNull(configurationName, "no configurationName provided");
		Configuration configuration = getConfiguration(configurationName);
		if(configuration == null) {
			throw new IllegalArgumentException("configuration ["+configuration+"] not found");
		}

		Assert.notNull(adapterName, "no adapterName provided");
		Adapter adapter = configuration.getRegisteredAdapter(adapterName);
		if(adapter == null) {
			throw new IllegalArgumentException("adapter ["+adapterName+"] not found");
		}

		return adapter;
	}

	private void stopReceiver(String configurationName, String adapterName, String receiverName, String commandIssuedBy) {
		Adapter adapter = getAdapterByName(configurationName, adapterName);

		Assert.notNull(receiverName, "no receiverName provided");
		Receiver<?> receiver = adapter.getReceiverByName(receiverName);
		Assert.notNull(receiver, ()->"receiver ["+receiverName+"] not found");

		RunState receiverRunState = receiver.getRunState();
		switch(receiverRunState) {
			case STOPPING:
			case STOPPED:
				adapter.getMessageKeeper().info(receiver, "already in state [" + receiverRunState + "]");
				break;
			case STARTED:
			case EXCEPTION_STARTING:
			case EXCEPTION_STOPPING:
				receiver.stop();
				log.info("receiver [{}] stopped by webcontrol on request of [{}]", receiverName, commandIssuedBy);
				break;
			default:
				log.warn("receiver [{}] currently in state [{}], ignoring stop() command", receiverName, receiverRunState);
				break;
		}
	}

	private void startReceiver(String configurationName, String adapterName, String receiverName, String commandIssuedBy) {
		Adapter adapter = getAdapterByName(configurationName, adapterName);

		Assert.notNull(receiverName, "no receiverName provided");
		Receiver<?> receiver = adapter.getReceiverByName(receiverName);
		Assert.notNull(receiver, ()->"receiver ["+receiverName+"] not found");

		RunState receiverRunState = receiver.getRunState();
		switch(receiverRunState) {
			case STARTING:
			case STARTED:
				adapter.getMessageKeeper().info(receiver, "already in state [" + receiverRunState + "]");
				break;
			case STOPPED:
				receiver.start();
				log.info("receiver [{}] started by [{}]", receiverName, commandIssuedBy);
				break;
			default:
				log.warn("receiver [{}] currently in state [{}], ignoring start() command", receiverName, receiverRunState);
				break;
		}
	}

	private void startAdapters(Configuration configuration) {
		Assert.notNull(configuration, "no configuration provided");
		log.info("Starting all autostart-configured adapters for configuation [{}]", configuration::getName);
//		configuration.getAdapterManager().start();
		configuration.start();
	}

	private void stopAdapters(Configuration configuration) {
		Assert.notNull(configuration, "no configuration provided");
		log.info("Stopping all adapters for configuation [{}]", configuration::getName);
//		configuration.getAdapterManager().stop();
		configuration.stop();
	}

	@Override
	@Deprecated
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
	@Deprecated
	public List<Adapter> getRegisteredAdapters() {
		List<Adapter> registeredAdapters = new ArrayList<>();
		for (Configuration configuration : configurations) {
			if(configuration.isActive()) {
				registeredAdapters.addAll(configuration.getRegisteredAdapters());
			}
		}
		return registeredAdapters;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher=applicationEventPublisher;
	}

	@Override
	public ApplicationEventPublisher getApplicationEventPublisher() {
		return applicationEventPublisher;
	}
}
