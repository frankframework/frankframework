/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.Lifecycle;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.cache.IbisCacheManager;
import nl.nn.adapterframework.configuration.classloaders.IConfigurationClassLoader;
import nl.nn.adapterframework.configuration.extensions.SapSystems;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.Protected;
import nl.nn.adapterframework.jdbc.migration.DatabaseMigratorBase;
import nl.nn.adapterframework.jms.JmsRealm;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.lifecycle.ConfigurableLifecycle;
import nl.nn.adapterframework.lifecycle.LazyLoadingEventListener;
import nl.nn.adapterframework.lifecycle.SpringContextScope;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.scheduler.job.IJob;
import nl.nn.adapterframework.scheduler.job.Job;
import nl.nn.adapterframework.statistics.HasStatistics.Action;
import nl.nn.adapterframework.statistics.MetricsInitializer;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.statistics.StatisticsKeeperLogger;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeper.MessageKeeperLevel;
import nl.nn.adapterframework.util.RunState;
import nl.nn.adapterframework.util.flow.FlowDiagramManager;

/**
 * Container of {@link Adapter Adapters} that belong together.
 * A configuration may be deployed independently from other configurations.
 * Names of nested elements like {@link Adapter Adapters}, {@link Receiver Receivers}, listeners and senders
 * can be reused in other configurations.
 * <br/><br/>
 * Configurations are shown in the Frank!Console along with their {@link Adapter Adapters},
 * {@link Receiver Receivers}, listeners and senders. The Adapter Status page of the Frank!Console
 * has a tab for each configuration that only shows information
 * about that configuration. See the Frank!Manual for details.
 *
 * @author Johan Verrips
 */
public class Configuration extends ClassPathXmlApplicationContext implements IConfigurable, ApplicationContextAware, ConfigurableLifecycle {
	protected Logger log = LogUtil.getLogger(this);
	private static final Logger secLog = LogUtil.getLogger("SEC");

	private Boolean autoStart = null;
	private boolean enabledAutowiredPostProcessing = false;

	private @Getter @Setter AdapterManager adapterManager; //We have to manually inject the AdapterManager bean! See refresh();
	private @Getter ScheduleManager scheduleManager; //We have to manually inject the ScheduleManager bean! See refresh();

	private @Getter RunState state = RunState.STOPPED;

	private @Getter String version;
	private @Getter IbisManager ibisManager;
	private @Getter String originalConfiguration;
	private @Getter String loadedConfiguration;
	private StatisticsKeeperIterationHandler statisticsHandler = null;
	private @Getter boolean configured = false;

	private @Getter ConfigurationException configurationException = null;

	private Date statisticsMarkDateMain=new Date();
	private Date statisticsMarkDateDetails=statisticsMarkDateMain;

	public Configuration() {
		setConfigLocation(SpringContextScope.CONFIGURATION.getContextFile()); //Don't call the super(..), it will trigger a refresh.
	}

	private void forEachStatisticsKeeper(StatisticsKeeperIterationHandler hski, Date now, Date mainMark, Date detailMark, Action action, String rootName, String rootType) throws SenderException {
		Object root = hski.start(now,mainMark,detailMark);
		try {
			Object groupData= hski.openGroup(root, rootName, rootType);
			for (Adapter adapter : adapterManager.getAdapterList()) {
				if (adapter.isConfigurationSucceeded()) {
					adapter.iterateOverStatistics(hski,groupData,action);
				}
			}
			IbisCacheManager.iterateOverStatistics(hski, groupData, action);
			hski.closeGroup(groupData);
		} finally {
			hski.end(root);
		}
	}

	public void dumpStatistics(Action action) {
		Date now = new Date();
		boolean showDetails=(action == Action.FULL || action == Action.MARK_FULL);
		try {
			if (statisticsHandler==null) {
				statisticsHandler =new StatisticsKeeperLogger();
				statisticsHandler.configure();
			}

			forEachStatisticsKeeper(statisticsHandler, now, statisticsMarkDateMain, showDetails ?statisticsMarkDateDetails : null, action, AppConstants.getInstance().getString("instance.name",""), "instance");
		} catch (Exception e) {
			log.error("dumpStatistics() caught exception", e);
		}
		if (action==Action.MARK_MAIN || action==Action.MARK_FULL) {
				statisticsMarkDateMain=now;
		}
		if (action==Action.MARK_FULL) {
				statisticsMarkDateDetails=now;
		}

	}

