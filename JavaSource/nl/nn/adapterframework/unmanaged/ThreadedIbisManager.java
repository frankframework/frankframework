/*
 * Created on 6-sep-07
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.adapterframework.unmanaged;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.pipes.IbisLocalSender;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.scheduler.SchedulerHelper;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.quartz.SchedulerException;

/**
 * Implementation of IbisManager which does not use EJB for
 * managing IBIS Adapters.
 *  
 * @author m00035f
 *
 */
public class ThreadedIbisManager implements IbisManager {
    protected Logger log=LogUtil.getLogger(this);
    
    private Configuration configuration;
	private SchedulerHelper schedulerHelper;
    
    /* (non-Javadoc)
	 * @see nl.nn.adapterframework.configuration.IbisManager#getConfiguration()
	 */
	public Configuration getConfiguration() {
		return configuration;
	}
    
    public void startIbis() {
        startAdapters();
        startScheduledJobs();
    }
    
    /**
     * Utility function to give commands to Adapters and Receiverss
     */
    public void handleAdapter(String action, String adapterName, String receiverName, String commandIssuedBy) {
        if (action.equalsIgnoreCase("STOPADAPTER")) {
            if (adapterName.equals("**ALL**")) {
                log.info("Stopping all adapters on request of [" + commandIssuedBy+"]");
                stopAdapters();
            }
            else {
                log.info("Stopping adapter [" + adapterName + "], on request of [" + commandIssuedBy+"]");
                stopAdapter(configuration.getRegisteredAdapter(adapterName));
            }
        }
        else if (action.equalsIgnoreCase("STARTADAPTER")) {
            if (adapterName.equals("**ALL**")) {
                log.info("Starting all adapters on request of [" + commandIssuedBy+"]");
                startAdapters();
            }
            else {
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
    public void startScheduledJobs() {
        List scheduledJobs = configuration.getScheduledJobs();
        // TODO: ScheduleHelper: non-static class injected via Spring
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
		} catch (SchedulerException e) {
            log.error("Could not start scheduler", e);
		}
    }
    
	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.configuration.IbisManager#startAdapters()
	 */
	public void startAdapters() {
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

	/**
	 * @param configuration
	 */
	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

    /**
     * @return
     */
    public SchedulerHelper getSchedulerHelper() {
        return schedulerHelper;
    }

    /**
     * @param helper
     */
    public void setSchedulerHelper(SchedulerHelper helper) {
        schedulerHelper = helper;
    }

}
