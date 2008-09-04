/*
 * $Log: ConfiguredJob.java,v $
 * Revision 1.1  2008-09-04 13:27:26  europe\L190409
 * restructured job scheduling
 *
 * Revision 1.7  2008/08/27 16:21:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added function dumpStatistics
 *
 * Revision 1.6  2007/12/12 09:09:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allow for query-type jobs
 *
 * Revision 1.5  2007/10/10 09:40:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * version using IbisManager
 *
 * Revision 1.4  2007/02/21 16:02:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 */
package nl.nn.adapterframework.scheduler;

import nl.nn.adapterframework.configuration.IbisManager;

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
public class ConfiguredJob extends BaseJob implements Job  {


	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			JobDataMap dataMap = context.getJobDetail().getJobDataMap();
			IbisManager ibisManager = (IbisManager)dataMap.get("manager");
			JobDef jobDef = (JobDef)dataMap.get("jobdef");
			log.info(getLogPrefix(jobDef) + "executing");
			jobDef.executeJob(ibisManager);
			log.debug(getLogPrefix(jobDef) + "completed");
		} catch (Exception e) {
			log.error(e);
			throw new JobExecutionException(e, false);
		}
	}
	
}
