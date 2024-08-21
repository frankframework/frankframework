/*
   Copyright 2021-2024 WeAreFrank!

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.annotation.Nonnull;

import org.frankframework.core.Adapter;
import org.frankframework.lifecycle.AbstractConfigurableLifecyle;
import org.frankframework.lifecycle.ConfiguringLifecycleProcessor;
import org.frankframework.util.RunState;
import org.frankframework.util.StringUtil;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.Lifecycle;
import org.springframework.context.LifecycleProcessor;

import lombok.Getter;
import lombok.Setter;

/**
 * Manager which holds all adapters within a {@link Configuration}.
 * The manager will start/stop adapters, in a different thread.
 * <p>
 * Configure/start/stop lifecycles are managed by Spring.
 * @see ConfiguringLifecycleProcessor
 *
 */
public class AdapterManager extends AbstractConfigurableLifecyle implements ApplicationContextAware, AutoCloseable {

	private @Getter @Setter ApplicationContext applicationContext;
	private List<? extends AdapterLifecycleWrapperBase> adapterLifecycleWrappers;

	private final List<Runnable> startAdapterThreads = Collections.synchronizedList(new ArrayList<>());
	private final List<Runnable> stopAdapterThreads = Collections.synchronizedList(new ArrayList<>());
	private final AtomicBoolean active = new AtomicBoolean(true); // Flag that indicates whether this manager is active and can accept new Adapters.

	private final Map<String, Adapter> adapters = new LinkedHashMap<>(); // insertion order map

	@Override
	public int getPhase() {
		return 100;
	}

	public void registerAdapter(Adapter adapter) {
		if(!active.get()) {
			throw new IllegalStateException("AdapterManager in state [closed] unable to register Adapter ["+adapter.getName()+"]");
		}

		if(!inState(RunState.STOPPED)) {
			log.warn("cannot add adapter, manager in state [{}]", this::getState);
		}

		// Cast arguments to String before invocation so that we do not have recursive call to logger when trace-level logging is enabled
		if (log.isDebugEnabled()) log.debug("registering adapter [{}] with AdapterManager [{}]", adapter.toString(), this.toString());
		if(adapter.getName() == null) {
			throw new IllegalStateException("adapter has no name");
		}
		if(adapters.containsKey(adapter.getName())) {
			throw new IllegalStateException("adapter [" + adapter.getName() + "] already registered.");
		}

		adapters.put(adapter.getName(), adapter);
	}

	public void removeAdapter(Adapter adapter) {
		if(!adapter.getRunState().isStopped()) {
			log.warn("unable to remove adapter [{}] while in state [{}]", adapter::getName, adapter::getRunState);
			return;
		}

		if(adapterLifecycleWrappers != null) {
			for (AdapterLifecycleWrapperBase adapterProcessor : adapterLifecycleWrappers) {
				adapterProcessor.removeAdapter(adapter);
			}
		}

		String name = adapter.getName();
		adapters.remove(name);
		log.debug("removed adapter [{}] from AdapterManager [{}]", name, this);
	}

	public void setAdapterLifecycleWrappers(List<? extends AdapterLifecycleWrapperBase> adapterLifecycleWrappers) {
		this.adapterLifecycleWrappers = adapterLifecycleWrappers;
	}

	public void addStartAdapterThread(Runnable runnable) {
		startAdapterThreads.add(runnable);
	}

	public void removeStartAdapterThread(Runnable runnable) {
		startAdapterThreads.remove(runnable);
	}

	public void addStopAdapterThread(Runnable runnable) {
		stopAdapterThreads.add(runnable);
	}

	public void removeStopAdapterThread(Runnable runnable) {
		stopAdapterThreads.remove(runnable);
	}

	public Adapter getAdapter(String name) {
		return getAdapters().get(name);
	}

	public final Map<String, Adapter> getAdapters() {
		return Collections.unmodifiableMap(adapters);
	}

	public @Nonnull List<Adapter> getAdapterList() {
		return new ArrayList<>(getAdapters().values());
	}

