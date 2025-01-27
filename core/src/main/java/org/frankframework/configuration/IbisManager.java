/*
   Copyright 2013-2019 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.cache.IbisCacheManager;
import org.frankframework.core.Adapter;
import org.frankframework.management.Action;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.receivers.Receiver;
import org.frankframework.util.LogUtil;

/**
 * Implementation of IbisManager which does not use EJB for
 * managing IBIS Adapters.
 *
 * @author  Tim van der Leeuw
 * @since   4.8
 */
@Log4j2
public class IbisManager implements ApplicationContextAware {
	private Logger secLog = LogUtil.getLogger("SEC");

	private IbisContext ibisContext;
	private final List<Configuration> configurations = new ArrayList<>();
	private @Getter @Setter ApplicationContext applicationContext; // Only here for the DatabaseClassLoader to create a FixedQuerySender bean.

	public void setIbisContext(IbisContext ibisContext) {
		this.ibisContext = ibisContext;
	}

	public IbisContext getIbisContext() {
		return ibisContext;
	}

	public void addConfiguration(Configuration configuration) {
		configurations.add(configuration);
	}

	public List<Configuration> getConfigurations() {
		return Collections.unmodifiableList(configurations);
	}

	@Nullable
	public Configuration getConfiguration(String configurationName) {
        return configurations.stream()
                .filter(configuration -> configurationName.equals(configuration.getName()))
				.findFirst()
				.orElse(null);
	}

	/**
	 * Stop and remove the Configuration
	 */
	public void unload(String configurationName) {
		if (configurationName == null) {
			while (!configurations.isEmpty()) {
				removeConfiguration(configurations.get(0));
			}
		} else {
			removeConfiguration(getConfiguration(configurationName));
		}
	}

	private void removeConfiguration(Configuration configuration) {
		Assert.notNull(configuration, "no configuration provided");
		log.info("removing configuration [{}]", configuration);
		configuration.close();

		configurations.remove(configuration);
	}

	/**
	 * Shut down the IBIS instance and clean up.
	 */
	public void shutdown() {
		unload(null);
		IbisCacheManager.shutdown();
	}

	/**
	 * Utility function to give commands to Adapters and Receivers
	 */
	public void handleAction(Action action, String configurationName, String adapterName, String receiverName, String commandIssuedBy, boolean isAdmin) {
		switch (action) {
		case STOPADAPTER:
			Assert.notNull(configurationName, "no configurationName provided");

			if (adapterName == null || adapterName.equals(BusMessageUtils.ALL_CONFIGS_KEY)) {
				if (configurationName.equals(BusMessageUtils.ALL_CONFIGS_KEY)) {
					log.info("Stopping all adapters on request of [{}]", commandIssuedBy);
					for (Configuration configuration : configurations) {
						stopConfiguration(configuration);
					}
				} else {
					log.info("Stopping all adapters for configuration [{}] on request of [{}]", configurationName, commandIssuedBy);
					stopConfiguration(getConfiguration(configurationName));
				}
			} else {
				Configuration configuration = getConfiguration(configurationName);
				Assert.notNull(configuration, ()->"configuration ["+configuration+"] not found");
				Adapter adapter = configuration.getRegisteredAdapter(adapterName);
				Assert.notNull(adapter, ()->"adapter ["+adapterName+"] not found");

				log.info("Stopping adapter [{}], on request of [{}]", adapterName, commandIssuedBy);
				adapter.stop();
			}
			break;

		case STARTADAPTER:
			Assert.notNull(configurationName, "no configurationName provided");

			if (adapterName == null || adapterName.equals(BusMessageUtils.ALL_CONFIGS_KEY)) {
				if (configurationName.equals(BusMessageUtils.ALL_CONFIGS_KEY)) {
					log.info("Starting all adapters on request of [{}]", commandIssuedBy);
					for (Configuration configuration : configurations) {
						startConfiguration(configuration);
					}
				} else {
					log.info("Starting all adapters for configuration [{}] on request of [{}]", configurationName, commandIssuedBy);
					startConfiguration(getConfiguration(configurationName));
				}
			} else {
				try {
					Configuration configuration = getConfiguration(configurationName);
					Assert.notNull(configuration, ()->"configuration ["+configuration+"] not found");
					Adapter adapter = configuration.getRegisteredAdapter(adapterName);
					Assert.notNull(adapter, ()->"adapter ["+adapterName+"] not found");

					log.info("Starting adapter [{}] on request of [{}]", adapterName, commandIssuedBy);
					adapter.start();
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

		log.info("request to stop receiver [{}] on request of [{}]", receiverName, commandIssuedBy);
		receiver.stop();
	}

	private void startReceiver(String configurationName, String adapterName, String receiverName, String commandIssuedBy) {
		Adapter adapter = getAdapterByName(configurationName, adapterName);

		Assert.notNull(receiverName, "no receiverName provided");
		Receiver<?> receiver = adapter.getReceiverByName(receiverName);
		Assert.notNull(receiver, ()->"receiver ["+receiverName+"] not found");

		log.info("request to start receiver [{}] on request of [{}]", receiverName, commandIssuedBy);
		receiver.start();
	}

	private void startConfiguration(Configuration configuration) {
		Assert.notNull(configuration, "no configuration provided");
		log.info("Starting all autostart-configured adapters for configuation [{}]", configuration::getName);
		configuration.start();
	}

	private void stopConfiguration(Configuration configuration) {
		Assert.notNull(configuration, "no configuration provided");
		log.info("Stopping all adapters for configuation [{}]", configuration::getName);
		configuration.stop();
	}
}
