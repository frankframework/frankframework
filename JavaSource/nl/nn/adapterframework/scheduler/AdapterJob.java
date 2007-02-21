/*
 * $Log: AdapterJob.java,v $
 * Revision 1.4  2007-02-21 16:02:46  europe\L190409
 * updated javadoc
 *
 */
package nl.nn.adapterframework.scheduler;

import nl.nn.adapterframework.configuration.Configuration;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;



/**
 * Job, specified in Configuration.xml, for executing things to do with an adapter, like starting or stopping it.
 * The <a href="http://quartz.sourceforge.net">Quartz scheduler</a> is used for scheduling.
 * <p>
 * Expects a JobDetail with a datamap with the following fields:
 * <ul>
 * <li>function: the function to do, possible values:  "startreceiver","stopadapter",  "stopreceiver" and "stopadapter"</li>
 * <li>config: the Configuration object</li>
 * <li>adapterName: the name of the adapter<li>
 * <li>receiverName: the name of the receiver<li>
 * </ul>
 *<p><b>Design consideration</b></p>
 * <p>Currently, the {@link nl.nn.adapterframework.configuration.Configuration configuration}
 * is stored in the job data map. As the configuration is not serializable, due to the nature of the
 * adapters, the quartz database support cannot be used.
 * </p>
 * @version Id
 *
 * @author  Johan Verrips
 * @see nl.nn.adapterframework.core.IAdapter
 * @see nl.nn.adapterframework.configuration.Configuration
  */
public class AdapterJob extends BaseJob implements Job  {
	public static final String version="$RCSfile: AdapterJob.java,v $ $Revision: 1.4 $ $Date: 2007-02-21 16:02:46 $";
	
    public AdapterJob() {
            super();
     }
     public void execute(JobExecutionContext context)
       throws JobExecutionException
     {
         try {
             log.info("executing"+getLogPrefix(context));
             JobDataMap dataMap = context.getJobDetail().getJobDataMap();
             Configuration config=(Configuration) dataMap.get("config");
             String adapterName  = dataMap.getString("adapterName");
             String receiverName = dataMap.getString("receiverName");
             String function = dataMap.getString("function");
             config.handleAdapter(function, adapterName, receiverName, " scheduled job"+getLogPrefix(context));

         } catch (Exception e) {
            log.error (e);
             throw new JobExecutionException (e, false);
         }
         log.debug(getLogPrefix(context)+"completed");
     }
}
