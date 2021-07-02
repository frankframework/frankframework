/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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
import org.springframework.context.ApplicationListener;
import org.springframework.context.Lifecycle;
import org.springframework.context.LifecycleProcessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.cache.IbisCacheManager;
import nl.nn.adapterframework.configuration.classloaders.IConfigurationClassLoader;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jms.JmsRealm;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.lifecycle.ConfigurableLifecycle;
import nl.nn.adapterframework.lifecycle.LazyLoadingEventListener;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.statistics.StatisticsKeeperLogger;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.flow.FlowDiagramManager;

/**
 * The Configuration is placeholder of all configuration objects. Besides that, it provides
 * functions for starting and stopping adapters as a facade.
 *
 * @author Johan Verrips
 * @see    nl.nn.adapterframework.configuration.ConfigurationException
 * @see    nl.nn.adapterframework.core.Adapter
 */
public class Configuration extends ClassPathXmlApplicationContext implements IConfigurable, ApplicationContextAware, ConfigurableLifecycle {
	protected Logger log = LogUtil.getLogger(this);

	private Boolean autoStart = null;
	private boolean enabledAutowiredPostProcessing = false;

	private @Getter @Setter AdapterManager adapterManager; //We have to manually inject the AdapterManager bean! See refresh();
	private @Getter @Setter ScheduleManager scheduleManager; //We have to manually inject the AdapterManager bean! See refresh();

	private BootState state = BootState.STOPPED;

	private String version;
	private IbisManager ibisManager;
	private String originalConfiguration;
	private String loadedConfiguration;
	private StatisticsKeeperIterationHandler statisticsHandler = null;
	private @Getter @Setter boolean configured = false;

	private ConfigurationException configurationException = null;

	private Date statisticsMarkDateMain=new Date();
	private Date statisticsMarkDateDetails=statisticsMarkDateMain;

	public void forEachStatisticsKeeper(StatisticsKeeperIterationHandler hski, Date now, Date mainMark, Date detailMark, int action) throws SenderException {
		Object root = hski.start(now,mainMark,detailMark);
		try {
			Object groupData=hski.openGroup(root,AppConstants.getInstance().getString("instance.name",""),"instance");
			for (Adapter adapter : adapterManager.getAdapterList()) {
				adapter.forEachStatisticsKeeperBody(hski,groupData,action);
			}
			IbisCacheManager.iterateOverStatistics(hski, groupData, action);
			hski.closeGroup(groupData);
		} finally {
			hski.end(root);
		}
	}

	public void dumpStatistics(int action) {
		Date now = new Date();
		boolean showDetails=(action == HasStatistics.STATISTICS_ACTION_FULL ||
							 action == HasStatistics.STATISTICS_ACTION_MARK_FULL ||
							 action == HasStatistics.STATISTICS_ACTION_RESET);
		try {
			if (statisticsHandler==null) {
				statisticsHandler =new StatisticsKeeperLogger();
				statisticsHandler.configure();
			}

//			StatisticsKeeperIterationHandlerCollection skihc = new StatisticsKeeperIterationHandlerCollection();
//
//			StatisticsKeeperLogger skl =new StatisticsKeeperLogger();
//			skl.configure();
//			skihc.registerIterationHandler(skl);
//
//			StatisticsKeeperStore skih = new StatisticsKeeperStore();
//			skih.setJmsRealm("lokaal");
//			skih.configure();
//			skihc.registerIterationHandler(skih);

			forEachStatisticsKeeper(statisticsHandler, now, statisticsMarkDateMain, showDetails ?statisticsMarkDateDetails : null, action);
		} catch (Exception e) {
			log.error("dumpStatistics() caught exception", e);
		}
		if (action==HasStatistics.STATISTICS_ACTION_RESET ||
			action==HasStatistics.STATISTICS_ACTION_MARK_MAIN ||
			action==HasStatistics.STATISTICS_ACTION_MARK_FULL) {
				statisticsMarkDateMain=now;
		}
		if (action==HasStatistics.STATISTICS_ACTION_RESET ||
			action==HasStatistics.STATISTICS_ACTION_MARK_FULL) {
				statisticsMarkDateDetails=now;
		}

	}

