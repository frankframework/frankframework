/*
 * $Log: BaseJob.java,v $
 * Revision 1.5  2008-09-04 13:27:27  europe\L190409
 * restructured job scheduling
 *
 * Revision 1.4  2007/02/12 14:08:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 */
package nl.nn.adapterframework.scheduler;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
/**
 * Base class for jobs.
 * 
 * @author  Johan Verrips
 * @since   4.0
 * @version Id
 */
public abstract class BaseJob implements Job {
    protected Logger log=LogUtil.getLogger(this);

	public String getLogPrefix(JobExecutionContext context) {
		String instName = context.getJobDetail().getName();
		return "Job ["+instName+"] ";
	}

	public String getLogPrefix(JobDef jobDef) {
		String instName = jobDef.getName();
		return "Job ["+instName+"] ";
	}
}

