package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * This handler updates the root log level and the value in the AppConstants named "log.logIntermediaryResults".
 * <p>Creation date: (03-11-2003 11:22:42)</p>
 * @version Id
 * @author Johan Verrips IOS
 */
public class LogHandler extends ActionBase {
	public static final String version="$Id: LogHandler.java,v 1.4 2007-02-16 14:22:54 europe\L190409 Exp $";
	
	 public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {


        // Initialize action
        initAction(request);

        String commandIssuedBy= " remoteHost ["+request.getRemoteHost()+"]";
		commandIssuedBy+=" remoteAddress ["+request.getRemoteAddr()+"]";
		commandIssuedBy+=" remoteUser ["+request.getRemoteUser()+"]";

		Logger lg=LogUtil.getHierarchy().getRootLogger();

        DynaActionForm logForm = (DynaActionForm) form;
        String form_logLevel = (String) logForm.get("logLevel");
         boolean form_logIntermediaryResults=false;
         if (null!= logForm.get("logIntermediaryResults")) {
            form_logIntermediaryResults = ((Boolean) logForm.get("logIntermediaryResults")).booleanValue();
         }
        log.warn("*** logintermediary results="+form_logIntermediaryResults);
        String logIntermediaryResults="false";
        if (form_logIntermediaryResults) logIntermediaryResults="true";

        Level level=Level.toLevel(form_logLevel);


		log.warn("LogLevel changed from ["
			+lg.getLevel()
			+"]  to ["
			+level
			+"]  and logIntermediaryResults from ["
			+AppConstants.getInstance().getProperty("log.logIntermediaryResults")
			+ "] to ["
			+ ""+form_logIntermediaryResults
			+"] by"+commandIssuedBy);

        AppConstants.getInstance().put("log.logIntermediaryResults", logIntermediaryResults);
		lg.setLevel(level);


        return (mapping.findForward("success"));
        }
}
