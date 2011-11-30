/*
 * $Log: AlterTracingPropertiesExecute.java,v $
 * Revision 1.4  2011-11-30 13:51:46  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2008/05/22 07:33:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use inherited error() method
 *
 * Revision 1.1  2006/09/14 15:28:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version of TracingHandlers
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.util.TracingUtil;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

public final class AlterTracingPropertiesExecute extends ActionBase {
	public static final String version="$RCSfile: AlterTracingPropertiesExecute.java,v $ $Revision: 1.4 $ $Date: 2011-11-30 13:51:46 $";

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
			error("could not set properties",t);
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
