package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.scheduler.SchedulerAdapter;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Retrieves the Scheduler metadata and the jobgroups with there jobs
 * from the Scheduler.
 * @version Id
 * @author  Johan Verrips
 */

public final class ShowSchedulerStatus extends ActionBase {
	public static final String version="$Id: ShowSchedulerStatus.java,v 1.3 2004-03-26 10:42:57 NNVZNL01#L180564 Exp $";
	


    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {


        // Initialize action
        initAction(request);

        SchedulerAdapter scheduler=new SchedulerAdapter();

	    request.setAttribute("metadata", scheduler.getSchedulerMetaDataToXml());
        request.setAttribute("jobdata", scheduler.getJobGroupNamesWithJobsToXml());



        // Forward control to the specified success URI
        log.debug("forward to success");
        return (mapping.findForward("success"));

    }
}
