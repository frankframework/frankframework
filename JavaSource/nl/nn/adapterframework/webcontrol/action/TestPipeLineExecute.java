package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.PipeLineResult;
import org.apache.struts.action.*;
import org.apache.struts.upload.FormFile;
import nl.nn.adapterframework.util.Misc;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * Test the Pipeline of an adapter
 * <p>$Id: TestPipeLineExecute.java,v 1.4 2004-11-10 13:02:40 L190409 Exp $</p>
 * @author  Johan Verrips
 * @see nl.nn.adapterframework.configuration.Configuration
 * @see nl.nn.adapterframework.core.Adapter
 * @see nl.nn.adapterframework.core.PipeLine
 */

public final class TestPipeLineExecute extends ActionBase {
	public static final String version="$Id: TestPipeLineExecute.java,v 1.4 2004-11-10 13:02:40 L190409 Exp $";
	

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
    

    DynaActionForm pipeLineTestForm = (DynaActionForm) form;
//    ArrayList form_adapters = (ArrayList) pipeLineTestForm.get("adapters");
    String form_adapterName = (String) pipeLineTestForm.get("adapterName");
    String form_message = (String) pipeLineTestForm.get("message");
    String form_resultText = "";
    String form_resultState = "";
    FormFile form_file = (FormFile) pipeLineTestForm.get("file");

    // if no message and no formfile, send an error
    if ((form_message == null) || (form_message.length() == 0)) {
        if ((form_file==null) || (form_file.getFileSize() == 0)) {
            storeFormData(null,null,null, pipeLineTestForm);
            errors.add("", new ActionError("errors.generic", "Nothing to send or test"));
        }
    }
    // Report any errors we have discovered back to the original form
    if (!errors.isEmpty()) {
        saveErrors(request, errors);
        storeFormData(null, null, null, pipeLineTestForm);
        return (new ActionForward(mapping.getInput()));
    }
    if ((form_adapterName == null) || (form_adapterName.length() == 0)) {
        errors.add("", new ActionError("errors.generic", "No adapter selected"));
    }
    // Report any errors we have discovered back to the original form
    if (!errors.isEmpty()) {
        saveErrors(request, errors);
        storeFormData(null,null, form_message, pipeLineTestForm);
        return (new ActionForward(mapping.getInput()));
    }
    // Execute the request
    IAdapter adapter = config.getRegisteredAdapter(form_adapterName);
    if (adapter == null)
        errors.add(
            "",
            new ActionError(
                "errors.generic",
                "Adapter with specified name ["
                    + form_adapterName
                    + "] could not be retrieved"));
    // Report any errors we have discovered back to the original form
    if (!errors.isEmpty()) {
        saveErrors(request, errors);
        storeFormData(null, null, form_message,  pipeLineTestForm);
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

    // Execute the request
    PipeLineResult pipeLineResult=adapter.processMessage("testmessage"+Misc.createSimpleUUID(), form_message);
    form_resultText = pipeLineResult.getResult();
    form_resultState= pipeLineResult.getState();
	
    storeFormData(form_resultText,form_resultState, form_message, pipeLineTestForm);

    // Report any errors we have discovered back to the original form
    if (!errors.isEmpty()) {
        saveErrors(request, errors);
        return (new ActionForward(mapping.getInput()));
    }

    // Forward control to the specified success URI
    log.debug("forward to success");
    return (mapping.findForward("success"));

}
public void storeFormData(String result, String state, String message,  DynaActionForm pipeLineTestForm) {

    // refresh list of stopped adapters
    // =================================
    ArrayList adapters = new ArrayList();
    adapters.add("-- select an adapter --");

    // get the names of the Adapters
    Iterator adapterNamesIt = config.getRegisteredAdapterNames();
    while (adapterNamesIt.hasNext()) {
        String adapterName = (String) adapterNamesIt.next();
//        IAdapter tadapter = config.getRegisteredAdapter(adapterName);
        adapters.add(adapterName);
    }
    pipeLineTestForm.set("adapters", adapters);
    if (null!=message) pipeLineTestForm.set("message", message);
    if (null != result) {
        pipeLineTestForm.set("result", result);

    }
    if (null != state) {
        pipeLineTestForm.set("state", state);

    }

}
}
