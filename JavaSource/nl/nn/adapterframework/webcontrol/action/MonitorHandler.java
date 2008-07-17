/*
 * $Log: MonitorHandler.java,v $
 * Revision 1.2  2008-07-17 16:21:49  europe\L190409
 * work in progess
 *
 * Revision 1.1  2008/07/14 17:29:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for flexibile monitoring
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.monitoring.Monitor;
import nl.nn.adapterframework.monitoring.MonitorException;
import nl.nn.adapterframework.monitoring.MonitorManager;
import nl.nn.adapterframework.monitoring.SeverityEnum;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.StringUtils;


/**
 * Extension to transactionalstorage browser, that enables delete and repost.
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since   4.3
 */
public class MonitorHandler extends ShowMonitors {
    
	protected void performAction(String action, int index, HttpServletResponse response) throws MonitorException {
		log.debug("performing action ["+action+"] on monitorName nr ["+index+"]");
		MonitorManager mm = MonitorManager.getInstance();
		if (StringUtils.isEmpty(action)) {
			log.warn("monitorHandler did not find action");
			return;
		}
		if (action.equals("createMonitor")) {
			Monitor monitor=new Monitor();
			int i=1;
			while (mm.findMonitor("monitor "+i)!=null) {
				i++;
			}
			monitor.setName("monitor "+i);
			mm.addMonitor(monitor);
			return;
		}
		if (action.equals("deleteMonitor")) {
			Monitor monitor=mm.getMonitor(index);
			if (monitor!=null) {
				log.info("removing monitor nr ["+index+"] name ["+monitor.getName()+"]");
				mm.removeMonitor(index);
			}
			return;
		}
		if (action.equals("clearMonitor")) {
			Monitor monitor=mm.getMonitor(index);
			if (monitor!=null) {
				log.info("clearing monitor ["+monitor.getName()+"]");
				monitor.changeState(false,SeverityEnum.WARNING,null,null,null);
			}
			return;
		}
		if (action.equals("raiseMonitor")) {
			Monitor monitor=mm.getMonitor(index);
			if (monitor!=null) {
				log.info("raising monitor ["+monitor.getName()+"]");
				monitor.changeState(true,SeverityEnum.WARNING,null,null,null);
			}
			return;
		}
		if (action.equals("exportConfig")) {
			try {
				response.setContentType("text/xml; charset="+Misc.DEFAULT_INPUT_STREAM_ENCODING);
				response.setHeader("Content-Disposition","attachment; filename=\"monitorConfig-"+AppConstants.getInstance().getProperty("instance.name","")+".xml\"");
				PrintWriter writer=response.getWriter();

				XmlBuilder config = mm.toXml(true);
				writer.print(config.toXML());
				writer.close();
			} catch (IOException e) {
				error("could not export config",e);
			}
			return;
		}
		log.debug("should performing action ["+action+"]");
	}
}
