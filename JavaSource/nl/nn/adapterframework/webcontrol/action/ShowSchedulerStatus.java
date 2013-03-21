/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
/*
 * $Log: ShowSchedulerStatus.java,v $
 * Revision 1.8  2011-11-30 13:51:46  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.6  2009/03/17 10:35:23  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * *** empty log message ***
 *
 * Revision 1.5  2008/08/27 16:30:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * the good old version string
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.scheduler.SchedulerAdapter;
import nl.nn.adapterframework.scheduler.SchedulerHelper;
import nl.nn.adapterframework.unmanaged.DefaultIbisManager;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

/**
 * Retrieves the Scheduler metadata and the jobgroups with there jobs
 * from the Scheduler.
 * @version $Id$
 * @author  Johan Verrips
 */

public final class ShowSchedulerStatus extends ActionBase {

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {


        // Initialize action
        initAction(request);

		if (ibisManager==null) {
			error("Cannot find ibismanager",null);
			return null;
		}
	
		// TODO Dit moet natuurlijk netter...
		DefaultIbisManager manager = (DefaultIbisManager)ibisManager;
		SchedulerHelper sh = manager.getSchedulerHelper();

		SchedulerAdapter schedulerAdapter = new SchedulerAdapter();
		Scheduler scheduler;
		try {
			scheduler = sh.getScheduler();
		} catch (SchedulerException e) {
			error("Cannot find scheduler",e);
			return null;
		}


        SchedulerAdapter sa=new SchedulerAdapter();

		if (log.isDebugEnabled()) {
			log.debug("set metadata ["+sa.getSchedulerMetaDataToXml(scheduler).toXML()+"]");
			log.debug("set jobdata ["+sa.getJobGroupNamesWithJobsToXml(scheduler, config).toXML()+"]");
		}
	    request.setAttribute("metadata", sa.getSchedulerMetaDataToXml(scheduler).toXML());
        request.setAttribute("jobdata", sa.getJobGroupNamesWithJobsToXml(scheduler, config).toXML());


        // Forward control to the specified success URI
        log.debug("forward to success");
        return (mapping.findForward("success"));

    }
}
