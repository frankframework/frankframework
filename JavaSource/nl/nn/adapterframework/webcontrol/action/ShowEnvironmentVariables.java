/*
 * $Log: ShowEnvironmentVariables.java,v $
 * Revision 1.1  2005-10-26 12:52:31  europe\L190409
 * added ShowEnvironmentVariables to console
 *
 */

package nl.nn.adapterframework.webcontrol.action;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Properties;

import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Shows the environment variables.
 * 
 * @author  Peter Leeuwenburgh
 * @version Id 
 * @since	4.4
 */

public class ShowEnvironmentVariables extends ActionBase {
	public static final String version = "$RCSfile: ShowEnvironmentVariables.java,v $ $Revision: 1.1 $ $Date: 2005-10-26 12:52:31 $";

	public void addPropertiesToXmlBuilder(XmlBuilder container, Properties props, String setName) {
		Enumeration enum = props.keys();
		XmlBuilder propertySet = new XmlBuilder("propertySet");
		propertySet.addAttribute("name", setName);
		container.addSubElement(propertySet);

		while (enum.hasMoreElements()) {
			String propName = (String) enum.nextElement();
			XmlBuilder property = new XmlBuilder("property");
			property.addAttribute("name", propName);
			property.setCdataValue(
				XmlUtils.encodeCdataString(props.getProperty(propName)));
			propertySet.addSubElement(property);
		}

	}
	
	public Properties getEnvironmentVariables() {
		BufferedReader br = null;
		try {
			Process p = null;
			Runtime r = Runtime.getRuntime();
			String OS = System.getProperty("os.name").toLowerCase();
			if (OS.indexOf("windows 9") > -1) {
				p = r.exec("command.com /c set");
			} else if (
				(OS.indexOf("nt") > -1)
					|| (OS.indexOf("windows 20") > -1)
					|| (OS.indexOf("windows xp") > -1)) {
				p = r.exec("cmd.exe /c set");
			} else {
				//assume Unix
				p = r.exec("env");
			}
			Properties props=new Properties();
//			props.load(p.getInputStream()); // this does not work, due to potential malformed escape sequences
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				int idx = line.indexOf('=');
				String key = line.substring(0, idx);
				String value = line.substring(idx + 1);
				props.setProperty(key,value);
			}
			return props;
		} catch (Exception e) {
			log.warn("Error while retrieving environment variables: ", e);
			return null;
		}
	}

	public ActionForward execute(
		ActionMapping mapping,
		ActionForm form,
		HttpServletRequest request,
		HttpServletResponse response)
		throws IOException, ServletException {
		// Initialize action
		initAction(request);
		// Retrieve environment variables for browsing

		XmlBuilder envVars = new XmlBuilder("environmentVariables");

		addPropertiesToXmlBuilder(envVars,AppConstants.getInstance(),"Application Constants");
		addPropertiesToXmlBuilder(envVars,System.getProperties(),"System Properties");
		
		Properties envVarProps=getEnvironmentVariables();
		if (envVars!=null) {
			addPropertiesToXmlBuilder(envVars,envVarProps,"Environment Variables");
		}

		log.debug("envVars: [" + envVars.toXML() + "]");
		request.setAttribute("envVars", envVars.toXML());

		// Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));

	}
}
