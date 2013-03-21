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
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.monitoring.EventTypeEnum;
import nl.nn.adapterframework.monitoring.Monitor;
import nl.nn.adapterframework.monitoring.MonitorException;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.monitoring.SeverityEnum;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Lock;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

/**
 * Show all monitors.
 * 
 * @author	Gerrit van Brakel
 * @since   4.9
 * @version $Id$
 */
public class ShowMonitors extends ActionBase {

	protected String performAction(DynaActionForm monitorForm, String action, int index, int triggerIndex, HttpServletResponse response) throws MonitorException {
		log.debug("should performing action ["+action+"] on monitorName nr ["+index+"]");
		return null;
	}

	public String determineExitForward(DynaActionForm monitorForm) {
		return "success";
	}

	public void initForm(DynaActionForm monitorForm) {
		MonitorManager mm = MonitorManager.getInstance();

		monitorForm.set("monitorManager",mm);
		List destinations=new ArrayList();
		for (int i=0;i<mm.getMonitors().size();i++) {
			Monitor m=mm.getMonitor(i);
			Set d=m.getDestinationSet();
			for (Iterator it=d.iterator();it.hasNext();) {
				destinations.add(i+","+it.next());				
			}
		}
		String[] selDest=new String[destinations.size()];
		selDest=(String[])destinations.toArray(selDest);
		monitorForm.set("selDestinations",selDest);
		monitorForm.set("enabled",new Boolean(mm.isEnabled()));
		monitorForm.set("eventTypes",EventTypeEnum.getEnumList());
		monitorForm.set("severities",SeverityEnum.getEnumList());
	}

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		// Initialize action
		initAction(request);

		if (null == config) {
			return (mapping.findForward("noconfig"));
		}

		String forward=null;
		DynaActionForm monitorForm = getPersistentForm(mapping, form, request);

		if (isCancelled(request)) {
			log.debug("edit is canceled");
			forward=determineExitForward(monitorForm);
		} 
		else { 

			//debugFormData(request,form);

			String action 	 = request.getParameter("action");
			String indexStr  = request.getParameter("index");
			String triggerIndexStr = request.getParameter("triggerIndex");
			int index=-1;
			if (StringUtils.isNotEmpty(indexStr)) {
				index=Integer.parseInt(indexStr);
			}
			int triggerIndex=-1;
			if (StringUtils.isNotEmpty(triggerIndexStr)) {
				triggerIndex=Integer.parseInt(triggerIndexStr);
			}
		
			MonitorManager mm = MonitorManager.getInstance();
			if ("getStatus".equals(action)) {
				response.setContentType("text/xml");
				PrintWriter out = response.getWriter();
				out.print(mm.getStatusXml().toXML());
				out.close();
				return null;
			} 
			Lock lock = mm.getStructureLock();
			try {
				lock.acquireExclusive();
				forward=performAction(monitorForm, action, index, triggerIndex, response);
				log.debug("forward ["+forward+"] returned from performAction");
				mm.reconfigure();
			} catch (Exception e) {
				error("could not perform action ["+action+"] on monitorIndex ["+index+"] triggerIndex ["+triggerIndex+"]", e);
			} finally {
				lock.releaseExclusive();
			}
			if (response.isCommitted()) {
				return null;
			}
		}	
		if (StringUtils.isEmpty(forward)) {
			log.debug("replacing empty forward with [success]");
			forward="success";
		}
		
		
		initForm(monitorForm);
		
		ActionForward af=mapping.findForward(forward);
		if (af==null) {
			throw new ServletException("could not find forward ["+forward+"]");
		}
		// Forward control to the specified success URI
		log.debug("forward to ["+forward+"], path ["+af.getPath()+"]");
		return (af);
	}

	public void debugFormData(HttpServletRequest request, ActionForm form) {
		for (Enumeration enumeration=request.getParameterNames();enumeration.hasMoreElements();) {
			String name=(String)enumeration.nextElement();
			String[] values=request.getParameterValues(name);
			if (values.length==1) {
				log.debug("Parameter ["+name+"] value ["+values[0]+"]");
			} else {
				for (int i=0;i<values.length;i++) {
					log.debug("Parameter ["+name+"]["+i+"] value ["+values[i]+"]");
				}
			}
		}
		if (form instanceof DynaActionForm) {
			DynaActionForm daf=(DynaActionForm)form;
			log.debug("class ["+daf.getDynaClass().getName()+"]");
			for (Iterator it=daf.getMap().keySet().iterator();it.hasNext();) {
				String key=(String)it.next();
				Object value=daf.get(key);
				log.debug("key ["+key+"] class ["+ClassUtils.nameOf(value)+"] value ["+value+"]");
				if (value!=null) {
					if (value instanceof Monitor) {
						Monitor monitor=(Monitor)value;
						log.debug("Monitor :"+monitor.toXml().toXML());		
					}
				}
			}
		}
	}

}
