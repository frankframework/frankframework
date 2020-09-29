/*
   Copyright 2013, 2016, 2019 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import nl.nn.adapterframework.cache.IbisCacheManager;
import nl.nn.adapterframework.configuration.IAdapterService;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.ejb.ListenerPortPoller;
import nl.nn.adapterframework.extensions.esb.EsbJmsListener;
import nl.nn.adapterframework.extensions.esb.EsbUtils;
import nl.nn.adapterframework.jdbc.JdbcTransactionalStorage;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.scheduler.SchedulerHelper;
import nl.nn.adapterframework.senders.IbisLocalSender;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.flow.FlowDiagramManager;

/**
 * Implementation of IbisManager which does not use EJB for
 * managing IBIS Adapters.
 *
 * @author  Tim van der Leeuw
 * @since   4.8
 */
public class DefaultIbisManager implements IbisManager, InitializingBean {
	protected Logger log = LogUtil.getLogger(this);
	protected Logger secLog = LogUtil.getLogger("SEC");

	private IbisContext ibisContext;
	private List<Configuration> configurations = new ArrayList<Configuration>();
	private SchedulerHelper schedulerHelper;
	private PlatformTransactionManager transactionManager;
	private ListenerPortPoller listenerPortPoller;
	private ApplicationEventPublisher applicationEventPublisher;
	private FlowDiagramManager flowDiagramManager;

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

	/**
	 * Start the already configured Configuration
	 */
	@Override
	public void startConfiguration(Configuration configuration) {
		startAdapters(configuration);
		startScheduledJobs(configuration);
		updateFlowDiagram(configuration);
	}

	private void updateFlowDiagram(Configuration configuration) {
		if (flowDiagramManager != null) {
			try {
				flowDiagramManager.generate(configuration);
			} catch (IOException e) {
				ConfigurationWarnings.add(log, "Error generating flow diagram for configuration ["+configuration.getName()+"]", e);
			}
		}
	}

	@Autowired(required = false)
	@Qualifier("flowDiagramManager")
	public void setFlowDiagramManager(FlowDiagramManager flowDiagramManager) {
		this.flowDiagramManager = flowDiagramManager;
	}

	/**
	 * Shut down the IBIS instance and clean up.
	 */
	@Override
	public void shutdown() {
		if (listenerPortPoller != null) {
			listenerPortPoller.clear();
		}
		unload((String) null);
		IbisCacheManager.shutdown();
	}

	@Override
	public void unload(String configurationName) {
		if (configurationName == null) {
			while (configurations.size() > 0) {
				unload(configurations.get(0));
			}
		} else {
			unload(getConfiguration(configurationName));
		}
	}

