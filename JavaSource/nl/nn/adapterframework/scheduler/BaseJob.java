package nl.nn.adapterframework.scheduler;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
/**
 * Base class for jobs
 * *
 * @version Id
 
 * @author  Johan Verrips
 * @since 4.0
 * Date: Nov 22, 2003
 * Time: 11:34:01 AM
 * 
 */
public abstract class BaseJob implements Job {
	public static final String version="$Id: BaseJob.java,v 1.3 2004-03-26 10:43:06 NNVZNL01#L180564 Exp $";
	
    Logger log=Logger.getLogger(this.getClass());

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

