package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.receivers.ServiceDispatcher;
import org.apache.struts.action.*;
import org.apache.struts.upload.FormFile;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
/**
 * Execute a service test
 * <p>$Id: TestServiceExecute.java,v 1.2 2004-02-04 10:02:09 a1909356#db2admin Exp $</p>
 * @author Johan Verrips
 */
public class TestServiceExecute extends ActionBase {
	public static final String version="$Id: TestServiceExecute.java,v 1.2 2004-02-04 10:02:09 a1909356#db2admin Exp $";
	

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
//        ArrayList form_services = (ArrayList) serviceTestForm.get("services");
        String form_serviceName = (String) serviceTestForm.get("serviceName");
        String form_message = (String) serviceTestForm.get("message");
        String form_result = (String) serviceTestForm.get("message");
        FormFile form_file = (FormFile) serviceTestForm.get("file");

        // if no message and no formfile, send an error
        if ((form_message == null) || (form_message.length() == 0)) {
            if ((form_file == null) || (form_file.getFileSize() == 0)) {
                storeFormData(null, null, serviceTestForm);
                errors.add("", new ActionError("errors.generic", "Nothing to send or test"));
            }
        }
        // Report any errors we have discovered back to the original form
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            storeFormData(null, null, serviceTestForm);
            return (new ActionForward(mapping.getInput()));
        }
        if ((form_serviceName == null) || (form_serviceName.length() == 0)) {
            errors.add("", new ActionError("errors.generic", "No service selected"));
        }
        // Report any errors we have discovered back to the original form
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            storeFormData(null, form_message, serviceTestForm);
            return (new ActionForward(mapping.getInput()));
        }
        // Execute the request
        if (!(ServiceDispatcher
            .getInstance()
            .isRegisteredServiceListener(form_serviceName)))
            errors.add(
                "",
                new ActionError(
                    "errors.generic",
                    "Servicer with specified name ["
                        + form_serviceName
                        + "] is not registered at the Dispatcher"));
        // Report any errors we have discovered back to the original form
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
            storeFormData(null, form_message, serviceTestForm);
            return (new ActionForward(mapping.getInput()));
        }

        // if upload is choosen, it prevails over the message
        if ((form_file != null) && (form_file.getFileSize() > 0)) {
            form_message = new String(form_file.getFileData());
            log.debug(
                "Upload of file ["
                    + form_file.getFileName()
                    + "] ContentType["
                    + form_file.getContentType()
                    + "]");

        }
        form_result = "";
        // Execute the request
        try {
            form_result =
                ServiceDispatcher.getInstance().dispatchRequest(form_serviceName, form_message);
        } catch (Exception e) {
            log.error(e);
            errors.add(
                "",
                new ActionError(
                    "errors.generic",
                    "Service with specified name ["
                        + form_serviceName
                        + "] got error: "
                        + e.getMessage()));
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
        ArrayList services = new ArrayList();
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
