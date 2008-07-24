/*
 * $Log: DefaultIbisManager.java,v $
 * Revision 1.7  2008-07-24 12:23:36  europe\L190409
 * write statistics on shutdown
 *
 * Revision 1.6  2008/01/29 12:16:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for thread number control
 *
 * Revision 1.5  2007/12/10 10:21:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging
 *
 * Revision 1.4  2007/11/22 09:10:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * update from ejb-branch
 *
 * Revision 1.3.2.4  2007/11/15 12:24:30  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Formatting fixes
 *
 * Revision 1.3.2.3  2007/11/15 10:22:29  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * * Add more detailed logging
 *
 * Revision 1.3.2.2  2007/11/15 09:53:34  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * * Add JavaDoc
 * * Extend shutdown-behaviour: destroy beans in the Spring Bean Factory; remove references to the Bean Factory (where reasonably possible)
 *
 * Revision 1.3.2.1  2007/10/25 08:36:58  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add shutdown method for IBIS which shuts down the scheduler too, and which unregisters all EjbJmsConfigurators from the ListenerPortPoller.
 * Unregister JmsListener from ListenerPortPoller during ejbRemove method.
 * Both changes are to facilitate more proper shutdown of the IBIS adapters.
 *
 * Revision 1.3  2007/10/16 09:12:27  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Merge with changes from EJB branch in preparation for creating new EJB brance
 *
 * Revision 1.1.2.4  2007/10/15 09:51:58  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add back transaction-management to BrowseExecute action
 *
 * Revision 1.1.2.3  2007/10/10 14:30:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.2  2007/10/10 07:52:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.unmanaged;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import nl.nn.adapterframework.configuration.AbstractSpringPoweredDigesterFactory;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationDigester;

import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.ejb.ListenerPortPoller;
import nl.nn.adapterframework.pipes.IbisLocalSender;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.scheduler.SchedulerHelper;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StatisticsKeeperLogger;
import nl.nn.adapterframework.util.StatisticsKeeperXmlBuilder;

import org.apache.log4j.Logger;
import org.quartz.SchedulerException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Implementation of IbisManager which does not use EJB for
 * managing IBIS Adapters.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public class DefaultIbisManager implements IbisManager, BeanFactoryAware {
    protected Logger log=LogUtil.getLogger(this);
	
    public static final String DFLT_DIGESTER_RULES = "digester-rules.xml";
    
    private Configuration configuration;
    private String name;
    private ConfigurationDigester configurationDigester;
    private SchedulerHelper schedulerHelper;
    private int deploymentMode;
    private PlatformTransactionManager transactionManager;
    private ListenerPortPoller listenerPortPoller;
    private XmlBeanFactory beanFactory;
    
    protected final String[] deploymentModes = new String[] {DEPLOYMENT_MODE_UNMANAGED_STRING, DEPLOYMENT_MODE_EJB_STRING};
    
    public void loadConfigurationFile(String configurationFile) {
        String digesterRulesFile = DFLT_DIGESTER_RULES;
        
        // Reading in Apache Digester configuration file
        if (null == configurationFile) {
            configurationFile = DFLT_CONFIGURATION;
        }
        
        log.info("* IBIS Startup: Reading IBIS configuration from file [" + configurationFile + "]" + (DFLT_CONFIGURATION.equals(configurationFile) ?
            " (default configuration file)" : ""));
        try {
            configurationDigester.unmarshalConfiguration(
                ClassUtils.getResourceURL(configurationDigester, digesterRulesFile),
                ClassUtils.getResourceURL(configurationDigester, configurationFile));
            name = configuration.getConfigurationName();
        } catch (Throwable e) {
            log.error("Error occured unmarshalling configuration:", e);
        }
    }
    
    
    /**
     * Start the already configured IBIS instance
     */
    public void startIbis() {
        log.info("* IBIS Startup: Initiating startup of IBIS instance [" + name + "]");
        startAdapters();
        startScheduledJobs();
        log.info("* IBIS Startup: Startup complete for instance [" + name + "]");
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
        log.info("* IBIS Shutdown: Initiating shutdown of IBIS instance [" + name + "]");
        // Stop Adapters and the Scheduler
        stopAdapters();
        shutdownScheduler();
        if (listenerPortPoller != null) {
            listenerPortPoller.clear();
        }
        
        // Clean up the Spring Bean Factory and references to it
        // In particular, clean up the static reference from the
        // Digester factory, since that can cause the garbage-collector
        // to never finalize the Bean Factory.
        // Singleton Beans in the Bean Factory are explicitly destroyed,
        // to ensure that they release their resources.
        AbstractSpringPoweredDigesterFactory.factory = null;
        beanFactory.destroySingletons();
        beanFactory = null;
        log.info("* IBIS Shutdown: Shutdown complete for instance [" + name + "]");
    }
    
    /**
     * Utility function to give commands to Adapters and Receivers
     * 
     */
    public void handleAdapter(String action, String adapterName, String receiverName, String commandIssuedBy) {
        if (action.equalsIgnoreCase("STOPADAPTER")) {
            if (adapterName.equals("**ALL**")) {
                log.info("Stopping all adapters on request of [" + commandIssuedBy+"]");
                stopAdapters();
            } else {
                log.info("Stopping adapter [" + adapterName + "], on request of [" + commandIssuedBy+"]");
                stopAdapter(configuration.getRegisteredAdapter(adapterName));
            }
        }
        else if (action.equalsIgnoreCase("STARTADAPTER")) {
            if (adapterName.equals("**ALL**")) {
                log.info("Starting all adapters on request of [" + commandIssuedBy+"]");
                startAdapters();
            } else {
                try {
                    log.info("Starting adapter [" + adapterName + "] on request of [" + commandIssuedBy+"]");
                    startAdapter(configuration.getRegisteredAdapter(adapterName));
                } catch (Exception e) {
                    log.error("error in execution of command [" + action + "] for adapter [" + adapterName + "]",   e);
                    //errors.add("", new ActionError("errors.generic", e.toString()));
                }
            }
        }
        else if (action.equalsIgnoreCase("STOPRECEIVER")) {
            IAdapter adapter = configuration.getRegisteredAdapter(adapterName);
            IReceiver receiver = adapter.getReceiverByName(receiverName);
            receiver.stopRunning();
            log.info("receiver [" + receiverName + "] stopped by webcontrol on request of " + commandIssuedBy);
        }
        else if (action.equalsIgnoreCase("STARTRECEIVER")) {
            IAdapter adapter = configuration.getRegisteredAdapter(adapterName);
            IReceiver receiver = adapter.getReceiverByName(receiverName);
            receiver.startRunning();
            log.info("receiver [" + receiverName + "] started by " + commandIssuedBy);
        }
		else if (action.equalsIgnoreCase("INCTHREADS")) {
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
		else if (action.equalsIgnoreCase("DECTHREADS")) {
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
    }
    
    public void shutdownScheduler() {
        try {
            log.info("Shutting down the scheduler");
            schedulerHelper.getScheduler().shutdown();
        } catch (SchedulerException e) {
            log.error("Could not stop scheduler", e);
        }
    }
    
    public void startScheduledJobs() {
        List scheduledJobs = configuration.getScheduledJobs();
        for (Iterator iter = scheduledJobs.iterator(); iter.hasNext();) {
            JobDef jobdef = (JobDef) iter.next();
            try {
                schedulerHelper.scheduleJob(this, jobdef);
                log.info("job scheduled with properties :" + jobdef.toString());
            } catch (Exception e) {
                log.error("Could not schedule job ["+jobdef.getName()+"]",e);
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
    public void startAdapters() {
        log.info("Starting all autostart-configured adapters");
        List adapters = configuration.getRegisteredAdapters();
        for (Iterator iter = adapters.iterator(); iter.hasNext();) {
            IAdapter adapter = (IAdapter) iter.next();

            if (adapter.isAutoStart()) {
                log.info("Starting adapter [" + adapter.getName()+"]");
                startAdapter(adapter);
            }
        }
    }
    
    /* (non-Javadoc)
     * @see nl.nn.adapterframework.configuration.IbisManager#stopAdapters()
     */
    public void stopAdapters() {
		StatisticsKeeperLogger skl = new StatisticsKeeperLogger();
     	getConfiguration().forEachStatisticsKeeper(skl);
     	
        log.info("Stopping all adapters");
        List adapters = configuration.getRegisteredAdapters();
        for (ListIterator iter = adapters.listIterator(adapters.size()); iter.hasPrevious();) {
            IAdapter adapter = (IAdapter) iter.previous();
            
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

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
    
    public Configuration getConfiguration() {
        return configuration;
    }

    public void setSchedulerHelper(SchedulerHelper helper) {
        schedulerHelper = helper;
    }
    public SchedulerHelper getSchedulerHelper() {
        return schedulerHelper;
    }

    public void setConfigurationDigester(ConfigurationDigester configurationDigester) {
        this.configurationDigester = configurationDigester;
    }
    
    public ConfigurationDigester getConfigurationDigester() {
        return configurationDigester;
    }

    public void setDeploymentMode(int deploymentMode) {
        if (deploymentMode < 0 || deploymentMode >= deploymentModes.length) {
            throw new IllegalArgumentException("DeploymentMode should be a value between 0 and " 
                    + (deploymentModes.length-1) + " inclusive.");
        }
		log.debug("setting deploymentMode to ["+deploymentMode+"]");
        this.deploymentMode = deploymentMode;
    }
    public int getDeploymentMode() {
        return deploymentMode;
    }
    
    public String getDeploymentModeString() {
        return deploymentModes[this.deploymentMode];
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

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (XmlBeanFactory) beanFactory;
    }
}
