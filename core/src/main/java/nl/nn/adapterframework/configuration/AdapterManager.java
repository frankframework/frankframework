/*
   Copyright 2021-2023 WeAreFrank!

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.Lifecycle;
import org.springframework.context.LifecycleProcessor;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.lifecycle.ConfigurableLifecyleBase;
import nl.nn.adapterframework.lifecycle.ConfiguringLifecycleProcessor;
import nl.nn.adapterframework.util.RunState;
import nl.nn.adapterframework.util.StringUtil;

/**
 * configure/start/stop lifecycles are managed by Spring. See {@link ConfiguringLifecycleProcessor}
 *
 */
public class AdapterManager extends ConfigurableLifecyleBase implements ApplicationContextAware, AutoCloseable {

	private @Getter @Setter ApplicationContext applicationContext;
	private List<? extends AdapterLifecycleWrapperBase> adapterLifecycleWrappers;

	private List<Runnable> startAdapterThreads = Collections.synchronizedList(new ArrayList<>());
	private List<Runnable> stopAdapterThreads = Collections.synchronizedList(new ArrayList<>());

	private final Map<String, Adapter> adapters = new LinkedHashMap<>(); // insertion order map

	public void registerAdapter(Adapter adapter) {
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

	public void unRegisterAdapter(Adapter adapter) {
		String name = adapter.getName();
		if(adapterLifecycleWrappers != null) {
			for (AdapterLifecycleWrapperBase adapterProcessor : adapterLifecycleWrappers) {
				adapterProcessor.removeAdapter(adapter);
			}
		}

		adapters.remove(name);
		log.debug("unregistered adapter [{}] from AdapterManager [{}]", name, this);
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

	public List<Runnable> getStartAdapterThreads() {
		return startAdapterThreads;
	}

	public void addStopAdapterThread(Runnable runnable) {
		stopAdapterThreads.add(runnable);
	}

	public void removeStopAdapterThread(Runnable runnable) {
		stopAdapterThreads.remove(runnable);
	}

	public List<Runnable> getStopAdapterThreads() {
		return stopAdapterThreads;
	}

	public Adapter getAdapter(String name) {
		return getAdapters().get(name);
	}
	public Adapter getAdapter(int i) {
		return getAdapterList().get(i);
	}

	public final Map<String, Adapter> getAdapters() {
		return Collections.unmodifiableMap(adapters);
	}

	public List<Adapter> getAdapterList() {
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
			log.info("stopping adapter [{}]", adapter::getName);
			adapter.stopRunning();
		}

		updateState(RunState.STOPPED);
	}

	@Override
	public void close() {
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
		while (!getStartAdapterThreads().isEmpty()) {
			log.debug("waiting for start threads to end: {}", ()-> StringUtil.safeCollectionToString(getStartAdapterThreads()));
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.warn("Interrupted thread while waiting for start threads to end", e);
			}
		}

		if(!inState(RunState.STOPPED)) {
			stop(); //Call this just in case...
		}

		while (!getStopAdapterThreads().isEmpty()) {
			log.debug("waiting for stop threads to end: {}", () -> StringUtil.safeCollectionToString(getStopAdapterThreads()));
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.warn("Interrupted thread while waiting for stop threads to end", e);
			}
		}

		while (!getAdapterList().isEmpty()) {
			Adapter adapter = getAdapter(0);
			unRegisterAdapter(adapter);
		}
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