	public void initMetrics() throws ConfigurationException {
		StatisticsKeeperIterationHandler metricsInitializer = getBean(MetricsInitializer.class);
		try {
			forEachStatisticsKeeper(metricsInitializer, new Date(), statisticsMarkDateMain, statisticsMarkDateDetails, Action.FULL, getName(), "configuration");
		} catch (SenderException e) {
			throw new ConfigurationException("Cannot initialize metrics", e);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		setParent(applicationContext);
	}

	@Override
	public ApplicationContext getApplicationContext() {
		return this;
	}

	/**
	 * Spring's configure method.
	 * Only called when the Configuration has been added through a parent context!
	 */
	@Override
	public void afterPropertiesSet() {
		if(!(getClassLoader() instanceof IConfigurationClassLoader)) {
			throw new IllegalStateException("No IConfigurationClassLoader set");
		}
		if(ibisManager == null) {
			throw new IllegalStateException("No IbisManager set");
		}
		if(StringUtils.isEmpty(getVersion())) {
			log.info("unable to determine [configuration.version] for configuration [{}]", this::getName);
		} else {
			log.debug("configuration [{}] found currentConfigurationVersion [{}]", this::getName, this::getVersion);
		}

		super.afterPropertiesSet(); //Triggers a context refresh

		if(enabledAutowiredPostProcessing) {
			//Append @Autowired PostProcessor to allow automatic type-based Spring wiring.
			AutowiredAnnotationBeanPostProcessor postProcessor = new AutowiredAnnotationBeanPostProcessor();
			postProcessor.setAutowiredAnnotationType(Autowired.class);
			postProcessor.setBeanFactory(getBeanFactory());
			getBeanFactory().addBeanPostProcessor(postProcessor);
		}

		ibisManager.addConfiguration(this); //Only if successfully refreshed, add the configuration
		log.info("initialized Configuration [{}] with ClassLoader [{}]", this::toString, this::getClassLoader);
	}

	/**
	 * Don't manually call this method. Spring should automatically trigger
	 * this when super.afterPropertiesSet(); is called.
	 */
	@Override
	public void refresh() throws BeansException, IllegalStateException {
		setId(getId()); // Update the setIdCalled flag in AbstractRefreshableConfigApplicationContext. When wired through spring it calls the setBeanName method.
		setVersion(ConfigurationUtils.getConfigurationVersion(getClassLoader()));

		super.refresh();

		setAdapterManager(getBean("adapterManager", AdapterManager.class));
		setScheduleManager(getBean("scheduleManager", ScheduleManager.class));
	}

	// We do not want all listeners to be initialized upon context startup. Hence listeners implementing LazyLoadingEventListener will be excluded from the beanType[].
	@Override
	public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		if(type.isAssignableFrom(ApplicationListener.class)) {
			List<String> blacklist = Arrays.asList(super.getBeanNamesForType(LazyLoadingEventListener.class, includeNonSingletons, allowEagerInit));
			List<String> beanNames = Arrays.asList(super.getBeanNamesForType(type, includeNonSingletons, allowEagerInit));
			log.info("removing LazyLoadingEventListeners {} from Spring auto-magic event-based initialization", blacklist);

			return beanNames.stream().filter(str -> !blacklist.contains(str)).toArray(String[]::new);
		}
		return super.getBeanNamesForType(type, includeNonSingletons, allowEagerInit);
	}

	/**
	 * Spring method which starts the ApplicationContext.
	 * Loads + digests the configuration and calls start() in all registered
	 * beans that implement the Spring {@link Lifecycle} interface.
	 */
	@Override
	public void start() {
		if(!isConfigured()) {
			throw new IllegalStateException("cannot start configuration that's not configured");
		}

		super.start();
		state = RunState.STARTED;
	}

