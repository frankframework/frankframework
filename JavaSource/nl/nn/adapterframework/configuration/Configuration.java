/*
 * $Log: Configuration.java,v $
 * Revision 1.35  2009-06-05 07:19:56  L190409
 * support for adapter level only statistics
 * added throws clause to forEachStatisticsKeeperBody()
 * end-processing of statisticskeeperhandler in a finally clause
 *
 * Revision 1.34  2009/03/17 10:29:35  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added getScheduledJob method
 *
 * Revision 1.33  2008/10/23 14:16:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * XSLT 2.0 made possible
 *
 * Revision 1.32  2008/09/04 12:00:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * collect interval statistics
 *
 * Revision 1.31  2008/08/27 15:53:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added statistics dump code
 * added reset option to statisticsdump
 *
 * Revision 1.30  2008/05/15 14:29:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added storage facility for configuration exceptions
 *
 * Revision 1.29  2008/01/29 15:49:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed a class from versions
 *
 * Revision 1.28  2007/10/16 08:40:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed ifsa facade version display
 *
 * Revision 1.27  2007/10/09 15:07:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * copy changes from Ibis-EJB:
 * added formerly static classe appConstants to config 
 * delegate work to IbisManager
 *
 * Revision 1.26  2007/10/08 13:29:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.25  2007/07/24 08:04:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reversed shutdown sequence
 *
 * Revision 1.24  2007/07/17 15:07:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added list of adapters, to access them in order
 *
 * Revision 1.23  2007/06/26 09:35:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * add instance name to log at startup
 *
 * Revision 1.22  2007/05/02 11:22:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute 'active'
 *
 * Revision 1.21  2007/02/26 16:55:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * start scheduler when a job is found in the configuration
 *
 * Revision 1.20  2007/02/21 15:57:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * throw exception if scheduled job not OK
 *
 * Revision 1.19  2007/02/12 13:38:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.18  2005/12/28 08:59:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * replaced application-name by instance-name
 *
 * Revision 1.17  2005/12/28 08:35:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced StatisticsKeeper-iteration
 *
 * Revision 1.16  2005/11/01 08:53:35  John Dekker <john.dekker@ibissource.org>
 * Moved quartz scheduling knowledge to the SchedulerHelper class
 *
 * Revision 1.15  2005/05/31 09:11:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * detailed version info for XML parsers and transformers
 *
 * Revision 1.14  2004/08/23 07:41:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed Pushers to Listeners
 *
 * Revision 1.13  2004/08/09 08:43:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed pushing receiverbase
 *
 * Revision 1.12  2004/07/06 07:06:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added PushingReceiver and Sap-extensions
 *
 * Revision 1.11  2004/06/30 10:01:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified error handling
 *
 * Revision 1.10  2004/06/16 12:34:46  Johan Verrips <johan.verrips@ibissource.org>
 * Added AutoStart functionality on Adapter
 *
 * Revision 1.9  2004/04/23 14:45:36  Johan Verrips <johan.verrips@ibissource.org>
 * added JMX support
 *
 * Revision 1.8  2004/03/30 07:30:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.7  2004/03/26 09:56:43  Johan Verrips <johan.verrips@ibissource.org>
 * Updated javadoc
 *
 */
package nl.nn.adapterframework.configuration;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.HasStatistics;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.util.StatisticsKeeperLogger;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

/**
 * The Configuration is placeholder of all configuration objects. Besides that, it provides
 * functions for starting and stopping adapters as a facade.
 * 
 * @version Id
 * @author Johan Verrips
 * @see    nl.nn.adapterframework.configuration.ConfigurationException
 * @see    nl.nn.adapterframework.core.IAdapter
 */
public class Configuration {
	public static final String version="$RCSfile: Configuration.java,v $ $Revision: 1.35 $ $Date: 2009-06-05 07:19:56 $";
    protected Logger log=LogUtil.getLogger(this); 
     
    private Map adapterTable = new Hashtable();
	private List adapters = new ArrayList();
	private Map jobTable = new Hashtable();
    private List scheduledJobs = new ArrayList();
    
