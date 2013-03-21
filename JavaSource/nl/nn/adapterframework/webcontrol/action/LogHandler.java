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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

/**
 * This handler updates the root log level and the value in the AppConstants named "log.logIntermediaryResults".
 *
 * @author  Johan Verrips IOS
 * @version $Id$
 */
public class LogHandler extends ActionBase {
	public static final String version = "$RCSfile: LogHandler.java,v $ $Revision: 1.8 $ $Date: 2011-11-30 13:51:45 $";
	
	 public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {


        // Initialize action
        initAction(request);

        String commandIssuedBy= " remoteHost ["+request.getRemoteHost()+"]";
		commandIssuedBy+=" remoteAddress ["+request.getRemoteAddr()+"]";
		commandIssuedBy+=" remoteUser ["+request.getRemoteUser()+"]";

		Logger lg=LogUtil.getRootLogger();

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
