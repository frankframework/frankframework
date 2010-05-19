/*
 * $Log: ShowConfiguration.java,v $
 * Revision 1.12  2010-05-19 10:31:08  m168309
 * show loaded configuration instead of original configuration
 *
 * Revision 1.11  2008/06/03 16:00:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * encode errorMessages
 *
 * Revision 1.10  2007/08/30 15:12:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified getRootLogger()
 *
 * Revision 1.9  2007/02/16 14:22:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed changing of log-level
 *
 * Revision 1.8  2007/02/12 14:35:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.7  2005/12/29 15:35:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made some steps to following links to included configuration files
 *
 * Revision 1.6  2005/10/17 09:37:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

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
	public static final String version = "$RCSfile: ShowConfiguration.java,v $ $Revision: 1.12 $ $Date: 2010-05-19 10:31:08 $";
	
	private static final String KEYWORD_INCLUDE="<include";
	private static final String KEYWORD_CONFIG="configuration=\"";
	private static final String KEYWORD_QUOTE="\"";
	
	private void checkForInclude(String line, List linklist) {
		if (line==null) {
			return;
		}
		int includePos=line.indexOf(KEYWORD_INCLUDE);
		if (includePos<0) {
			return;
		}
		int configurationStartPos=line.indexOf(KEYWORD_CONFIG,includePos+KEYWORD_INCLUDE.length());
		if (configurationStartPos<0) {
			return;
		}
		configurationStartPos+=KEYWORD_CONFIG.length();
		int configurationEndPos=line.indexOf(KEYWORD_QUOTE,configurationStartPos);

		String configuration=line.substring(configurationStartPos,configurationEndPos);	
		log.debug("configuration found ["+configuration+"]");
		linklist.add(configuration);
	}
	
	public ActionForward execute(
	    ActionMapping mapping,
	    ActionForm form,
	    HttpServletRequest request,
	    HttpServletResponse response)
	    throws IOException, ServletException {
	
	    // Initialize action
	    initAction(request);
		 
	    DynaActionForm configurationPropertiesForm = getPersistentForm(mapping, form, request);
		configurationPropertiesForm.set("logLevel", LogUtil.getRootLogger().getLevel().toString());
		configurationPropertiesForm.set("logIntermediaryResults", new Boolean(false));
		if (AppConstants.getInstance().getResolvedProperty("log.logIntermediaryResults")!=null) {
			if (AppConstants.getInstance().getResolvedProperty("log.logIntermediaryResults").equalsIgnoreCase("true")) {
				configurationPropertiesForm.set("logIntermediaryResults", new Boolean(true));
			}
		}
	     
	     
	    URL configURL = config.getConfigurationURL();
	    String result = "";
	    List linklist = new ArrayList();
	    try {
	
	        // Read all the text returned by the server
	        BufferedReader in =
	            new BufferedReader(new InputStreamReader(configURL.openStream()));
	        String str;
	        String lineSeparator=SystemUtils.LINE_SEPARATOR;
	        if (null==lineSeparator) lineSeparator="\n";
	        while ((str = in.readLine()) != null) {
	            // str is one line of text; readLine() strips the newline character(s)
//				checkForInclude(str,linklist);
	            result += str+lineSeparator;
	        }
	        
	        in.close();
	        try {
	        result=XmlUtils.identityTransform(result);
	        } catch(DomBuilderException e){
	        	log.error(e);
	        }
//	        if (AppConstants.getInstance().getBoolean("showConfiguration.resolve.variables", true))
				result=StringResolver.substVars(result, AppConstants.getInstance());

			result = ConfigurationUtils.getActivatedConfiguration(result);

			if (ConfigurationUtils.stubConfiguration()) {
				result = ConfigurationUtils.getStubbedConfiguration(result);
			}			
	        
	    } catch (MalformedURLException e) {
	        result =
	            "<b>error occured retrieving configurationfile:" + XmlUtils.encodeChars(e.getMessage()) + "</b>";
	    } catch (IOException e) {
	        result =
	            "<b>error occured retrieving configurationfile:" + XmlUtils.encodeChars(e.getMessage()) + "</b>";
		} catch (ConfigurationException e) {
			result =
				"<b>error occured retrieving configurationfile:" + XmlUtils.encodeChars(e.getMessage()) + "</b>";
	    }
			
	    request.setAttribute("configXML", result);
	
	    // Forward control to the specified success URI
	    log.debug("forward to success");
	    return (mapping.findForward("success"));
	
	}
}
