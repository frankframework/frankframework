/*
 * $Log: SchedulerHandler.java,v $
 * Revision 1.5  2008-05-22 07:39:14  europe\L190409
 * removed version string
 *
 * Revision 1.4  2008/05/22 07:38:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use inherited error() method
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.scheduler.SchedulerAdapter;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.quartz.Scheduler;

/**
 * @version Id
 * @author  Johan Verrips
 */
public class SchedulerHandler extends ActionBase {
	
	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
	    // Extract attributes we will need
	    initAction(request);
	
	    String action = request.getParameter("action");
	    if (null == action)
	        action = mapping.getParameter();
	    String jobName = request.getParameter("jobName");
	    String groupName = request.getParameter("groupName");
	
	    SchedulerAdapter schedulerAdapter = new SchedulerAdapter();
	    Scheduler scheduler = schedulerAdapter.getTheScheduler();
	    try {
	        if (action.equalsIgnoreCase("startScheduler")) {
	            log.info("start scheduler:" + new Date() + getCommandIssuedBy(request));
	            scheduler.start();
	        } else
	            if (action.equalsIgnoreCase("pauseScheduler")) {
	                log.info("pause scheduler:" + new Date() + getCommandIssuedBy(request));
	                scheduler.pause();
	            } else
	                if (action.equalsIgnoreCase("deleteJob")) {
	                    log.info("delete job jobName [" + jobName
	                            + "] groupName [" + groupName
	                            + "] " + getCommandIssuedBy(request));
	                    scheduler.deleteJob(jobName, groupName);
	                } else
	                    if (action.equalsIgnoreCase("triggerJob")) {
	                        log.info("trigger job jobName [" + jobName
	                                + "] groupName [" + groupName
	                                + "] " + getCommandIssuedBy(request));
	                        scheduler.triggerJob(jobName, groupName);
	                    } else {
	                        log.error("no valid argument for SchedulerHandler:" + action);
	                    } 
	
	    } catch (Exception e) {
	        error("",e);
	    }
	
	    // Report any errors
	    if (!errors.isEmpty()) {
	        saveErrors(request, errors);
	    }
	
	    // Remove the obsolete form bean
	    if (mapping.getAttribute() != null) {
	        if ("request".equals(mapping.getScope()))
	            request.removeAttribute(mapping.getAttribute());
	        else
	            session.removeAttribute(mapping.getAttribute());
	    }
	
	    // Forward control to the specified success URI
	    return (mapping.findForward("success"));
	}

}
