package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.AppConstants;
import org.apache.commons.lang.SystemUtils;
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
 * <p>$Id: ShowConfiguration.java,v 1.2 2004-02-04 10:02:09 a1909356#db2admin Exp $</p>
 * @author  Johan Verrips
 * @see nl.nn.adapterframework.configuration.Configuration
 */

public final class ShowConfiguration extends ActionBase {
	public static final String version="$Id: ShowConfiguration.java,v 1.2 2004-02-04 10:02:09 a1909356#db2admin Exp $";
	


public ActionForward execute(
    ActionMapping mapping,
    ActionForm form,
    HttpServletRequest request,
    HttpServletResponse response)
    throws IOException, ServletException {

    // Initialize action
    initAction(request);
	 
    DynaActionForm configurationPropertiesForm = getPersistentForm(mapping, form, request);
	configurationPropertiesForm.set("logLevel", log.getRootLogger().getLevel().toString());
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
        String lineSeperator=SystemUtils.LINE_SEPARATOR;
        if (null==lineSeperator) lineSeperator="\n";
        while ((str = in.readLine()) != null) {
            // str is one line of text; readLine() strips the newline character(s)
            result += str+lineSeperator;
        }
        
        in.close();
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
