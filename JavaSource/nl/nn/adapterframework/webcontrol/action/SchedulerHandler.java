package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.scheduler.SchedulerAdapter;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.quartz.Scheduler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * @version Id
 * @author  Johan
 * Date: Nov 28, 2003
 * Time: 2:48:31 PM
 */
public class SchedulerHandler extends ActionBase {
	public static final String version="$Id: SchedulerHandler.java,v 1.3 2004-03-26 10:42:57 NNVZNL01#L180564 Exp $";
	
public ActionForward execute(
    ActionMapping mapping,
    ActionForm form,
    HttpServletRequest request,
    HttpServletResponse response)
    throws IOException, ServletException {
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
                    log.info(
                        "delete job jobName ["
                            + jobName
                            + "] groupName ["
                            + groupName
                            + "] "
                            + getCommandIssuedBy(request));
                    scheduler.deleteJob(jobName, groupName);
                } else
                    if (action.equalsIgnoreCase("triggerJob")) {
                        log.info(
                            "trigger job jobName ["
                                + jobName
                                + "] groupName ["
                                + groupName
                                + "] "
                                + getCommandIssuedBy(request));
                        scheduler.triggerJob(jobName, groupName);
                    } else {
                        log.error("no valid argument for SchedulerHandler:" + action);
                    } 

    } catch (org.quartz.SchedulerException se) {
        errors.add("", new ActionError("errors.generic", se.toString()));
        log.error(se.toString());
    } catch (Exception e) {
        errors.add("", new ActionError("errors.generic", e.toString()));
        log.error(e.toString());
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