	/**
	 * Digest the configuration and generate flow diagram.
	 */
	@Override
	public void configure() throws ConfigurationException {
		log.info("configuring configuration [{}]", this::getId);
		if(getName().contains("/")) {
			throw new ConfigurationException("It is not allowed to have '/' in configuration name ["+getName()+"]");
		}
		state = RunState.STARTING;
		long start = System.currentTimeMillis();

		try {
			runMigrator();

			ConfigurationDigester configurationDigester = getBean(ConfigurationDigester.class);
			configurationDigester.digest();

			generateConfigurationFlow();

			//Trigger a configure on all Lifecycle beans
			LifecycleProcessor lifecycle = getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
			if(lifecycle instanceof ConfigurableLifecycle) {
				((ConfigurableLifecycle) lifecycle).configure();
			}
		} catch (ConfigurationException e) {
			state = RunState.STOPPED;
			publishEvent(new ConfigurationMessageEvent(this, "aborted starting; "+ e.getMessage()));
			throw e;
		}
		initMetrics();
		configured = true;

		String msg;
		if (isAutoStart()) {
			start();
			msg = "startup in " + (System.currentTimeMillis() - start) + " ms";
		}
		else {
			msg = "configured in " + (System.currentTimeMillis() - start) + " ms";
		}
		secLog.info("Configuration [" + getName() + "] [" + getVersion()+"] " + msg);
		publishEvent(new ConfigurationMessageEvent(this, msg));
	}

	/**
	 * Generate a flow over the digested {@link Configuration}.
	 * Uses {@link Configuration#getLoadedConfiguration()}.
	 */
	private void generateConfigurationFlow() {
		FlowDiagramManager flowDiagramManager = getBean(FlowDiagramManager.class);
		try {
			flowDiagramManager.generate(this);
		} catch (Exception e) { //Don't throw an exception when generating the flow fails
			ConfigurationWarnings.add(this, log, "Error generating flow diagram for configuration ["+getName()+"]", e);
		}
	}

	/** Execute any database changes before calling {@link #configure()}. */
	protected void runMigrator() {
		// For now explicitly call configure, fix this once ConfigurationDigester implements ConfigurableLifecycle
		DatabaseMigratorBase databaseMigrator = getBean("jdbcMigrator", DatabaseMigratorBase.class);
		if(databaseMigrator.isEnabled()) {
			try {
				if(databaseMigrator.validate()) {
					databaseMigrator.update();
				}
			} catch (Exception e) {
				log("unable to run JDBC migration", e);
			}
		}
	}

	@Override
	public void close() {
		try {
			state = RunState.STOPPING;
			super.close();
		} finally {
			configured = false;
			state = RunState.STOPPED;
		}
	}

	// capture ContextClosedEvent which is published during AbstractApplicationContext#doClose()
	@Override
	public void publishEvent(ApplicationEvent event) {
		if(event instanceof ContextClosedEvent) {
			secLog.info("Configuration [" + getName() + "] [" + getVersion()+"] closed");
			publishEvent(new ConfigurationMessageEvent(this, "closed"));
		}

		super.publishEvent(event);
	}

	/**
	 * Log a message to the MessageKeeper that corresponds to this configuration
	 */
	public void log(String message) {
		log(message, (MessageKeeperLevel) null);
	}

	/**
	 * Log a message to the MessageKeeper that corresponds to this configuration
	 */
	public void log(String message, MessageKeeperLevel level) {
		this.publishEvent(new ConfigurationMessageEvent(this, message, level));
	}

	/**
	 * Log a message to the MessageKeeper that corresponds to this configuration
	 */
	public void log(String message, Exception e) {
		this.publishEvent(new ConfigurationMessageEvent(this, message, e));
	}

	public boolean isUnloadInProgressOrDone() {
		return inState(RunState.STOPPING) || inState(RunState.STOPPED);
	}

	@Override
	public boolean isRunning() {
		return inState(RunState.STARTED) && super.isRunning();
	}

	private boolean inState(RunState state) {
		return getState() == state;
	}