	public Configuration() {
		setConfigLocation("FrankFrameworkConfigurationContext.xml"); //Don't call the super(..), it will trigger a refresh.
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

		setVersion(ConfigurationUtils.getConfigurationVersion(getClassLoader()));
		if(StringUtils.isEmpty(getVersion())) {
			log.info("unable to determine [configuration.version] for configuration [{}]", ()-> getName());
		} else {
			log.debug("configuration [{}] found currentConfigurationVersion [{}]", ()-> getName(), ()-> getVersion());
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
		log.info("initialized Configuration [{}] with ClassLoader [{}]", ()-> toString(), ()-> getClassLoader());
	}

	/**
	 * Don't manually call this method. Spring should automatically trigger 
	 * this when super.afterPropertiesSet(); is called.
	 */
	@Override
	public void refresh() throws BeansException, IllegalStateException {
		super.refresh();

		if(adapterManager == null) { //Manually set the AdapterManager bean
			setAdapterManager(getBean("adapterManager", AdapterManager.class));
		}
		if(scheduleManager == null) { //Manually set the ScheduleManager bean
			setScheduleManager(getBean("scheduleManager", ScheduleManager.class));
		}
	}

	// We do not want all listeners to be initialized upon context startup. Hence listeners implementing LazyLoadingEventListener will be excluded from the beanType[].
	@Override
	public String[] getBeanNamesForType(Class<?> type, boolean includeNonSingletons, boolean allowEagerInit) {
		if(type.isAssignableFrom(ApplicationListener.class)) {
			List<String> blacklist = Arrays.asList(super.getBeanNamesForType(LazyLoadingEventListener.class, includeNonSingletons, allowEagerInit));
			List<String> beanNames = Arrays.asList(super.getBeanNamesForType(type, includeNonSingletons, allowEagerInit));
			log.info("removing LazyLoadingEventListeners "+blacklist+" from Spring auto-magic event-based initialization");

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
		state = BootState.STARTED;
	}

	/**
	 * Digest the configuration and generate flow diagram.
	 */
	@Override
	public void configure() {
		log.info("configuring configuration ["+getId()+"]");
		state = BootState.STARTING;

		ConfigurationDigester configurationDigester = getBean(ConfigurationDigester.class);
		try {
			configurationDigester.digest();
		} catch (ConfigurationException e) {
			throw new IllegalStateException(e);
		}

		FlowDiagramManager flowDiagramManager = getBean(FlowDiagramManager.class);
		try {
			flowDiagramManager.generate(this);
		} catch (Exception e) { //Don't throw an exception when generating the flow fails
			ConfigurationWarnings.add(this, log, "Error generating flow diagram for configuration ["+getName()+"]", e);
		}

		//Trigger a configure on all Lifecycle beans
		LifecycleProcessor lifecycle = getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
		if(lifecycle instanceof ConfigurableLifecycle) {
			((ConfigurableLifecycle) lifecycle).configure();
		}

		setConfigured(true);
	}

	@Override
	public void close() {
		try {
			state = BootState.STOPPING;
			super.close();
		} finally {
			state = BootState.STOPPED;
		}
	}

	public boolean isUnloadInProgressOrDone() {
		return inState(BootState.STOPPING) || inState(BootState.STOPPED);
	}

	@Override
	public boolean isRunning() {
		return inState(BootState.STARTED) && super.isRunning();
	}

	@Override
	public BootState getState() {
		return state;
	}

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
	 * Register an adapter with the configuration.
	 */
	public void registerAdapter(Adapter adapter) {
		adapter.setConfiguration(this);
		adapterManager.registerAdapter(adapter);

		log.debug("Configuration [" + getName() + "] registered adapter [" + adapter.toString() + "]");
	}

	/**
	 * Register an {@link JobDef job} for scheduling at the configuration.
	 * The configuration will create an {@link JobDef AdapterJob} instance and a JobDetail with the
	 * information from the parameters, after checking the
	 * parameters of the job. (basically, it checks wether the adapter and the
	 * receiver are registered.
	 * <p>See the <a href="http://quartz.sourceforge.net">Quartz scheduler</a> documentation</p>
	 * @param jobdef a JobDef object
	 * @see nl.nn.adapterframework.scheduler.JobDef for a description of Cron triggers
	 * @since 4.0
	 */
	public void registerScheduledJob(JobDef jobdef) throws ConfigurationException {
		scheduleManager.register(jobdef);
	}

	public void registerStatisticsHandler(StatisticsKeeperIterationHandler handler) throws ConfigurationException {
		log.debug("registerStatisticsHandler() registering ["+ClassUtils.nameOf(handler)+"]");
		statisticsHandler=handler;
		handler.configure();
	}

	@Override
	public void setName(String name) {
		if(StringUtils.isNotEmpty(name)) {
			setId(name); //ID should never be NULL
		}
	}

	@Override
	public String getName() {
		return getId();
	}

	@Deprecated
	@ConfigurationWarning("Please use attribute name instead")
	public void setConfigurationName(String name) {
		this.setName(name);
	}

	public void setVersion(String version) {
		if(StringUtils.isNotEmpty(version)) {
			this.version = version;
		}
	}

	public String getVersion() {
		return version;
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
			} else {
				return null;
			}
		}

		return getClassLoader().getClass().getSimpleName();
	}

	public void setIbisManager(IbisManager ibisManager) {
		this.ibisManager = ibisManager;
	}

	public IbisManager getIbisManager() {
		return ibisManager;
	}

	public void setOriginalConfiguration(String originalConfiguration) {
		this.originalConfiguration = originalConfiguration;
	}

	public String getOriginalConfiguration() {
		return originalConfiguration;
	}

	public void setLoadedConfiguration(String loadedConfiguration) {
		this.loadedConfiguration = loadedConfiguration;
	}

	public String getLoadedConfiguration() {
		return loadedConfiguration;
	}

	public JobDef getScheduledJob(String name) {
		return scheduleManager.getSchedule(name);
	}

	public List<JobDef> getScheduledJobs() {
		return scheduleManager.getSchedulesList();
	}

	public void setConfigurationException(ConfigurationException exception) {
		configurationException = exception;
	}

	public ConfigurationException getConfigurationException() {
		return configurationException;
	}

	public ConfigurationWarnings getConfigurationWarnings() {
		if(isActive()) {
			return getBean("configurationWarnings", ConfigurationWarnings.class);
		}

		return null;
	}

	// Dummy setter to allow JmsRealms being added to Configurations via FrankDoc.xsd
	public void registerJmsRealm(JmsRealm realm) {
		JmsRealmFactory.getInstance().registerJmsRealm(realm);
	}

	@Override
	public ClassLoader getConfigurationClassLoader() {
		return getClassLoader();
	}

	@Override
	public void setBeanName(String name) {
		super.setBeanName(name);
		setDisplayName("ConfigurationContext [" + name + "]");
	}
}
