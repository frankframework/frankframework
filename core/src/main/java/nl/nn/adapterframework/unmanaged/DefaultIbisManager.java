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
package nl.nn.adapterframework.unmanaged;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;

import nl.nn.adapterframework.cache.IbisCacheManager;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.statistics.HasStatistics.Action;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.RunState;

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
	private List<Configuration> configurations = new ArrayList<>();
	private PlatformTransactionManager transactionManager;
	private ApplicationEventPublisher applicationEventPublisher;

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
		log.info("removing configuration [{}]", configuration);
		configuration.close();

		configurations.remove(configuration);
	}

	/**
	 * Shut down the IBIS instance and clean up.
	 */
	@Override
	public void shutdown() {
		unload((String) null);
		IbisCacheManager.shutdown();
	}

	@Override
	public void handleAction(IbisAction action, String configurationName, String adapterName, String receiverName, String commandIssuedBy, boolean isAdmin) {
		switch (action) {
		case STOPADAPTER:
			if (adapterName.equals("*ALL*")) {
				if (configurationName.equals("*ALL*")) {
					log.info("Stopping all adapters on request of [" + commandIssuedBy+"]");
					for (Configuration configuration : configurations) {
						stopAdapters(configuration);
					}
				} else {
					log.info("Stopping all adapters for configuration [" + configurationName + "] on request of [" + commandIssuedBy+"]");
					stopAdapters(getConfiguration(configurationName));
				}
			} else {
				for (Configuration configuration : configurations) {
					if (configuration.getRegisteredAdapter(adapterName) != null) {
						log.info("Stopping adapter [" + adapterName + "], on request of [" + commandIssuedBy+"]");
						configuration.getRegisteredAdapter(adapterName).stopRunning();
					}
				}
			}
			break;

		case STARTADAPTER:
			if (adapterName.equals("*ALL*")) {
				if (configurationName.equals("*ALL*")) {
					log.info("Starting all adapters on request of [" + commandIssuedBy+"]");
					for (Configuration configuration : configurations) {
						startAdapters(configuration);
					}
				} else {
					log.info("Starting all adapters for configuration [" + configurationName + "] on request of [" + commandIssuedBy+"]");
					startAdapters(getConfiguration(configurationName));
				}
			} else {
				try {
					for (Configuration configuration : configurations) {
						if (configuration.getRegisteredAdapter(adapterName) != null) {
							log.info("Starting adapter [" + adapterName + "] on request of [" + commandIssuedBy+"]");
							configuration.getRegisteredAdapter(adapterName).startRunning();
						}
					}
				} catch (Exception e) {
					log.error("error in execution of command [" + action + "] for adapter [" + adapterName + "]",   e);
					//errors.add("", new ActionError("errors.generic", e.toString()));
				}
			}
			break;

		case STOPRECEIVER:
			for (Configuration configuration : configurations) {
				if (configuration.getRegisteredAdapter(adapterName) != null) {
					Adapter adapter = configuration.getRegisteredAdapter(adapterName);

					Receiver<?> receiver = adapter.getReceiverByName(receiverName);
					RunState receiverRunState = receiver.getRunState();
					switch(receiverRunState) {
						case STOPPING:
						case STOPPED:
							adapter.getMessageKeeper().info(receiver, "already in state [" + receiverRunState + "]");
							break;
						case STARTED:
						case EXCEPTION_STARTING:
						case EXCEPTION_STOPPING:
							receiver.stopRunning();
							log.info("receiver [" + receiverName + "] stopped by webcontrol on request of " + commandIssuedBy);
							break;
						default:
							log.warn("receiver [" + receiverName + "] currently in state [" + receiverRunState + "], ignoring stop() command");
							break;
					}
				}
			}
			break;

		case STARTRECEIVER:
			for (Configuration configuration : configurations) {
				if (configuration.getRegisteredAdapter(adapterName) != null) {
					Adapter adapter = configuration.getRegisteredAdapter(adapterName);

					Receiver<?> receiver = adapter.getReceiverByName(receiverName);
					RunState receiverRunState = receiver.getRunState();
					switch(receiverRunState) {
						case STARTING:
						case STARTED:
							adapter.getMessageKeeper().info(receiver, "already in state [" + receiverRunState + "]");
							break;
						case STOPPED:
							receiver.startRunning();
							log.info("receiver [" + receiverName + "] started by " + commandIssuedBy);
							break;
						default:
							log.warn("receiver [" + receiverName + "] currently in state [" + receiverRunState + "], ignoring start() command");
							break;
					}
				}
			}
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
				log.warn("Full reload not allowed for [" + commandIssuedBy+"]");
			}
			break;

		case INCTHREADS:
			for (Configuration configuration : configurations) {
				if (configuration.getRegisteredAdapter(adapterName) != null) {
					Adapter adapter = configuration.getRegisteredAdapter(adapterName);
					Receiver<?> receiver = adapter.getReceiverByName(receiverName);
					if (receiver.isThreadCountControllable()) {
						receiver.increaseThreadCount();
					}
					log.info("receiver [" + receiverName + "] increased threadcount on request of " + commandIssuedBy);
				}
			}
			break;

		case DECTHREADS:
			for (Configuration configuration : configurations) {
				if (configuration.getRegisteredAdapter(adapterName) != null) {
					Adapter adapter = configuration.getRegisteredAdapter(adapterName);
					Receiver<?> receiver = adapter.getReceiverByName(receiverName);
					if (receiver.isThreadCountControllable()) {
						receiver.decreaseThreadCount();
					}
					log.info("receiver [" + receiverName + "] decreased threadcount on request of " + commandIssuedBy);
				}
			}
			break;


		default:
			throw new NotImplementedException("action ["+action.name()+"] not implemented");
		}
	}

	private void startAdapters(Configuration configuration) {
		log.info("Starting all autostart-configured adapters for configuation " + configuration.getName());
		configuration.getAdapterManager().start();
	}

	private void stopAdapters(Configuration configuration) {
		configuration.dumpStatistics(Action.MARK_FULL);
		log.info("Stopping all adapters for configuation " + configuration.getName());
		configuration.getAdapterManager().stop();
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
		List<Adapter> registeredAdapters = new ArrayList<>();
		for (Configuration configuration : configurations) {
			if(configuration.isActive()) {
				registeredAdapters.addAll(configuration.getRegisteredAdapters());
			}
		}
		return registeredAdapters;
	}

	@Override
	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		log.debug("setting transaction manager to [" + transactionManager + "]");
		this.transactionManager = transactionManager;
	}

	@Override
	public void dumpStatistics(Action action) {
		for (Configuration configuration : configurations) {
			configuration.dumpStatistics(action);
		}
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