	/** If the Configuration should automatically start all {@link Adapter Adapters} and {@link Job Scheduled Jobs}. */
	public void setAutoStart(boolean autoStart) {
		this.autoStart = autoStart;
	}

	public boolean isAutoStart() {
		if(autoStart == null && getClassLoader() != null) {
			autoStart = AppConstants.getInstance(getClassLoader()).getBoolean("configurations.autoStart", true);
		}
		return autoStart;
	}

	public boolean isStubbed() {
		if(getClassLoader() instanceof IConfigurationClassLoader) {
			return ConfigurationUtils.isConfigurationStubbed(getClassLoader());
		}

		return false;
	}

	/**
	 * Get a registered adapter by its name through {@link AdapterManager#getAdapter(String)}
	 * @param name the adapter to retrieve
	 * @return IAdapter
	 */
	public Adapter getRegisteredAdapter(String name) {
		if(adapterManager == null || !isActive()) {
			return null;
		}

		return adapterManager.getAdapter(name);
	}

	public List<Adapter> getRegisteredAdapters() {
		if(adapterManager == null || !isActive()) {
			return Collections.emptyList();
		}
		return adapterManager.getAdapterList();
	}

	public void addStartAdapterThread(Runnable runnable) {
		adapterManager.addStartAdapterThread(runnable);
	}

	public void removeStartAdapterThread(Runnable runnable) {
		adapterManager.removeStartAdapterThread(runnable);
	}

	public void addStopAdapterThread(Runnable runnable) {
		adapterManager.addStopAdapterThread(runnable);
	}

	public void removeStopAdapterThread(Runnable runnable) {
		adapterManager.removeStopAdapterThread(runnable);
	}

	/**
	 * Include the referenced Module in this configuration
	 */
	public void registerInclude(Include module) {
		// method exists to trigger FrankDoc.
	}

	/**
	 * Add adapter.
	 */
	public void registerAdapter(Adapter adapter) {
		adapter.setConfiguration(this);
		adapterManager.registerAdapter(adapter);

		log.debug("Configuration [{}] registered adapter [{}]", this::getName, adapter::toString);
	}

	// explicitly in this position, to have the right location in the XSD
	/**
	 * Container for jobs scheduled for periodic execution.
	 */
	public void setScheduleManager(ScheduleManager scheduleManager) {
		this.scheduleManager = scheduleManager;
	}

	/**
	 * Register an {@link IJob job} for scheduling at the configuration.
	 * The configuration will create an {@link IJob AdapterJob} instance and a JobDetail with the
	 * information from the parameters, after checking the
	 * parameters of the job. (basically, it checks whether the adapter and the
	 * receiver are registered.
	 * <p>See the <a href="https://www.quartz-scheduler.org/">Quartz scheduler</a> documentation</p>
	 * @param jobdef a JobDef object
	 * @see nl.nn.adapterframework.scheduler.JobDef for a description of Cron triggers
	 * @since 4.0
	 */
	@Deprecated // deprecated to force use of Scheduler element
	public void registerScheduledJob(IJob jobdef) {
		scheduleManager.registerScheduledJob(jobdef);
	}

	public void registerStatisticsHandler(StatisticsKeeperIterationHandler handler) throws ConfigurationException {
		log.debug("registerStatisticsHandler() registering [{}]", ()->ClassUtils.nameOf(handler));
		statisticsHandler=handler;
		handler.configure();
	}

	/**
	 * Configurations should be wired through Spring, which in turn should call {@link #setBeanName(String)}.
	 * Once the ConfigurationContext has a name it should not be changed anymore, hence
	 * {@link AbstractRefreshableConfigApplicationContext#setBeanName(String) super.setBeanName(String)} only sets the name once.
	 * If not created by Spring, the setIdCalled flag in AbstractRefreshableConfigApplicationContext wont be set, allowing the name to be updated.
	 *
	 * The DisplayName will always be updated, which is purely used for logging purposes.
	 */
	@Override
	public void setName(String name) {
		if(StringUtils.isNotEmpty(name)) {
			if(state == RunState.STARTING && !getName().equals(name)) {
				publishEvent(new ConfigurationMessageEvent(this, "name ["+getName()+"] does not match XML name attribute ["+name+"]", MessageKeeperLevel.WARN));
			}
			setBeanName(name);
		}
	}

