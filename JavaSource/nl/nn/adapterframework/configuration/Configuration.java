package nl.nn.adapterframework.configuration;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IReceiver;

import nl.nn.adapterframework.scheduler.AdapterJob;
import nl.nn.adapterframework.scheduler.JobDef;

import nl.nn.adapterframework.util.AppConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

import java.net.URL;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Enumeration;

/**
 * The Configuration is placeholder of all configuration objects. Besides that, it provides
 * functions for starting and stopping adapters as a facade.
 * @author Johan Verrips
 * @see    nl.nn.adapterframework.configuration.ConfigurationException
 * @see    nl.nn.adapterframework.core.IAdapter
 * <p>$Id $</p>
 */
public class Configuration {
    protected Logger log; 
    public static final String version="$Id: Configuration.java,v 1.5 2004-03-23 16:39:20 L190409 Exp $";
     
    private Hashtable adapterTable = new Hashtable();


    private URL configurationURL;
    private URL digesterRulesURL;
    private String configurationName = "";

    /**
     *	initializes the log and the AppConstants
     * @see nl.nn.adapterframework.util.AppConstants
     */
    public Configuration() {
        log = Logger.getLogger(this.getClass().getName());
        

        //initialize Application Constants
        AppConstants.getInstance();
        
        log.info(VersionInfo());
    }
    public Configuration(URL digesterRulesURL, URL configurationURL) {
        this();
        this.configurationURL = configurationURL;
        this.digesterRulesURL = digesterRulesURL;

    }
    public String getConfigurationName() {
        return configurationName;
    }
    public URL getConfigurationURL() {
        return configurationURL;
    }
    public String getDigesterRulesFileName() {
        return digesterRulesURL.getFile();

    }
    /**
     * get a registered adapter by its name
     * @param name  the adapter to retrieve
     * @return IAdapter
     */
    public IAdapter getRegisteredAdapter(String name) {
        return (IAdapter) adapterTable.get(name);
    }
    //Returns a sorted list of registered adapter names as an <code>Iterator</code>
    public Iterator getRegisteredAdapterNames() {
        SortedSet sortedKeys = new TreeSet(adapterTable.keySet());
        return sortedKeys.iterator();
    }
    /**
     * Utility function
     */

