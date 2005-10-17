/*
 * $Log: ShowConfiguration.java,v $
 * Revision 1.6  2005-10-17 09:37:31  europe\L190409
 * added ToDo
 *
 * Revision 1.5  2004/06/16 13:07:41  Johan Verrips <johan.verrips@ibissource.org>
 * Added identity transform functionality to resolve entities
 *
 * Revision 1.4  2004/03/26 10:42:58  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.3  2004/03/23 17:02:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected some typos and solved some warnings
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Shows the configuration (with resolved variables).
 * <p>If the property <code>showConfiguration.resolve.variables</code>,  in
 * {@link nl.nn.adapterframework.util.AppConstants AppConstants} is <code>true</code>
 * the variables (${variable}) in the configuration.xml are resolved. </p>
 * <p>For security-reasons you might set this value to <code>false</code>, so that passwords
 * configured in the <code>environment entries</code> of the application server are not revealed.</p>
 * 
 * @version Id
 * @author  Johan Verrips
 * @see nl.nn.adapterframework.configuration.Configuration
 */

public final class ShowConfiguration extends ActionBase {
	public static final String version = "$RCSfile: ShowConfiguration.java,v $ $Revision: 1.6 $ $Date: 2005-10-17 09:37:31 $";
	
	public ActionForward execute(
	    ActionMapping mapping,
	    ActionForm form,
	    HttpServletRequest request,
	    HttpServletResponse response)
	    throws IOException, ServletException {
	
	    // Initialize action
	    initAction(request);
		 
	    DynaActionForm configurationPropertiesForm = getPersistentForm(mapping, form, request);
		configurationPropertiesForm.set("logLevel", Logger.getRootLogger().getLevel().toString());
		configurationPropertiesForm.set("logIntermediaryResults", new Boolean(false));
		if (AppConstants.getInstance().getResolvedProperty("log.logIntermediaryResults")!=null) {
			if (AppConstants.getInstance().getResolvedProperty("log.logIntermediaryResults").equalsIgnoreCase("true")) {
				configurationPropertiesForm.set("logIntermediaryResults", new Boolean(true));
			}
		}
	     
	    URL configURL = config.getConfigurationURL();
	    String result = "";
	    try {
	
	        // Read all the text returned by the server
	        BufferedReader in =
	            new BufferedReader(new InputStreamReader(configURL.openStream()));
	        String str;
	        String lineSeparator=SystemUtils.LINE_SEPARATOR;
	        if (null==lineSeparator) lineSeparator="\n";
	        while ((str = in.readLine()) != null) {
	            // str is one line of text; readLine() strips the newline character(s)
	            result += str+lineSeparator;
	            //TODO replace include statements with links to the acutal resource
	        }
	        
	        in.close();
	        try {
	        result=XmlUtils.identityTransform(result);
	        } catch(DomBuilderException e){
	        	log.error(e);
	        }
	        if (AppConstants.getInstance().getBoolean("showConfiguration.resolve.variables", true))
				result=StringResolver.substVars(result, AppConstants.getInstance());
	        
	    } catch (MalformedURLException e) {
	        result =
	            "<b>error occured retrieving configurationfile:" + e.getMessage() + "</b>";
	    } catch (IOException e) {
	        result =
	            "<b>error occured retrieving configurationfile:" + e.getMessage() + "</b>";
	    }
			
	    request.setAttribute("configXML", result);
	
	    // Forward control to the specified success URI
	    log.debug("forward to success");
	    return (mapping.findForward("success"));
	
	}
}
