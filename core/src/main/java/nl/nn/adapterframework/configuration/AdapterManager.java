package nl.nn.adapterframework.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.RunStateEnum;

public class AdapterManager implements InitializingBean, AutoCloseable, ApplicationContextAware {
	protected final Logger log = LogUtil.getLogger(this);

	private @Getter @Setter ApplicationContext applicationContext;
	private List<? extends AdapterProcessor> adapterProcessors;

	private List<Runnable> startAdapterThreads = Collections.synchronizedList(new ArrayList<Runnable>());
	private List<Runnable> stopAdapterThreads = Collections.synchronizedList(new ArrayList<Runnable>());

	private final Map<String, Adapter> adapters = new LinkedHashMap<>(); // insertion order map

	@Override
	public void afterPropertiesSet() throws Exception {
		System.out.println("init");
	}

	@Override
	public void close() {
		System.out.println("destroying AdapterManager");

		while (getStartAdapterThreads().size() > 0) {
			log.debug("Waiting for start threads to end: " + getStartAdapterThreads());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.warn("Interrupted waiting for start threads to end", e);
			}
		}

		stopAdapters();
		while (getStopAdapterThreads().size() > 0) {
			log.debug("Waiting for stop threads to end: " + getStopAdapterThreads());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.warn("Interrupted waiting for stop threads to end", e);
			}
		}
//		unRegisterAdapters
	}

	public void registerAdapter(Adapter adapter) {
		if(log.isDebugEnabled()) log.debug("registering adapter ["+adapter+"] with AdapterService ["+this+"]");
		if(adapter.getName() == null) {
			throw new IllegalStateException("Adapter has no name");
		}
		if(adapters.containsKey(adapter.getName())) {
			throw new IllegalStateException("Adapter [" + adapter.getName() + "] already registered.");
		}

		for (AdapterProcessor adapterProcessor : adapterProcessors) {
			adapterProcessor.addAdapter(adapter);
		}
		adapters.put(adapter.getName(), adapter);
	}

	@Autowired
	public void setAdapterProcessors(List<? extends AdapterProcessor> adapterProcessors) {
		this.adapterProcessors = adapterProcessors;
		System.err.println(adapterProcessors);
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

	public void stopAdapters() {
		log.info("Stopping all adapters for configuation " + applicationContext.getId());
		List<Adapter> adapters = getAdapterList();
		Collections.reverse(adapters);
		for (Adapter adapter : adapters) {
			log.info("Stopping adapter [" + adapter.getName() + "]");
			adapter.stopRunning();
		}
//		call unregister in processors
	}

	public void startAdapters() {
		log.info("Starting all autostart-configured adapters for configuation " + applicationContext.getDisplayName());
		for (Adapter adapter : getAdapters().values()) {
			if (adapter.isAutoStart()) {
				log.info("Starting adapter [" + adapter.getName() + "]");
				adapter.startRunning();
			}
		}
	}
}
