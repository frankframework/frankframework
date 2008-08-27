/*
 * $Log: AdapterJob.java,v $
 * Revision 1.7  2008-08-27 16:21:26  europe\L190409
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
import nl.nn.adapterframework.jdbc.DirectQuerySender;

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
	public static final String version = "$RCSfile: AdapterJob.java,v $ $Revision: 1.7 $ $Date: 2008-08-27 16:21:26 $";

	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			log.info("executing" + getLogPrefix(context));
			JobDataMap dataMap = context.getJobDetail().getJobDataMap();
			// TODO: Put correct manager into the dataMap
			IbisManager ibisManager = (IbisManager)dataMap.get("manager");
			String function = dataMap.getString("function");

			if (function.equalsIgnoreCase("EXECUTEQUERY")) {
				try {
					String query = dataMap.getString("query");
					String jmsRealm = dataMap.getString("jmsRealm");

					DirectQuerySender qs = new DirectQuerySender();
					try {
						qs.setName("QuerySender");
						qs.setJmsRealm(jmsRealm);
						qs.setQueryType("other");
						qs.configure();
						qs.open();
						String result = qs.sendMessage("dummy", query);
						log.info("result [" + result + "]");
					} catch (Exception e) {
						log.error("Error while executing query (as part of scheduled job execution)", e);
					} finally {
						qs.close();
					}
				} catch (Exception e) {
					log.error("Error while creating or closing connection (as part of scheduled job execution)", e);
				}
			} else if (function.equalsIgnoreCase("dumpStatistics") || function.equalsIgnoreCase("dumpStatisticsAndReset")) {
				ibisManager.getConfiguration().dumpStatistics(function.equalsIgnoreCase("dumpStatisticsAndReset"));
			} else {
				String adapterName = dataMap.getString("adapterName");
				String receiverName = dataMap.getString("receiverName");

				ibisManager.handleAdapter(function, adapterName, receiverName, " scheduled job" + getLogPrefix(context));
			}

		} catch (Exception e) {
			log.error(e);
			throw new JobExecutionException(e, false);
		}
		log.debug(getLogPrefix(context) + "completed");
	}
}
