/*
 * $Log: AlterTracingPropertiesExecute.java,v $
 * Revision 1.1  2006-09-14 15:28:49  europe\L190409
 * first version of TracingHandlers
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.util.TracingUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

public final class AlterTracingPropertiesExecute extends ActionBase {
	public static final String version="$RCSfile: AlterTracingPropertiesExecute.java,v $ $Revision: 1.1 $ $Date: 2006-09-14 15:28:49 $";

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

		// Was this transaction cancelled?
		if (isCancelled(request)) {
			removeFormBean(mapping, request);
			return (mapping.findForward("cancel"));
		}

		DynaActionForm alterTracingPropertiesForm = (DynaActionForm) form;
		String form_properties =
			(String) alterTracingPropertiesForm.get("properties");

		try {
			TracingUtil.setProperties(form_properties);
		} catch (Throwable t) {
			errors.add("", new ActionError("errors.generic", t));
		}

		if (!errors.isEmpty()) {
			saveErrors(request, errors);
			return (new ActionForward(mapping.getInput()));
		}

		// Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));
	}
}