    private URL configurationURL;
    private URL digesterRulesURL;
    private String configurationName = "";
    private boolean enableJMX=false;
    
    private AppConstants appConstants;
    
    private ConfigurationException configurationException=null;
    
    private static Date statisticsMarkDateMain=new Date();
	private static Date statisticsMarkDateDetails=statisticsMarkDateMain;
    
    /**
     *Set JMX extensions as enabled or not. Default is that JMX extensions are NOT enabled.
     * @param enable
     * @since 4.1.1
     */
    public void setEnableJMX(boolean enable){
    	enableJMX=enable;
    }

	/**
	 * Are JMX extensions enabled?
     * @since 4.1.1
	 * @return boolean
	 */    
    public boolean isEnableJMX(){
    	return enableJMX;
    }

	public void forEachStatisticsKeeper(StatisticsKeeperIterationHandler hski, int action) throws SenderException {
		Object root=hski.start();
		try {
			Object groupData=hski.openGroup(root,appConstants.getString("instance.name",""),"instance");
			for (int i=0; i<adapters.size(); i++) {
				IAdapter adapter = getRegisteredAdapter(i);
				adapter.forEachStatisticsKeeperBody(hski,groupData,action);
			}
			hski.closeGroup(groupData);
		} finally {
			hski.end(root);
		}
	}

