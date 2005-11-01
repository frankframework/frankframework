/*
 * $Log: ServiceJob.java,v $
 * Revision 1.1  2005-11-01 08:51:14  europe\m00f531
 * Add support for dynamic scheduling, i.e. add a job to the scheduler using 
 * a sender
 *
 */
package nl.nn.adapterframework.scheduler;

import nl.nn.adapterframework.pipes.IbisLocalSender;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * Job for sending messages to a javalistener.
 * The <a href="http://quartz.sourceforge.net">Quartz scheduler</a> is used for scheduling.
 * <p>
 * Job is registered at runtime by the SchedulerSender
 * 
 * @author John Dekker
 */
public class ServiceJob extends BaseJob {
	public static final String version="$RCSfile: ServiceJob.java,v $ $Revision: 1.1 $ $Date: 2005-11-01 08:51:14 $";

	public ServiceJob() {
		super();
	}
	
	public void execute(JobExecutionContext context) throws JobExecutionException {
		try {
			log.info("executing" + getLogPrefix(context));
			JobDataMap dataMap = context.getJobDetail().getJobDataMap();
			String serviceName = dataMap.getString(SchedulerSender.JAVALISTENER);
			String message = dataMap.getString(SchedulerSender.MESSAGE);
			String correlationId = dataMap.getString(SchedulerSender.CORRELATIONID);
			
			// send job
			IbisLocalSender localSender = new IbisLocalSender();
			localSender.setJavaListener(serviceName);
			localSender.setIsolated(false);
			localSender.setName("ServiceJob");
			localSender.configure();
			
			localSender.open();
			try {
				localSender.sendMessage(correlationId, message);
			}
			finally {
				localSender.close();
			}
		}
		catch (Exception e) {
			log.error(e);
			throw new JobExecutionException(e, false);
		}
		log.debug(getLogPrefix(context) + "completed");
	}

}
