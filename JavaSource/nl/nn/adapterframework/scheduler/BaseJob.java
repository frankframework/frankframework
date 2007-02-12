/*
 * $Log: BaseJob.java,v $
 * Revision 1.4  2007-02-12 14:08:01  europe\L190409
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
	public static final String version = "$RCSfile: BaseJob.java,v $ $Revision: 1.4 $ $Date: 2007-02-12 14:08:01 $";
    protected Logger log=LogUtil.getLogger(this);

    public String getLogPrefix(JobExecutionContext context) {
        String instName = context.getJobDetail().getName();
        String instGroup = context.getJobDetail().getGroup();
//        String instDescription=context.getJobDetail().getDescription();

        StringBuffer sb=new StringBuffer();
        sb.append(" jobname ["+instName+"]");
        sb.append(" group ["+instGroup+"]");
//        sb.append(" jobDescription ["+instDescription+"] ");
        return sb.toString();
    }
}