	@Override
	public String getName() {
		return getId();
	}

	/** The version of the Configuration, typically provided by the BuildInfo.properties file. */
	@Protected
	public void setVersion(String version) {
		if(StringUtils.isNotEmpty(version)) {
			if(state == RunState.STARTING && this.version != null && !this.version.equals(version)) {
				publishEvent(new ConfigurationMessageEvent(this, "version ["+this.version+"] does not match XML version attribute ["+version+"]", MessageKeeperLevel.WARN));
			}

			this.version = version;
		}
	}

	/**
	 * If no ClassLoader has been set it tries to fall back on the `configurations.xxx.classLoaderType` property.
	 * Because of this, it may not always represent the correct or accurate type.
	 */
	public String getClassLoaderType() {
		if(!(getClassLoader() instanceof IConfigurationClassLoader)) { //Configuration has not been loaded yet
			String type = AppConstants.getInstance().getProperty("configurations."+getName()+".classLoaderType");
			if(StringUtils.isNotEmpty(type)) { //We may not return an empty String
				return type;
			}
			return null;
		}

		return getClassLoader().getClass().getSimpleName();
	}

	public void setIbisManager(IbisManager ibisManager) {
		this.ibisManager = ibisManager;
	}

	/** The entire (raw) configuration */
	@Protected
	public void setOriginalConfiguration(String originalConfiguration) {
		this.originalConfiguration = originalConfiguration;
	}

	/** The loaded (with resolved properties) configuration */
	@Protected
	public void setLoadedConfiguration(String loadedConfiguration) {
		this.loadedConfiguration = loadedConfiguration;
	}

	public IJob getScheduledJob(String name) {
		if (scheduleManager == null || !isActive()) {
			return null;
		}
		return scheduleManager.getSchedule(name);
	}

	public List<IJob> getScheduledJobs() {
		if (scheduleManager == null || !isActive()) {
			return Collections.emptyList();
		}
		return scheduleManager.getSchedulesList();
	}

	public void setConfigurationException(ConfigurationException exception) {
		configurationException = exception;
	}

	public ConfigurationWarnings getConfigurationWarnings() {
		if (!isActive()) {
			return null;
		}
		return getBean("configurationWarnings", ConfigurationWarnings.class);
	}

	@Override
	public ClassLoader getConfigurationClassLoader() {
		return getClassLoader();
	}

	// Dummy setter to allow SapSystems being added to Configurations via Frank!Config XSD
	public void setSapSystems(SapSystems sapSystems) {
		// SapSystems self register;
	}

	// Dummy setter to allow JmsRealms being added to Configurations via Frank!Config XSD
	public void setJmsRealms(JmsRealmFactory realm) {
		// JmsRealm-objects self register in JmsRealmFactory;
	}

	// Dummy setter to allow JmsRealms being added to Configurations via Frank!Config XSD
	@Deprecated
	public void registerJmsRealm(JmsRealm realm) {
		JmsRealmFactory.getInstance().registerJmsRealm(realm); // For backwards compatibility to support old ibisdoc xsd
	}

	/**
	 * Container for monitor objects
	 */
	public void setMonitoring(MonitorManager monitorManager) {
		// Monitors self register in MonitorManager;
	}
	// above comment is used in FrankDoc
	// Dummy setter to allow Monitors being added to Configurations via Frank!Config XSD
	@Deprecated
	public void registerMonitoring(MonitorManager factory) {
	}

	public void setSharedResources(SharedResources resource) {
		//Dummy Frank!Doc setter
	}

	/**
	 * Overwrite the DisplayName created by the super.setBeanName which prepends 'ApplicationContext'.
	 * The BeanName can only be set once, after which it only updates the DisplayName.
	 */
	@Override
	@Protected
	public void setBeanName(String name) {
		super.setBeanName(name);
		setDisplayName("ConfigurationContext [" + name + "]");
	}
}
