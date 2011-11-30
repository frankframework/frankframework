/*
 * $Log: ShowLogging.java,v $
 * Revision 1.7  2011-11-30 13:51:46  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.5  2011/02/14 12:55:57  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * show error message when error occurs
 *
 * Revision 1.4  2005/10/20 15:28:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added option to show directories
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Dir2Xml;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Shows the logging files.
 * <p>Retrieves the values <code>logging.path</code>
 * and <code>logging.wildcard</code> to perform a directory listing
 * that will be shown in the jsp. These values should be stored 
 * in the deployment specific properties file, as the container
 * may override the output of logging.</p>
 * <p>the logging.path variable may be a system variable, e.g.
 * <code><pre>
 * logging.path=${log.dir}
 * <pre></code>
 * </p>
 * Creation date: (26-02-2003 12:42:00)
 * @version Id
 * @author Johan Verrips IOS
 */
public class ShowLogging extends ActionBase {
	public static final String version="$RCSfile: ShowLogging.java,v $ $Revision: 1.7 $ $Date: 2011-11-30 13:51:46 $";
	
	boolean showDirectories = AppConstants.getInstance().getBoolean("logging.showdirectories", false);
	
	public ActionForward execute(
	    ActionMapping mapping,
	    ActionForm form,
	    HttpServletRequest request,
	    HttpServletResponse response)
	    throws IOException, ServletException {
	
	    // Initialize action
	    initAction(request);
	    // Retrieve logging directory for browsing
	    String path=request.getParameter("directory");
	    if (StringUtils.isEmpty(path)) {
			path=AppConstants.getInstance().getResolvedProperty("logging.path");
	    }
		String wildcard=AppConstants.getInstance().getProperty("logging.wildcard");
	    Dir2Xml dx=new Dir2Xml();
	    dx.setPath(path);
	    if (wildcard!=null) dx.setWildCard(wildcard);
	    String listresult;
	    try {
		    listresult=dx.getDirList(showDirectories);
	    } catch (Exception e) {
			error("error occured on getting directory list",e);
		    log.warn("returning empty result for directory listing");
		    listresult="<directory/>";
	    }
	    request.setAttribute("Dir2Xml", listresult);
	
	
	  // Report any errors we have discovered back to the original form
	    if (!errors.isEmpty()) {
	        saveErrors(request, errors);
	    }
	    // Forward control to the specified success URI
	    log.debug("forward to success");
	    return (mapping.findForward("success"));
	
	}
}
