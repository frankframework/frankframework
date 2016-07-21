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
package nl.nn.adapterframework.configuration;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.cache.IbisCacheManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.statistics.StatisticsKeeperLogger;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.RunStateEnum;

import org.apache.log4j.Logger;

/**
 * The Configuration is placeholder of all configuration objects. Besides that, it provides
 * functions for starting and stopping adapters as a facade.
 *
 * @author Johan Verrips
 * @see    nl.nn.adapterframework.configuration.ConfigurationException
 * @see    nl.nn.adapterframework.core.IAdapter
 */
public class Configuration {
    protected Logger log = LogUtil.getLogger(this);
    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

	private boolean autoStart = AppConstants.getInstance().getBoolean("configurations.autoStart", true);

    private final AdapterService adapterService;

	private final Map jobTable = new Hashtable(); // TODO useless synchronization ?
    private final List<JobDef> scheduledJobs = new ArrayList<JobDef>();

    private URL configurationURL;
    private URL digesterRulesURL;
    private String name;
    private IbisManager ibisManager;
    private String originalConfiguration;
    private String loadedConfiguration;
    private StatisticsKeeperIterationHandler statisticsHandler=null;

    private ConfigurationException configurationException=null;
    private BaseConfigurationWarnings configurationWarnings = new BaseConfigurationWarnings();

    private static Date statisticsMarkDateMain=new Date();
	private static Date statisticsMarkDateDetails=statisticsMarkDateMain;

	public void forEachStatisticsKeeper(StatisticsKeeperIterationHandler hski, Date now, Date mainMark, Date detailMark, int action) throws SenderException {
		Object root = hski.start(now,mainMark,detailMark);
		try {
			Object groupData=hski.openGroup(root,AppConstants.getInstance().getString("instance.name",""),"instance");
			for (Map.Entry<String, IAdapter> entry : adapterService.getAdapters().entrySet()) {
				IAdapter adapter = entry.getValue();
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


    /**
     *	initializes the log and the AppConstants
     * @see nl.nn.adapterframework.util.AppConstants
     */
    public Configuration(AdapterService adapterService) {
         this.adapterService = adapterService;
    }
    public Configuration(URL digesterRulesURL, URL configurationURL) {
        this(new BasicAdapterServiceImpl());
        this.configurationURL = configurationURL;
        this.digesterRulesURL = digesterRulesURL;
    }

	public ClassLoader getClassLoader() {
		return classLoader;
	}

	public void setAutoStart(boolean autoStart) {
		this.autoStart = autoStart;
	}

	public boolean isAutoStart() {
		return autoStart;
	}

    /**
     * get a registered adapter by its name
     * @param name  the adapter to retrieve
     * @return IAdapter
     */
    @Deprecated
    public IAdapter getRegisteredAdapter(String name) {
        return adapterService.getAdapter(name);
    }

    @Deprecated
	public IAdapter getRegisteredAdapter(int index) {
		return getRegisteredAdapters().get(index);
	}

    @Deprecated
	public List<IAdapter> getRegisteredAdapters() {
        return new ArrayList<IAdapter>(adapterService.getAdapters().values());
	}

	public List<String> getSortedStartedAdapterNames() {
		List<String> startedAdapters = new ArrayList<String>();
		for (int i = 0; i < getRegisteredAdapters().size(); i++) {
			IAdapter adapter = getRegisteredAdapter(i);
			// add the adapterName if it is started.
			if (adapter.getRunState().equals(RunStateEnum.STARTED)) {
				startedAdapters.add(adapter.getName());
			}
		}
		Collections.sort(startedAdapters, String.CASE_INSENSITIVE_ORDER);
		return startedAdapters;
	}
    
    //Returns a sorted list of registered adapter names as an <code>Iterator</code>
    @Deprecated
    public Iterator<IAdapter> getRegisteredAdapterNames() {
        return adapterService.getAdapters().values().iterator();
    }
    
    public AdapterService getAdapterService() {
        return adapterService;
    }

    /**
     * @param adapterName the adapter
     * @param receiverName the receiver
     * @return true if the receiver is known at the adapter
     */
    public boolean isRegisteredReceiver(String adapterName, String receiverName){
        IAdapter adapter=getRegisteredAdapter(adapterName);
        if (null ==adapter) {
        	return false;
		}
        return adapter.getReceiverByName(receiverName) != null;
    }

	/**
	 * Register an adapter with the configuration.
	 * @param adapter
	 * @throws ConfigurationException
	 */
	public void registerAdapter(IAdapter adapter) throws ConfigurationException {
		if (adapter instanceof Adapter && !((Adapter)adapter).isActive()) {
			log.debug("adapter [" + adapter.getName() + "] is not active, therefore not included in configuration");
			return;
		}
		adapter.setConfiguration(this);
		adapterService.registerAdapter(adapter);
		log.debug("Configuration [" + name + "] registered adapter [" + adapter.toString() + "]");
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
		jobdef.configure(this);
		jobTable.put(jobdef.getName(), jobdef);
        scheduledJobs.add(jobdef);
    }

	public void registerStatisticsHandler(StatisticsKeeperIterationHandler handler) throws ConfigurationException {
		log.debug("registerStatisticsHandler() registering ["+ClassUtils.nameOf(handler)+"]");
		statisticsHandler=handler;
		handler.configure();
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	/**
	 * @deprecated replaced by setName(String)
	 * @param name
	 */
	public void setConfigurationName(String name) {
		this.name = name;
	}

	/**
	 * @deprecated replaced by getName()
	 * @param name
	 */
	public String getConfigurationName() {
		return name;
	}

	public String getClassLoaderType() {
		return classLoader.getParent().getClass().getSimpleName();
	}

	public void setIbisManager(IbisManager ibisManager) {
		this.ibisManager = ibisManager;
	}

	public IbisManager getIbisManager() {
		return ibisManager;
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
		return (JobDef) jobTable.get(name);
	}

	public JobDef getScheduledJob(int index) {
		return scheduledJobs.get(index);
	}

	public List<JobDef> getScheduledJobs() {
		return scheduledJobs;
	}

	public void setConfigurationException(ConfigurationException exception) {
		configurationException = exception;
	}

	public ConfigurationException getConfigurationException() {
		return configurationException;
	}

	public BaseConfigurationWarnings getConfigurationWarnings() {
		return configurationWarnings;
	}
}
