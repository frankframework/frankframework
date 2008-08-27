/*
 * $Log: ShowMonitorExecute.java,v $
 * Revision 1.3  2008-08-27 16:28:19  europe\L190409
 * added stateChangeDate
 *
 * Revision 1.2  2008/08/07 11:32:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework
 *
 * Revision 1.1  2008/07/24 12:42:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework of monitoring
 *
 * Revision 1.2  2008/07/17 16:21:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * work in progess
 *
 * Revision 1.1  2008/07/14 17:29:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for flexibile monitoring
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.monitoring.Monitor;
import nl.nn.adapterframework.monitoring.MonitorException;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.monitoring.SeverityEnum;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.digester.Digester;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.upload.FormFile;


/**
 * Extension to transactionalstorage browser, that enables delete and repost.
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since   4.3
 */
public class ShowMonitorExecute extends ShowMonitors {
    
	protected String performAction(DynaActionForm monitorForm, String action, int index, int triggerIndex, HttpServletResponse response) throws MonitorException {
		log.debug("performing action ["+action+"] on monitorName nr ["+index+"]");
		MonitorManager mm = MonitorManager.getInstance();
		if (StringUtils.isEmpty(action)) {
			log.warn("monitorHandler did not find action");
			return null;
		}
		if (action.equals("edit")) {
			FormFile form_file=(FormFile) monitorForm.get("configFile");
	
			if (form_file!=null && form_file.getFileSize()>0) {
				log.debug("Upload of file ["+form_file.getFileName()+"] ContentType["+form_file.getContentType()+"]");
				Digester d=new Digester();
				mm.setDigesterRules(d);
				mm.getMonitors().clear();
				d.push(mm);
				try {
					d.parse(form_file.getInputStream());
				} catch (Exception e) {
					error("cannot parse file ["+form_file.getFileName()+"]",e);
				}
			} else {
				mm.updateDestinations((String[])monitorForm.get("selDestinations"));
			}
			mm.setEnabled(((Boolean)monitorForm.get("enabled")).booleanValue());
			return null;
		}
		if (action.equals("createMonitor")) {
			Monitor monitor=new Monitor();
			int i=1;
			while (mm.findMonitor("monitor "+i)!=null) {
				i++;
			}
			monitor.setName("monitor "+i);
			mm.addMonitor(monitor);
			return null;
		}
		if (action.equals("deleteMonitor")) {
			Monitor monitor=mm.getMonitor(index);
			if (monitor!=null) {
				log.info("removing monitor nr ["+index+"] name ["+monitor.getName()+"]");
				mm.removeMonitor(index);
			}
			return null;
		}
		if (action.equals("clearMonitor")) {
			Monitor monitor=mm.getMonitor(index);
			if (monitor!=null) {
				log.info("clearing monitor ["+monitor.getName()+"]");
				monitor.changeState(new Date(),false,SeverityEnum.WARNING,null,null,null);
			}
			return null;
		}
		if (action.equals("raiseMonitor")) {
			Monitor monitor=mm.getMonitor(index);
			if (monitor!=null) {
				log.info("raising monitor ["+monitor.getName()+"]");
				monitor.changeState(new Date(),true,SeverityEnum.WARNING,null,null,null);
			}
			return null;
		}
		if (action.equals("exportConfig")) {
			try {
				response.setContentType("text/xml; charset="+Misc.DEFAULT_INPUT_STREAM_ENCODING);
				response.setHeader("Content-Disposition","attachment; filename=\"monitorConfig-"+AppConstants.getInstance().getProperty("instance.name","")+".xml\"");
				PrintWriter writer=response.getWriter();

				XmlBuilder config = mm.toXml();
				writer.print(config.toXML());
				writer.close();
			} catch (IOException e) {
				error("could not export config",e);
			}
			return null;
		}
		log.debug("should performing action ["+action+"]");
		return null;
	}
}
