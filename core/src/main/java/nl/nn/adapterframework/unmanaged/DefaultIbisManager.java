/*
   Copyright 2013, 2016 Nationale-Nederlanden

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import nl.nn.adapterframework.cache.IbisCacheManager;
import nl.nn.adapterframework.configuration.BasicAdapterServiceImpl;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationDigester;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.configuration.classloaders.BasePathClassLoader;
import nl.nn.adapterframework.core.Adapter;
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
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.RunStateEnum;

import org.apache.log4j.Logger;
import org.quartz.SchedulerException;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Implementation of IbisManager which does not use EJB for
 * managing IBIS Adapters.
 *
 * @author  Tim van der Leeuw
 * @since   4.8
 */
public class DefaultIbisManager implements IbisManager {
    protected Logger log = LogUtil.getLogger(this);

    private List<Configuration> configurations = new ArrayList<Configuration>();
    private SchedulerHelper schedulerHelper;
    private PlatformTransactionManager transactionManager;
    private ListenerPortPoller listenerPortPoller;

	/**
	 * Load configuration with a specific ClassLoader which might for example
	 * override the getResource method to load configuration and related
	 * resources from a different location from the standard classpath. In case
	 * basePath is not null the ClassLoader is wrapped in
	 * {@link BasePathClassLoader} to make it possible to reference resources
	 * in the configuration relative to the configuration file and have an
	 * extra resource override (resource is first resolved relative to the
	 * configuration, when not found it is resolved by the original ClassLoader. 
	 * 
	 * @see ClassUtils#getResourceURL(ClassLoader, String)
	 * @see AppConstants#getInstance(ClassLoader)
	 */
	public void loadConfigurationFile(ClassLoader classLoader, String basePath,
			String configurationFile, boolean configLogAppend) throws ConfigurationException {
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			if (basePath != null) {
				if (classLoader != null) {
					classLoader = new BasePathClassLoader(classLoader, basePath);
				} else {
					classLoader = new BasePathClassLoader(originalClassLoader, basePath);
				}
			}
			if (classLoader != null) {
				Thread.currentThread().setContextClassLoader(classLoader);
			}
			Configuration configuration = new Configuration(new BasicAdapterServiceImpl());
			// Name will be replaced during digest unless not specified in the
			// configuration file.
			configuration.setName(configurationFile);
			configuration.setIbisManager(this);
			configurations.add(configuration);
			ConfigurationWarnings.getInstance().setActiveConfiguration(configuration);
			ConfigurationDigester configurationDigester = new ConfigurationDigester();
			configurationDigester.digestConfiguration(classLoader, configuration, configurationFile, configLogAppend);
			if (configuration.isAutoStart()) {
				startConfiguration(configuration);
			}
		} finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
			ConfigurationWarnings.getInstance().setActiveConfiguration(null);
		}
	}

  public Configuration getConfiguration() {
      if (configurations.size() > 0) {
          return configurations.get(0);
      } else {
          return null;
      }
  }

  public List<Configuration> getConfigurations() {
      return configurations;
  }

  public Configuration getConfiguration(String configurationName) {
      for (Configuration configuration : configurations) {
          if (configurationName.equals(configuration.getConfigurationName())) {
              return configuration;
          }
      }
      return null;
  }

    /**
     * Start the already configured Configuration
     */
    public void startConfiguration(Configuration configuration) {
        log.info("* IBIS Startup: Initiating startup of configuration [" + configuration.getConfigurationName() + "]");
        startAdapters(configuration);
        startScheduledJobs(configuration);
        log.info("* IBIS Startup: Startup complete for configuration [" + configuration.getConfigurationName() + "]");
    }

    /**
     * Shut down the IBIS instance and clean up.
     *
     * After execution of this method, the IBIS instance can not
     * be used anymore.
     *
     * TODO: Add shutdown-methods to Adapter, Receiver, Listener to make shutdown more complete.
     */
    public void shutdownIbis() {
        for (Configuration configuration : configurations) {
            log.info("* IBIS Shutdown: Initiating shutdown of IBIS instance [" + configuration.getConfigurationName() + "]");
            // Stop Adapters and the Scheduler
            shutdownScheduler();
            stopAdapters(configuration);
            if (listenerPortPoller != null) {
                listenerPortPoller.clear();
            }
            IbisCacheManager.shutdown();
            log.info("* IBIS Shutdown: Shutdown complete for instance [" + configuration.getConfigurationName() + "]");
        }
    }

    /**
     * Utility function to give commands to Adapters and Receivers
     *
     */
    public void handleAdapter(String action, String configurationName, String adapterName, String receiverName, String commandIssuedBy) {
        if (action.equalsIgnoreCase("STOPADAPTER")) {
            if (adapterName.equals("*ALL*")) {
                if (configurationName.equals("*ALL*")) {
                    for (Configuration configuration : configurations) {
                        log.info("Stopping all adapters on request of [" + commandIssuedBy+"]");
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
                        stopAdapter(configuration.getRegisteredAdapter(adapterName));
                    }
                }
            }
        }
        else if (action.equalsIgnoreCase("STARTADAPTER")) {
            if (adapterName.equals("*ALL*")) {
                if (configurationName.equals("*ALL*")) {
                    for (Configuration configuration : configurations) {
                        log.info("Starting all adapters on request of [" + commandIssuedBy+"]");
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
                            startAdapter(configuration.getRegisteredAdapter(adapterName));
                        }
                    }
                } catch (Exception e) {
                    log.error("error in execution of command [" + action + "] for adapter [" + adapterName + "]",   e);
                    //errors.add("", new ActionError("errors.generic", e.toString()));
                }
            }
        }
        else if (action.equalsIgnoreCase("STOPRECEIVER")) {
            for (Configuration configuration : configurations) {
                if (configuration.getRegisteredAdapter(adapterName) != null) {
                    IAdapter adapter = configuration.getRegisteredAdapter(adapterName);
                    IReceiver receiver = adapter.getReceiverByName(receiverName);
                    receiver.stopRunning();
                    log.info("receiver [" + receiverName + "] stopped by webcontrol on request of " + commandIssuedBy);
                }
            }
        }
        else if (action.equalsIgnoreCase("STARTRECEIVER")) {
            for (Configuration configuration : configurations) {
                if (configuration.getRegisteredAdapter(adapterName) != null) {
                    IAdapter adapter = configuration.getRegisteredAdapter(adapterName);
                    IReceiver receiver = adapter.getReceiverByName(receiverName);
                    receiver.startRunning();
                    log.info("receiver [" + receiverName + "] started by " + commandIssuedBy);
                }
            }
        }
		else if (action.equalsIgnoreCase("INCTHREADS")) {
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
		}
		else if (action.equalsIgnoreCase("DECTHREADS")) {
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
		}
        else if (action.equalsIgnoreCase("SENDMESSAGE")) {
            try {
                // send job
                IbisLocalSender localSender = new IbisLocalSender();
                localSender.setJavaListener(receiverName);
                localSender.setIsolated(false);
                localSender.setName("AdapterJob");
                localSender.configure();

                localSender.open();
                try {
                    localSender.sendMessage(null, "");
                }
                finally {
                    localSender.close();
                }
            }
            catch(Exception e) {
                log.error("Error while sending message (as part of scheduled job execution)", e);
            }
//          ServiceDispatcher.getInstance().dispatchRequest(receiverName, "");
        }
        else if (action.equalsIgnoreCase("MOVEMESSAGE")) {
			for (Configuration configuration : configurations) {
				if (configuration.getRegisteredAdapter(adapterName) != null) {
					IAdapter adapter = configuration.getRegisteredAdapter(adapterName);
					IReceiver receiver = adapter.getReceiverByName(receiverName);
					if (receiver instanceof ReceiverBase) {
						ReceiverBase rb = (ReceiverBase) receiver;
						ITransactionalStorage errorStorage = rb.getErrorStorage();
						if (errorStorage == null) {
							log.error("action ["
									+ action
									+ "] is only allowed for receivers with an ErrorStorage");
						} else {
							if (errorStorage instanceof JdbcTransactionalStorage) {
								JdbcTransactionalStorage jdbcErrorStorage = (JdbcTransactionalStorage) rb
										.getErrorStorage();
								IListener listener = rb.getListener();
								if (listener instanceof EsbJmsListener) {
									EsbJmsListener esbJmsListener = (EsbJmsListener) listener;
									EsbUtils.receiveMessageAndMoveToErrorStorage(
											esbJmsListener, jdbcErrorStorage);
								} else {
									log.error("action ["
											+ action
											+ "] is currently only allowed for EsbJmsListener, not for type ["
											+ listener.getClass().getName() + "]");
								}
							} else {
								log.error("action ["
										+ action
										+ "] is currently only allowed for JdbcTransactionalStorage, not for type ["
										+ errorStorage.getClass().getName() + "]");
							}
						}
					}
				}
			}
    	}
    }

    public void shutdownScheduler() {
        try {
            log.info("Shutting down the scheduler");
            schedulerHelper.getScheduler().shutdown();
        } catch (SchedulerException e) {
            log.error("Could not stop scheduler", e);
        }
    }

    public void startScheduledJobs(Configuration configuration) {
        List scheduledJobs = configuration.getScheduledJobs();
        for (Iterator iter = scheduledJobs.iterator(); iter.hasNext();) {
            JobDef jobdef = (JobDef) iter.next();
            try {
                schedulerHelper.scheduleJob(this, jobdef);
                log.info("job scheduled with properties :" + jobdef.toString());
            } catch (Exception e) {
                log.error("Could not schedule job ["+jobdef.getName()+"] cron ["+jobdef.getCronExpression()+"]",e);
            }
        }
        try {
            schedulerHelper.startScheduler();
            log.info("Scheduler started");
        } catch (SchedulerException e) {
            log.error("Could not start scheduler", e);
        }
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.configuration.IbisManager#startAdapters()
     */
    public void startAdapters(Configuration configuration) {
        log.info("Starting all autostart-configured adapters for configuation " + configuration.getConfigurationName());
        for (IAdapter adapter : configuration.getAdapterService().getAdapters().values()) {
            if (adapter.isAutoStart()) {
                log.info("Starting adapter [" + adapter.getName()+"]");
                startAdapter(adapter);
            }
        }
    }


    /* (non-Javadoc)
     * @see nl.nn.adapterframework.configuration.IbisManager#stopAdapters()
     */
    public void stopAdapters(Configuration configuration) {
        configuration.dumpStatistics(HasStatistics.STATISTICS_ACTION_MARK_FULL);

        log.info("Stopping all adapters for configuation " + configuration.getConfigurationName());
        List<IAdapter> adapters = new ArrayList<IAdapter>(configuration.getAdapterService().getAdapters().values());
        Collections.reverse(adapters);
        for (IAdapter adapter : adapters) {
            log.info("Stopping adapter [" + adapter.getName() + "]");
			stopAdapter(adapter);
        }
    }

    /**
     * Start the adapter. The thread-name will be set tot the adapter's name.
     * The run method, called by t.start(), will call the startRunning method
     * of the IReceiver. The Adapter will be a new thread, as this interface
     * extends the <code>Runnable</code> interface. The actual starting is done
     * in the <code>run</code> method.
     * @see IReceiver#startRunning()
     * @see Adapter#run
     */
    public void startAdapter(final IAdapter adapter) {
        adapter.startRunning();
        /*
        Object monitor;
        synchronized (adapterThreads) {
            monitor = adapterThreads.get(adapter.getName());
            if (monitor == null) {
                monitor = new Object();
                adapterThreads.put(adapter.getName(), monitor);
            }
        }
        final Object fMonitor = monitor;
        final Thread t = new Thread(new Runnable() {
            public void run() {
                synchronized (fMonitor) {
                    adapter.startRunning();
                    try {
                        fMonitor.wait();
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                    adapter.stopRunning();
                }
            }
        }, adapter.getName());
        t.start();
        */
    }
    public void stopAdapter(final IAdapter adapter) {
        adapter.stopRunning();
        /*
        Object monitor = adapterThreads.get(adapter.getName());
        synchronized (monitor) {
            monitor.notify();
        }
        */
    }

    public IAdapter getRegisteredAdapter(String name) {
        List<IAdapter> adapters = getRegisteredAdapters();
        for (IAdapter adapter : adapters) {
            if (name.equals(adapter.getName())) {
                return adapter;
            }
        }
        return null;
    }

    public List<IAdapter> getRegisteredAdapters() {
        List registeredAdapters = new ArrayList<IAdapter>();
        for (Configuration configuration : configurations) {
            registeredAdapters.addAll(configuration.getRegisteredAdapters());
        }
        return registeredAdapters;
    }

    public List<IAdapter> getRegisteredAdapters(String configurationName) {
        for (Configuration configuration : configurations) {
            if (configurationName.equals(configuration.getConfigurationName())) {
                return configuration.getRegisteredAdapters();
            }
        }
        return null;
    }

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

    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
    	log.debug("setting transaction manager to ["+transactionManager+"]");
        this.transactionManager = transactionManager;
    }

    public ListenerPortPoller getListenerPortPoller() {
        return listenerPortPoller;
    }

    public void setListenerPortPoller(ListenerPortPoller listenerPortPoller) {
        this.listenerPortPoller = listenerPortPoller;
    }

    public void dumpStatistics(int action) {
        for (Configuration configuration : configurations) {
            configuration.dumpStatistics(action);
        }
    }

}