	public void dumpStatistics(int action) {
		Date now=new Date();
		boolean showDetails=(action==HasStatistics.STATISTICS_ACTION_FULL || 
							 action==HasStatistics.STATISTICS_ACTION_MARK_FULL ||
							 action==HasStatistics.STATISTICS_ACTION_RESET);
		
		StatisticsKeeperLogger skl = new StatisticsKeeperLogger(now,statisticsMarkDateMain, showDetails ?statisticsMarkDateDetails : null);
		try {
			forEachStatisticsKeeper(skl, action);
		} catch (SenderException e) {
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


    /**
     *	initializes the log and the AppConstants
     * @see nl.nn.adapterframework.util.AppConstants
     */
    public Configuration() {
    }
    public Configuration(URL digesterRulesURL, URL configurationURL) {
        this();
        this.configurationURL = configurationURL;
        this.digesterRulesURL = digesterRulesURL;

    }
    protected void init() {
		//Default XSLT processor 1.0, not XSLT 2.0 processor (net.sf.saxon.TransformerFactoryImpl)
		System.setProperty("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");
        log.info(VersionInfo());
    }

    /**
     * get a registered adapter by its name
     * @param name  the adapter to retrieve
     * @return IAdapter
     */
    public IAdapter getRegisteredAdapter(String name) {
        return (IAdapter) adapterTable.get(name);
    }
	public IAdapter getRegisteredAdapter(int index) {
		return (IAdapter) adapters.get(index);
	}

	public List getRegisteredAdapters() {
		return adapters;
	}

    //Returns a sorted list of registered adapter names as an <code>Iterator</code>
    public Iterator getRegisteredAdapterNames() {
        SortedSet sortedKeys = new TreeSet(adapterTable.keySet());
        return sortedKeys.iterator();
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
        if (null==adapter) {
        	return false;
		}
        return adapter.getReceiverByName(receiverName) != null;
    }
    public void listObjects() {
		for (int i=0; i<adapters.size(); i++) {
			IAdapter adapter = getRegisteredAdapter(i);

			log.info(i+") "+ adapter.getName()+ ": "	+ adapter.toString());
        }
    }
    
    /**
     * Register an adapter with the configuration.  If JMX is {@link #setEnableJMX(boolean) enabled},
     * the adapter will be visible and managable as an MBEAN. 
     * @param adapter
     * @throws ConfigurationException
     */
    public void registerAdapter(IAdapter adapter) throws ConfigurationException {
    	if (adapter instanceof Adapter && !((Adapter)adapter).isActive()) {
    		log.debug("adapter [" + adapter.getName() + "] is not active, therefore not included in configuration");
    		return;
    	} 
        if (null != adapterTable.get(adapter.getName())) {
            throw new ConfigurationException("Adapter [" + adapter.getName() + "] already registered.");
        }
        adapterTable.put(adapter.getName(), adapter);
		adapters.add(adapter);
		if (isEnableJMX()) {
			log.debug("Registering adapter [" + adapter.getName() + "] to the JMX server");
	        JmxMbeanHelper.hookupAdapter( (nl.nn.adapterframework.core.Adapter) adapter);
	        log.info ("[" + adapter.getName() + "] registered to the JMX server");
		}
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
    public void registerScheduledJob(JobDef jobdef) throws ConfigurationException {
		jobdef.configure(this);
		jobTable.put(jobdef.getName(), jobdef);
        scheduledJobs.add(jobdef);
    }
    
    
    public String getInstanceInfo() {
		String instanceInfo=appConstants.getProperty("application.name")+" "+
							appConstants.getProperty("application.version")+" "+
							appConstants.getProperty("instance.name")+" "+
							appConstants.getProperty("instance.version")+" ";
		String buildId=	appConstants.getProperty("instance.build_id");
		if (StringUtils.isNotEmpty(buildId)) {
			instanceInfo+=" build "+buildId;						
		}
		return instanceInfo;
    }
    
    public String VersionInfo() {
    	StringBuffer sb=new StringBuffer();
    	sb.append(getInstanceInfo()+SystemUtils.LINE_SEPARATOR);
    	sb.append(version+SystemUtils.LINE_SEPARATOR);
    	sb.append(ConfigurationDigester.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.core.IReceiver.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.core.IPullingListener.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.core.Adapter.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.core.IPipe.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.core.PipeLine.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.receivers.ServiceDispatcher.version+SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.receivers.ReceiverBase.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.util.AppConstants.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.util.Variant.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.pipes.AbstractPipe.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.pipes.MessageSendingPipe.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.pipes.XmlValidator.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.pipes.XmlSwitch.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.errormessageformatters.ErrorMessageFormatter.version+SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.http.HttpSender.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.http.WebServiceSender.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.http.IbisWebServiceSender.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.http.WebServiceListener.version +SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.webcontrol.ConfigurationServlet.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.webcontrol.IniDynaActionForm.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.webcontrol.action.ActionBase.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.webcontrol.action.ShowConfiguration.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.webcontrol.action.ShowConfigurationStatus.version+SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.scheduler.SchedulerAdapter.version +SystemUtils.LINE_SEPARATOR);
    	sb.append(nl.nn.adapterframework.extensions.coolgen.CoolGenWrapperPipe.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.ifsa.IfsaRequesterSender.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.ifsa.IfsaProviderListener.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.rekenbox.RekenBoxCaller.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.rekenbox.Adios2XmlPipe.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.sap.SapFunctionFacade.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.sap.SapListener.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.sap.SapSender.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.extensions.sap.SapSystem.version +SystemUtils.LINE_SEPARATOR);
		sb.append(nl.nn.adapterframework.util.XmlUtils.getVersionInfo());
    	return sb.toString();
    	
    }

	public void setConfigurationName(String name) {
		configurationName = name;
		log.debug("configuration name set to [" + name + "]");
	}
	public String getConfigurationName() {
		return configurationName;
	}


	public void setConfigurationURL(URL url) {
		configurationURL = url;
	}
	public URL getConfigurationURL() {
		return configurationURL;
	}


	public void setDigesterRulesURL(URL url) {
		digesterRulesURL = url;
	}
	public String getDigesterRulesFileName() {
		return digesterRulesURL.getFile();
	}
   

	public JobDef getScheduledJob(String name) {
		return (JobDef) jobTable.get(name);
	}
	public JobDef getScheduledJob(int index) {
		return (JobDef) scheduledJobs.get(index);
	}


	public List getScheduledJobs() {
		return scheduledJobs;
	}

    public void setAppConstants(AppConstants constants) {
        appConstants = constants;
    }
	public AppConstants getAppConstants() {
		return appConstants;
	}


	public void setConfigurationException(ConfigurationException exception) {
		configurationException = exception;
	}
	public ConfigurationException getConfigurationException() {
		return configurationException;
	}

}
