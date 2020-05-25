package nl.nn.adapterframework.webcontrol.api;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;

import nl.nn.adapterframework.configuration.AdapterService;
import nl.nn.adapterframework.configuration.AdapterServiceImpl;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.util.RunStateEnum;

public class MockIbisManager implements IbisManager {
	IbisContext ibisContext = null;
	List<Configuration> configurations = new ArrayList<Configuration>();

	public MockIbisManager() {
		AdapterService adapterService = new AdapterServiceImpl();
		IAdapter adapter = new Adapter();
		adapter.setName("dummyAdapter");
		try {
			adapterService.registerAdapter(adapter);
		} catch (ConfigurationException e) {
			fail("error registering adapter ["+adapter+"]");
		}
		Configuration mockConfiguration = new Configuration(adapterService);
		mockConfiguration.setName("myConfiguration");
		configurations.add(mockConfiguration);
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
	public void handleAdapter(String action, String configurationName, String adapterName, String receiverName, String commandIssuedBy, boolean isAdmin) {
		// TODO Auto-generated method stub

	}

	@Override
	public void startConfiguration(Configuration configuration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void unload(String configurationName) {
		// TODO Auto-generated method stub

	}

	@Override
	public void shutdown() {
		// TODO Auto-generated method stub

	}

	@Override
	public IAdapter getRegisteredAdapter(String name) {
		List<IAdapter> adapters = getRegisteredAdapters();
		for (IAdapter adapter : adapters) {
			if (name.equals(adapter.getName())) {
				return adapter;
			}
		}
		return null;
	}

	@Override
	public List<String> getSortedStartedAdapterNames() {
		List<String> startedAdapters = new ArrayList<String>();
		for (IAdapter adapter : getRegisteredAdapters()) {
			// add the adapterName if it is started.
			if (adapter.getRunState().equals(RunStateEnum.STARTED)) {
				startedAdapters.add(adapter.getName());
			}
		}
		Collections.sort(startedAdapters, String.CASE_INSENSITIVE_ORDER);
		return startedAdapters;
	}

	@Override
	public List<IAdapter> getRegisteredAdapters() {
		List<IAdapter> registeredAdapters = new ArrayList<IAdapter>();
		for (Configuration configuration : configurations) {
			registeredAdapters.addAll(configuration.getRegisteredAdapters());
		}
		return registeredAdapters;
	}

	@Override
	public PlatformTransactionManager getTransactionManager() {
		return null;
	}

	@Override
	public void dumpStatistics(int action) {
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
	}

	@Override
	public ApplicationEventPublisher getApplicationEventPublisher() {
		return null;
	}

}
