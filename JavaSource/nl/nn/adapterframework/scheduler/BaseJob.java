package nl.nn.adapterframework.scheduler;

import org.apache.log4j.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
/**
 * Base class for jobs
 * @author  Johan Verrips
 * @since 4.0
 * Date: Nov 22, 2003
 * Time: 11:34:01 AM
 * 
 */
public abstract class BaseJob implements Job {
	public static final String version="$Id: BaseJob.java,v 1.1 2004-02-04 08:36:21 a1909356#db2admin Exp $";
	
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

