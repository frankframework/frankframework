/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.frankframework.configuration.classloaders.IConfigurationClassLoader;
import org.frankframework.configuration.extensions.SapSystems;
import org.frankframework.core.Adapter;
import org.frankframework.core.IConfigurable;
import org.frankframework.doc.Protected;
import org.frankframework.jms.JmsRealm;
import org.frankframework.jms.JmsRealmFactory;
import org.frankframework.lifecycle.ConfigurableLifecycle;
import org.frankframework.lifecycle.LazyLoadingEventListener;
import org.frankframework.lifecycle.SpringContextScope;
import org.frankframework.monitoring.MonitorManager;
import org.frankframework.receivers.Receiver;
import org.frankframework.scheduler.JobDef;
import org.frankframework.scheduler.job.IJob;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;
import org.frankframework.util.MessageKeeper.MessageKeeperLevel;
import org.frankframework.util.RunState;
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
	private static final Logger applicationLog = LogUtil.getLogger("APPLICATION");

	private Boolean autoStart = null;
	private final boolean enabledAutowiredPostProcessing = false;

	private @Getter @Setter AdapterManager adapterManager; //We have to manually inject the AdapterManager bean! See refresh();
	private @Getter ScheduleManager scheduleManager; //We have to manually inject the ScheduleManager bean! See refresh();

	private @Getter RunState state = RunState.STOPPED;

	private @Getter String version;
	private @Getter IbisManager ibisManager;
	private @Getter String originalConfiguration;
	private @Getter String loadedConfiguration;
	private @Getter boolean configured = false;

	private @Getter ConfigurationException configurationException = null;

	public Configuration() {
		setConfigLocation(SpringContextScope.CONFIGURATION.getContextFile()); //Don't call the super(..), it will trigger a refresh.
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

		if(StringUtils.isNotEmpty(AppConstants.getInstance().getProperty("frankframework-ladybug.version"))) {
			this.getEnvironment().addActiveProfile("aop"); //Makes this configurable depending on if the ladybug is present on the classpath.
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
		log.info("starting configuration [{}]", this::getName);
		if(!isConfigured()) {
			throw new IllegalStateException("cannot start configuration that's not configured");
		}

		super.start();
		state = RunState.STARTED;
	}

	/*
	 * Opposed to close you do not need to reconfigure the configuration.
	 * Allows you to stop and start Configurations.
	 */
	@Override
	public void stop() {
		log.info("stopping configuration [{}]", this::getName);
		state = RunState.STOPPING;
		try {
			super.stop();
		} finally {
			state = RunState.STOPPED;
		}
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
			ConfigurationDigester configurationDigester = getBean(ConfigurationDigester.class);
			configurationDigester.digest();

			//Trigger a configure on all (Configurable) Lifecycle beans
			LifecycleProcessor lifecycle = getBean(LIFECYCLE_PROCESSOR_BEAN_NAME, LifecycleProcessor.class);
			if(!(lifecycle instanceof ConfigurableLifecycle configurableLifecycle)) {
				throw new ConfigurationException("wrong lifecycle processor found, unable to configure beans");
			}

			configurableLifecycle.configure();
		} catch (ConfigurationException e) {
			state = RunState.STOPPED;
			publishEvent(new ConfigurationMessageEvent(this, "aborted starting; "+ e.getMessage()));
			applicationLog.info("Configuration [{}] [{}] was not able to startup", getName(), getVersion());
			throw e;
		}
		configured = true;

		String msg;
		if (isAutoStartup()) {
			start();
			msg = "startup in " + (System.currentTimeMillis() - start) + " ms";
		}
		else {
			msg = "configured in " + (System.currentTimeMillis() - start) + " ms";
		}
		secLog.info("Configuration [{}] [{}] {}", getName(), getVersion(), msg);
		applicationLog.info("Configuration [{}] [{}] {}", getName(), getVersion(), msg);
		publishEvent(new ConfigurationMessageEvent(this, msg));
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
			applicationLog.info("Configuration [{}] [{}] closed", this::getName, this::getVersion);
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

	/** If the Configuration should automatically start all {@link Adapter Adapters} and {@link IJob Scheduled Jobs}. */
	public void setAutoStart(boolean autoStart) {
		this.autoStart = autoStart;
	}

	@Override
	public boolean isAutoStartup() {
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
	 * @return Adapter
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
	 * @see JobDef for a description of Cron triggers
	 * @since 4.0
	 */
	@Deprecated // deprecated to force use of Scheduler element
	public void registerScheduledJob(IJob jobdef) {
		scheduleManager.registerScheduledJob(jobdef);
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
	@Nullable
	public String getClassLoaderType() {
		if(!(getClassLoader() instanceof IConfigurationClassLoader)) { //Configuration has not been loaded yet
			String type = AppConstants.getInstance().getProperty("configurations."+getName()+".classLoaderType");
			if(StringUtils.isNotEmpty(type)) { //We may not return an empty String
				return type;
			}
			return null;
		}

		if(getClassLoader() == null) {
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
		// Dummy Frank!Doc setter
		// Monitors self register in MonitorManager
	}

	@Deprecated
	public void registerMonitoring(MonitorManager factory) {
		// Dummy setter to allow Monitors being added to Configurations via Frank!Config XSD
	}

	public void setSharedResources(SharedResources resource) {
		// Dummy Frank!Doc setter
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