    public void handleAdapter(String action, String adapterName, String receiverName, String commandIssuedBy) {
        if ((action.equalsIgnoreCase("STOPADAPTER")) && (adapterName.equals("**ALL**"))) {
            log.info("Stopping all adapters on request of" + commandIssuedBy);
            this.stopAdapters();
        }
        if ((action.equalsIgnoreCase("STOPADAPTER")) && (!(adapterName.equals("**ALL**")))) {
            log.info("Stopping adapter [" + adapterName + "], on request of" + commandIssuedBy);
            this.getRegisteredAdapter(adapterName).stopRunning();
        }
        if ((action.equalsIgnoreCase("STARTADAPTER")) && (adapterName.equals("**ALL**"))) {
            // for the start option we 'd like to catch the errors
            // therefore the config.startAdapters() is not used
            Iterator keys = this.getRegisteredAdapterNames();
            while (keys.hasNext()) {
                String name = (String) keys.next();
                IAdapter adapter = this.getRegisteredAdapter(name);
                log.info("Starting adapter [" + name + "] on request of" + commandIssuedBy);
                adapter.startRunning();
            }


        }
        if ((action.equalsIgnoreCase("STARTADAPTER")) && (!(adapterName.equals("**ALL**")))) {
            try {
                log.info("Starting adapter [" + adapterName + "] on request of" + commandIssuedBy);
                this.getRegisteredAdapter(adapterName).startRunning();
            } catch (Exception e) {
                log.error(
                        "error in execution of command ["
                        + action
                        + "] for adapter ["
                        + adapterName
                        + "]",
                        e);

                //errors.add("", new ActionError("errors.generic", e.toString()));
            }
        }
        if (action.equalsIgnoreCase("STOPRECEIVER")) {
            IAdapter adapter = (IAdapter) this.getRegisteredAdapter(adapterName);
            IReceiver receiver = adapter.getReceiverByName(receiverName);
            receiver.stopRunning();
            log.info("receiver [" + receiverName + "] stopped by webcontrol on request of " + commandIssuedBy);

        }
        if (action.equalsIgnoreCase("STARTRECEIVER")) {
            IAdapter adapter = (IAdapter) this.getRegisteredAdapter(adapterName);
            IReceiver receiver = adapter.getReceiverByName(receiverName);
            receiver.startRunning();
            log.info("receiver [" + receiverName + "] started by " + commandIssuedBy);

        }

    }
    /**
     * returns wether an adapter is known at the configuration.
     * @param name the Adaptername
     * @return true if the adapter is known at the configuration
     */
    public boolean isRegisteredAdapter(String name){
        return getRegisteredAdapter(name)==null;
    }
    /**
     * @param adapterName the adapter
     * @param receiverName the receiver
     * @return true if the receiver is known at the adapter
     */
    public boolean isRegisteredReceiver(String adapterName, String receiverName){
        IAdapter adapter=getRegisteredAdapter(adapterName);
        if (null==adapter) return false;
        adapter.getReceiverByName(receiverName);
        return adapter.getReceiverByName(receiverName)==null;
    }
    public void listObjects() {

        Enumeration keys = adapterTable.keys();
        while (keys.hasMoreElements()) {
            String name = (String) keys.nextElement();
            log.info(
                    name
                    + ":"
                    + ((IAdapter) adapterTable.get(name)).toString());
        }

    }
    public void registerAdapter(IAdapter adapter) throws ConfigurationException {
        if (null != adapterTable.get(adapter.getName())) {
            throw new ConfigurationException("Adapter [" + adapter.getName() + "] already registered.");
        }
        adapterTable.put(adapter.getName(), adapter);
        log.info("Registering adapter [" + adapter.getName() + "]");

        if (log.isDebugEnabled()) {
            try {
                adapter.configure();
            } catch (Exception e) {
                log.error("error configuring adapter [" + adapter.getName() + "]", e);
            }
        } else
            adapter.configure();

    }
    /**
     * Register an {@link AdapterJob job} for scheduling at the configuration.
     * The configuration will create an {@link AdapterJob AdapterJob} instance and a JobDetail with the
     * information from the parameters, after checking the
     * parameters of the job. (basically, it checks wether the adapter and the
     * receiver are registered.
     * <p>See the <a href="http://quartz.sourceforge.net">Quartz scheduler</a> documentation</p>
     * @param jobdef a JobDef object
     * @see nl.nn.adapterframework.scheduler.JobDef for a description of Cron triggers
     * @since 4.0
     */
    public void registerScheduledJob(JobDef jobdef) {
        SchedulerFactory schedFact = new StdSchedulerFactory();
        try {
            Scheduler sched = schedFact.getScheduler();

            // if there's no such adapter, log an error
            if (this.getRegisteredAdapter(jobdef.getAdapterName()) == null) {
                log.error("Jobdef [" + jobdef.getName() + "] got error: adapter [" + jobdef.getAdapterName() + "] not registered.");
                return;
            }
            if (StringUtils.isNotEmpty(jobdef.getReceiverName())){
                if (! isRegisteredReceiver(jobdef.getAdapterName(), jobdef.getReceiverName())){
                    log.error("Jobdef [" + jobdef.getName() + "] got error: adapter [" + jobdef.getAdapterName() + "] receiver ["+jobdef.getReceiverName()+"] not registered.");
                }
            }
            // if the job already exists, remove it.
            if ((sched.getJobDetail(jobdef.getName(), Scheduler.DEFAULT_GROUP)) != null) {
                try {
                    sched.deleteJob(jobdef.getName(), Scheduler.DEFAULT_GROUP);
                } catch (SchedulerException e) {
                    log.error("error removing job ["+jobdef.getName()+"] from the scheduler", e);
                }
            }
            JobDetail jobDetail = new JobDetail(jobdef.getName(), // job name
                    Scheduler.DEFAULT_GROUP, // job group
                    AdapterJob.class);        // the java class to execute

            jobDetail.getJobDataMap().put("adapterName", jobdef.getAdapterName());
            jobDetail.getJobDataMap().put("config", this);
            jobDetail.getJobDataMap().put("function", jobdef.getFunction());
            jobDetail.getJobDataMap().put("receiverName", jobdef.getReceiverName());
            if (StringUtils.isNotEmpty(jobdef.getDescription()))  jobDetail.setDescription(jobdef.getDescription());

            CronTrigger cronTrigger = new CronTrigger(jobdef.getName(), Scheduler.DEFAULT_GROUP);
            cronTrigger.setCronExpression(jobdef.getCronExpression());
            sched.scheduleJob(jobDetail, cronTrigger);
            log.info("job scheduled with properties :" + jobdef.toString());
        } catch (Exception e) {
            log.error("error occured on registerScheduledJob", e);
        }
    }
    public void setConfigurationName(String name) {
        configurationName = name;
        log.debug("configuration name set to [" + name + "]");
    }
    public void startAdapters() {
        Enumeration keys = adapterTable.keys();
        while (keys.hasMoreElements()) {
            String name = (String) keys.nextElement();
            IAdapter adapter = getRegisteredAdapter(name);
            log.info("Starting adapter " + name);
            adapter.startRunning();
        }

    }
    public void stopAdapters() {
        Enumeration keys = adapterTable.keys();
        while (keys.hasMoreElements()) {
            String name = (String) keys.nextElement();
            IAdapter adapter = getRegisteredAdapter(name);
            log.info("Stopping adapter [" + name + "]");

            adapter.stopRunning();
        }

    }
    public String VersionInfo() {
    	StringBuffer sb=new StringBuffer();
    	sb.append(version+SystemUtils.LINE_SEPARATOR);
    	sb.append(ConfigurationDigester.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.core.IReceiver.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.core.IPullingListener.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.core.Adapter.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.core.IPipe.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.core.PipeLine.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.receivers.JmsReceiver.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.receivers.ServiceDispatcher.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.receivers.ServiceListener.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.receivers.PullingReceiverBase.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.util.AppConstants.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.util.Variant.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.util.XmlUtils.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.pipes.AbstractPipe.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.pipes.MessageSendingPipe.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.pipes.XmlValidator.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.pipes.XmlSwitch.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.errormessageformatters.ErrorMessageFormatter.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.webcontrol.ConfigurationServlet.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.webcontrol.IniDynaActionForm.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.webcontrol.action.ActionBase.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.webcontrol.action.ShowConfiguration.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.webcontrol.action.ShowConfigurationStatus.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.scheduler.SchedulerAdapter.version +SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.extensions.coolgen.CoolGenWrapperPipe.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.ifsa.IfsaClient.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.ifsa.IfsaFacade.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.ifsa.IfsaServiceListener.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.rekenbox.RekenBoxCaller.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.rekenbox.Adios2XmlPipe.version +SystemUtils.LINE_SEPARATOR);
    	return sb.toString();
    	
    }
}