	private void unload(Configuration configuration) {
		configuration.setUnloadInProgressOrDone(true);
		while (configuration.getStartAdapterThreads().size() > 0) {
			log.debug("Waiting for start threads to end: " + configuration.getStartAdapterThreads());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.warn("Interrupted waiting for start threads to end", e);
			}
		}
		stopAdapters(configuration);
		while (configuration.getStopAdapterThreads().size() > 0) {
			log.debug("Waiting for stop threads to end: " + configuration.getStopAdapterThreads());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				log.warn("Interrupted waiting for stop threads to end", e);
			}
		}
		while (configuration.getRegisteredAdapters().size() > 0) {
			IAdapter adapter = configuration.getRegisteredAdapter(0);
			IAdapterService adapterService = configuration.getAdapterService();
			adapterService.unRegisterAdapter(adapter);
		}

		//Remove all registered jobs
		for (JobDef jobdef : configuration.getScheduledJobs()) {
			try {
				getSchedulerHelper().deleteTrigger(jobdef);
			}
			catch (SchedulerException se) {
				log.error("unable to remove scheduled job ["+jobdef+"]", se);
			}
		}

		configurations.remove(configuration);
	}

	/**
	 * Utility function to give commands to Adapters and Receivers
	 */
	@Override
	public void handleAdapter(String action, String configurationName,
			String adapterName, String receiverName, String commandIssuedBy,
			boolean isAdmin) {
		if (action.equalsIgnoreCase("STOPADAPTER")) {
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
		} else if (action.equalsIgnoreCase("STARTADAPTER")) {
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
		} else if (action.equalsIgnoreCase("STOPRECEIVER")) {
			for (Configuration configuration : configurations) {
				if (configuration.getRegisteredAdapter(adapterName) != null) {
					IAdapter adapter = configuration.getRegisteredAdapter(adapterName);
					IReceiver receiver = adapter.getReceiverByName(receiverName);
					receiver.stopRunning();
					log.info("receiver [" + receiverName + "] stopped by webcontrol on request of " + commandIssuedBy);
				}
			}
		} else if (action.equalsIgnoreCase("STARTRECEIVER")) {
			for (Configuration configuration : configurations) {
				if (configuration.getRegisteredAdapter(adapterName) != null) {
					IAdapter adapter = configuration.getRegisteredAdapter(adapterName);
					IReceiver receiver = adapter.getReceiverByName(receiverName);
					receiver.startRunning();
					log.info("receiver [" + receiverName + "] started by " + commandIssuedBy);
				}
			}
		} else if (action.equalsIgnoreCase("RELOAD")) {
			String msg = "Reload configuration [" + configurationName + "] on request of [" + commandIssuedBy+"]";
			log.info(msg);
			secLog.info(msg);
			ibisContext.reload(configurationName);
		} else if (action.equalsIgnoreCase("FULLRELOAD")) {
			if (isAdmin) {
				String msg = "Full reload on request of [" + commandIssuedBy+"]";
				log.info(msg);
				secLog.info(msg);
				ibisContext.fullReload();
			} else {
				log.warn("Full reload not allowed for [" + commandIssuedBy+"]");
			}
		} else if (action.equalsIgnoreCase("INCTHREADS")) {
			for (Configuration configuration : configurations) {
				if (configuration.getRegisteredAdapter(adapterName) != null) {
					IAdapter adapter = configuration.getRegisteredAdapter(adapterName);
					IReceiver receiver = adapter.getReceiverByName(receiverName);
					if (receiver instanceof IThreadCountControllable) {
						IThreadCountControllable tcc = (IThreadCountControllable)receiver;
						if (tcc.isThreadCountControllable()) {
							tcc.increaseThreadCount();
						}
					}
					log.info("receiver [" + receiverName + "] increased threadcount on request of " + commandIssuedBy);
				}
			}
		} else if (action.equalsIgnoreCase("DECTHREADS")) {
			for (Configuration configuration : configurations) {
				if (configuration.getRegisteredAdapter(adapterName) != null) {
					IAdapter adapter = configuration.getRegisteredAdapter(adapterName);
					IReceiver receiver = adapter.getReceiverByName(receiverName);
					if (receiver instanceof IThreadCountControllable) {
						IThreadCountControllable tcc = (IThreadCountControllable)receiver;
						if (tcc.isThreadCountControllable()) {
							tcc.decreaseThreadCount();
						}
					}
					log.info("receiver [" + receiverName + "] decreased threadcount on request of " + commandIssuedBy);
				}
			}
		} else if (action.equalsIgnoreCase("SENDMESSAGE")) {
			try {
				// send job
				IbisLocalSender localSender = new IbisLocalSender();
				localSender.setJavaListener(receiverName);
				localSender.setIsolated(false);
				localSender.setName("AdapterJob");
				localSender.configure();
				localSender.open();
				try {
					localSender.sendMessage(new Message(""), null);
				} finally {
					localSender.close();
				}
			} catch(Exception e) {
				log.error("Error while sending message (as part of scheduled job execution)", e);
			}
		} else if (action.equalsIgnoreCase("MOVEMESSAGE")) {
			for (Configuration configuration : configurations) {
				if (configuration.getRegisteredAdapter(adapterName) != null) {
					IAdapter adapter = configuration.getRegisteredAdapter(adapterName);
					IReceiver receiver = adapter.getReceiverByName(receiverName);
					if (receiver instanceof ReceiverBase) {
						ReceiverBase rb = (ReceiverBase) receiver;
						ITransactionalStorage errorStorage = rb.getErrorStorage();
						if (errorStorage == null) {
							log.error("action [" + action + "] is only allowed for receivers with an ErrorStorage");
						} else {
							if (errorStorage instanceof JdbcTransactionalStorage) {
								JdbcTransactionalStorage jdbcErrorStorage = (JdbcTransactionalStorage) rb.getErrorStorage();
								IListener listener = rb.getListener();
								if (listener instanceof EsbJmsListener) {
									EsbJmsListener esbJmsListener = (EsbJmsListener) listener;
									EsbUtils.receiveMessageAndMoveToErrorStorage(esbJmsListener, jdbcErrorStorage);
								} else {
									log.error("action [" + action + "] is currently only allowed for EsbJmsListener, not for type [" + listener.getClass().getName() + "]");
								}
							} else {
								log.error("action [" + action + "] is currently only allowed for JdbcTransactionalStorage, not for type [" + errorStorage.getClass().getName() + "]");
							}
						}
					}
				}
			}
		}
	}

	public void startScheduledJobs(Configuration configuration) {
		List<JobDef> scheduledJobs = configuration.getScheduledJobs();
		for (Iterator<JobDef> iter = scheduledJobs.iterator(); iter.hasNext();) {
			JobDef jobdef = iter.next();
			try {
				schedulerHelper.scheduleJob(this, jobdef);
				log.info("job scheduled with properties :" + jobdef.toString());
			} catch (Exception e) {
				log.error("Could not schedule job [" + jobdef.getName() + "] cron [" + jobdef.getCronExpression() + "]", e);
			}
		}
		try {
			schedulerHelper.startScheduler();
			log.info("Scheduler started");
		} catch (SchedulerException e) {
			log.error("Could not start scheduler", e);
		}
	}

	private void startAdapters(Configuration configuration) {
		log.info("Starting all autostart-configured adapters for configuation " + configuration.getName());
		for (IAdapter adapter : configuration.getAdapterService().getAdapters().values()) {
			if (adapter.isAutoStart()) {
				log.info("Starting adapter [" + adapter.getName() + "]");
				adapter.startRunning();
			}
		}
	}

	private void stopAdapters() {
		for (Configuration configuration : configurations) {
			stopAdapters(configuration);
		}
	}

	private void stopAdapters(Configuration configuration) {
		configuration.dumpStatistics(HasStatistics.STATISTICS_ACTION_MARK_FULL);
		log.info("Stopping all adapters for configuation " + configuration.getName());
		List<IAdapter> adapters = new ArrayList<IAdapter>(configuration.getAdapterService().getAdapters().values());
		Collections.reverse(adapters);
		for (IAdapter adapter : adapters) {
			log.info("Stopping adapter [" + adapter.getName() + "]");
			adapter.stopRunning();
		}
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
	public List<IAdapter> getRegisteredAdapters() {
		List<IAdapter> registeredAdapters = new ArrayList<IAdapter>();
		for (Configuration configuration : configurations) {
			registeredAdapters.addAll(configuration.getRegisteredAdapters());
		}
		return registeredAdapters;
	}

	public List<IAdapter> getRegisteredAdapters(String configurationName) {
		for (Configuration configuration : configurations) {
			if (configurationName.equals(configuration.getName())) {
				return configuration.getRegisteredAdapters();
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

	public void setSchedulerHelper(SchedulerHelper helper) {
		schedulerHelper = helper;
	}

	public SchedulerHelper getSchedulerHelper() {
		return schedulerHelper;
	}

	@Override
	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		log.debug("setting transaction manager to [" + transactionManager + "]");
		this.transactionManager = transactionManager;
	}

	public ListenerPortPoller getListenerPortPoller() {
		return listenerPortPoller;
	}

	public void setListenerPortPoller(ListenerPortPoller listenerPortPoller) {
		this.listenerPortPoller = listenerPortPoller;
	}

	@Override
	public void dumpStatistics(int action) {
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

	@Override
	public void afterPropertiesSet() throws Exception {
		boolean requiresDatabase = AppConstants.getInstance().getBoolean("jdbc.required", true);
		if(requiresDatabase) {
			//Try to create a new transaction to check if there is a connection to the database
			TransactionStatus status = this.transactionManager.getTransaction(new DefaultTransactionDefinition());
			if(status != null) { //If there is a transaction (read connection) close it!
				this.transactionManager.commit(status);
			}
		}
	}
}