	@Override
	public void configure() {
		if(!inState(RunState.STOPPED)) {
			log.warn("unable to configure [{}] while in state [{}]", ()->this, this::getState);
			return;
		}
		updateState(RunState.STARTING);

		log.info("configuring all adapters in AdapterManager [{}]", this);

		for (Adapter adapter : getAdapterList()) {
			try {
				if(adapterLifecycleWrappers != null) {
					for (AdapterLifecycleWrapperBase adapterProcessor : adapterLifecycleWrappers) {
						adapterProcessor.addAdapter(adapter);
					}
				}
				adapter.configure();
			} catch (ConfigurationException e) {
				log.error("error configuring adapter [{}]", adapter.getName(), e);
			}
		}
	}

	/**
	 * Inherited from the Spring {@link Lifecycle} interface.
	 * Upon registering all Beans in the ApplicationContext (Configuration)
	 * the {@link LifecycleProcessor} will trigger this method.
	 *
	 * Starts all Adapters registered in this manager.
	 */
	@Override
	public void start() {
		if(!inState(RunState.STARTING)) {
			log.warn("unable to start [{}] while in state [{}]", ()->this, this::getState);
			return;
		}

		log.info("starting all autostart-configured adapters in AdapterManager [{}]", this);
		for (Adapter adapter : getAdapterList()) {
			if (adapter.configurationSucceeded() && adapter.isAutoStart()) {
				log.info("Starting adapter [{}]", adapter::getName);
				adapter.startRunning();
			}
		}

		updateState(RunState.STARTED);
	}

	/**
	 * Stops all Adapters registered in this manager.
	 */
	@Override
	public void stop() {
		if(!inState(RunState.STARTED)) {
			log.warn("forcing [{}] to stop while in state [{}]", ()->this, this::getState);
		}
		updateState(RunState.STOPPING);

		log.info("stopping all adapters in AdapterManager [{}]", this);
		List<Adapter> adapters = getAdapterList();
		Collections.reverse(adapters);
		for (Adapter adapter : adapters) {
			stopAdapter(adapter);
		}
		Thread.yield(); // Give chance to the stop-adapter threads to activate
		updateState(RunState.STOPPED);
	}

	private void stopAdapter(Adapter adapter) {
		log.info("stopping adapter [{}]", adapter::getName);
		adapter.stopRunning();
	}

	/**
	 * Closes this AdapterManager.
	 * All adapters are removed from the Manager and you're unable to (re-)start after it's been closed!
	 */
	@Override
	public void close() {
		active.set(false);
		log.info("destroying AdapterManager [{}]", this);

		try {
			doClose();
		} catch(Exception e) {
			if(!getAdapterList().isEmpty()) {
				Configuration config = (Configuration) applicationContext;
				config.log("not all adapters have been unregistered " + getAdapterList(), e);
			}
		}
	}

	/**
	 * - wait for StartAdapterThreads to finish
	 * - try to stop all adapters
	 * - wait for StopAdapterThreads to finish
	 * - unregister all adapters from this manager
	 */
	private void doClose() {
		long sleepDelay = 50L;
		while (!startAdapterThreads.isEmpty()) {
			log.debug("waiting for start threads to end: {}", ()-> StringUtil.safeCollectionToString(startAdapterThreads));

			try {
				Thread.sleep(sleepDelay);
				if (sleepDelay < 1000L) sleepDelay = sleepDelay * 2L;
			} catch (InterruptedException e) {
				log.warn("Interrupted thread while waiting for start threads to end", e);
				Thread.currentThread().interrupt();
			}
		}

		if(!inState(RunState.STOPPED)) {
			stop(); //Call this just in case...
		}

		sleepDelay = 50L;
		while (!stopAdapterThreads.isEmpty()) {
			log.debug("waiting for stop threads to end: {}", () -> StringUtil.safeCollectionToString(stopAdapterThreads));

			try {
				Thread.sleep(sleepDelay);
				if (sleepDelay < 1000L) sleepDelay = sleepDelay * 2L;
			} catch (InterruptedException e) {
				log.warn("Interrupted thread while waiting for stop threads to end", e);
				Thread.currentThread().interrupt();
			}
		}

		getAdapterList().forEach(this::removeAdapter);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName()).append("@").append(Integer.toHexString(hashCode()));
		builder.append(" state [").append(getState()).append("]");
		builder.append(" adapters [").append(adapters.size()).append("]");
		if(applicationContext != null) {
			builder.append(" applicationContext [").append(applicationContext.getDisplayName()).append("]");
		}
		return builder.toString();
	}
}
