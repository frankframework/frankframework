/*
   Copyright 2021 WeAreFrank!

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

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.Lifecycle;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.RunStateEnum;

public class AdapterManager implements ApplicationContextAware, AutoCloseable, Lifecycle {
	protected final Logger log = LogUtil.getLogger(this);

	private @Getter @Setter ApplicationContext applicationContext;
	private List<? extends AdapterLifecycleWrapperBase> adapterLifecycleWrappers;

	private enum BootState {
		STARTING, STARTED, STOPPING, STOPPED;
	}
	private BootState state = BootState.STARTING;

	private List<Runnable> startAdapterThreads = Collections.synchronizedList(new ArrayList<Runnable>());
	private List<Runnable> stopAdapterThreads = Collections.synchronizedList(new ArrayList<Runnable>());

	private final Map<String, Adapter> adapters = new LinkedHashMap<>(); // insertion order map

	public void registerAdapter(Adapter adapter) {
		if(state != BootState.STARTING) {
			log.warn("cannot add adapter, manager in state ["+state.name()+"]");
		}

		if(log.isDebugEnabled()) log.debug("registering adapter ["+adapter+"] with AdapterManager ["+this+"]");
		if(adapter.getName() == null) {
			throw new IllegalStateException("Adapter has no name");
		}
		if(adapters.containsKey(adapter.getName())) {
			throw new IllegalStateException("Adapter [" + adapter.getName() + "] already registered.");
		}

		if(adapterLifecycleWrappers != null) {
			for (AdapterLifecycleWrapperBase adapterProcessor : adapterLifecycleWrappers) {
				adapterProcessor.addAdapter(adapter);
			}
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
		if(log.isDebugEnabled()) log.debug("unregistered adapter ["+name+"] from AdapterManager ["+this+"]");
	}

	@Autowired
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

	/**
	 * Get a registered adapter by its name through {@link IAdapterService#getAdapter(String)}
	 * @param name the adapter to retrieve
	 * @return IAdapter
	 */
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

	public List<String> getSortedStartedAdapterNames() {
		List<String> startedAdapters = new ArrayList<String>();
		for (int i = 0; i < getAdapterList().size(); i++) {
			IAdapter adapter = getAdapter(i);
			// add the adapterName if it is started.
			if (adapter.getRunState().equals(RunStateEnum.STARTED)) {
				startedAdapters.add(adapter.getName());
			}
		}
		Collections.sort(startedAdapters, String.CASE_INSENSITIVE_ORDER);
		return startedAdapters;
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
		if(state != BootState.STARTING) {
			return;
		}

		log.info("starting all autostart-configured adapters for AdapterManager "+this+"]");
		for (Adapter adapter : getAdapterList()) {
			try {
				adapter.configure();
			} catch (ConfigurationException e) {
				log.error("error configuring adapter ["+adapter.getName()+"]", e);
			}

			if (adapter.configurationSucceeded() && adapter.isAutoStart()) {
				log.info("Starting adapter [" + adapter.getName() + "]");
				adapter.startRunning();
			}
		}
		state = BootState.STARTED;
	}

	/**
	 * Stops all Adapters registered in this manager.
	 */
	@Override
	public void stop() {
		if(state != BootState.STARTED) {
			return;
		}

		state = BootState.STOPPING;
		log.info("stopping all adapters in AdapterManager ["+this+"]");
		List<Adapter> adapters = getAdapterList();
		Collections.reverse(adapters);
		for (Adapter adapter : adapters) {
			log.info("stopping adapter [" + adapter.getName() + "]");
			adapter.stopRunning();
		}
		state = BootState.STOPPED;
	}

	@Override
	public void close() {
		log.info("destroying AdapterManager ["+this+"]");

		while (getStartAdapterThreads().size() > 0) {
			log.debug("Waiting for start threads to end: " + getStartAdapterThreads());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.warn("Interrupted waiting for start threads to end", e);
			}
		}

		stop(); //Call this just in case...

		while (getStopAdapterThreads().size() > 0) {
			log.debug("Waiting for stop threads to end: " + getStopAdapterThreads());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.warn("Interrupted waiting for stop threads to end", e);
			}
		}

		while (getAdapterList().size() > 0) {
			Adapter adapter = getAdapter(0);
			unRegisterAdapter(adapter);
		}
	}

	@Override
	public boolean isRunning() {
		return state == BootState.STARTED;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()));
		builder.append(" state ["+state+"]");
		builder.append(" adapters ["+adapters.size()+"]");
		if(applicationContext != null) {
			builder.append(" applicationContext ["+applicationContext.getDisplayName()+"]");
		}
		return builder.toString();
	}
}
