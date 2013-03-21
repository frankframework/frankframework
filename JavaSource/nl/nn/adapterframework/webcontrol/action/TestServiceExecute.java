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
/*
 * $Log: TestServiceExecute.java,v $
 * Revision 1.9  2011-11-30 13:51:46  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.7  2011/05/19 15:03:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use simplified ServiceDispatcher
 *
 * Revision 1.6  2008/12/16 13:37:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * read messages in the right encoding
 *
 * Revision 1.5  2008/05/22 07:46:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use inherited error() method
 *
 * Revision 1.4  2007/10/08 13:41:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.upload.FormFile;

/**
 * Execute a service test.
 * @version $Id$
 * @author Johan Verrips
 */
public class TestServiceExecute extends ActionBase {
	
    public ActionForward execute(
        ActionMapping mapping,
        ActionForm form,
        HttpServletRequest request,
        HttpServletResponse response)
        throws IOException, ServletException {

        // Initialize action
        initAction(request);
	    if (null == config) {
	        return (mapping.findForward("noconfig"));
	    }

        DynaActionForm serviceTestForm = (DynaActionForm) form;
//        List form_services = (List) serviceTestForm.get("services");
        String form_serviceName = (String) serviceTestForm.get("serviceName");
        String form_message = (String) serviceTestForm.get("message");
        String form_result = (String) serviceTestForm.get("message");
        FormFile form_file = (FormFile) serviceTestForm.get("file");

        // if no message and no formfile, send an error
        if ((form_message == null) || (form_message.length() == 0)) {
            if ((form_file == null) || (form_file.getFileSize() == 0)) {
                storeFormData(null, null, serviceTestForm);
                warn("Nothing to send or test");
            }
        }
        // Report any errors we have discovered back to the original form
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            storeFormData(null, null, serviceTestForm);
            return (new ActionForward(mapping.getInput()));
        }
        if ((form_serviceName == null) || (form_serviceName.length() == 0)) {
            warn("No service selected");
        }
        // Report any errors we have discovered back to the original form
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            storeFormData(null, form_message, serviceTestForm);
            return (new ActionForward(mapping.getInput()));
        }
        // Execute the request
        if (!(ServiceDispatcher.getInstance().isRegisteredServiceListener(form_serviceName)))
        	warn("Servicer with specified name [" + form_serviceName
                        + "] is not registered at the Dispatcher");
        // Report any errors we have discovered back to the original form
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            storeFormData(null, form_message, serviceTestForm);
            return (new ActionForward(mapping.getInput()));
        }

        // if upload is choosen, it prevails over the message
        if ((form_file != null) && (form_file.getFileSize() > 0)) {
            form_message = XmlUtils.readXml(form_file.getFileData(),request.getCharacterEncoding(),false);
            log.debug(
                "Upload of file ["
                    + form_file.getFileName()
                    + "] ContentType["
                    + form_file.getContentType()
                    + "]");

        } else {
			form_message=new String(form_message.getBytes(),Misc.DEFAULT_INPUT_STREAM_ENCODING);
        }
        form_result = "";
        // Execute the request
        try {
        	Map context = new HashMap();
            form_result =
                ServiceDispatcher.getInstance().dispatchRequest(form_serviceName, null, form_message, context);
        } catch (Exception e) {
        	warn("Service with specified name [" + form_serviceName + "] got error",e);
        }
        storeFormData(form_result, form_message, serviceTestForm);

        // Report any errors we have discovered back to the original form
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            return (new ActionForward(mapping.getInput()));
        }

        // Forward control to the specified success URI
        log.debug("forward to success");
        return (mapping.findForward("success"));

    }
    
    public void storeFormData(
        String result,
        String message,
        DynaActionForm serviceTestForm) {

        // refresh list of stopped adapters
        // =================================
        Iterator it = ServiceDispatcher.getInstance().getRegisteredListenerNames();
        List services = new ArrayList();
        services.add("----- select a service -----");
        while (it.hasNext()) {
            services.add((String) it.next());
        }
        serviceTestForm.set("services", services);
        if (null != message)
            serviceTestForm.set("message", message);
        if (null != result) {
            serviceTestForm.set("result", result);

        }
    }
    
}
