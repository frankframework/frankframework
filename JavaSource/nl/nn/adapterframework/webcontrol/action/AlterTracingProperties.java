package nl.nn.adapterframework.webcontrol.action;

/*
 * $Log: AlterTracingProperties.java,v $
 * Revision 1.3  2011-11-30 13:51:45  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2006/09/14 15:28:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version of TracingHandlers
 *
 */
import nl.nn.adapterframework.util.TracingUtil;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AlterTracingProperties extends ActionBase {
	public static final String version="$RCSfile: AlterTracingProperties.java,v $ $Revision: 1.3 $ $Date: 2011-11-30 13:51:45 $";

	public ActionForward execute(
		ActionMapping mapping,
		ActionForm form,
		HttpServletRequest request,
		HttpServletResponse response)
		throws ServletException {
		initAction(request);
		if (null == config) {
			return (mapping.findForward("noconfig"));
		}

		DynaActionForm alterGimTemplateForm =
			getPersistentForm(mapping, form, request);

		initAction(request);

		// Report any errors we have discovered back to the original form
		if (!errors.isEmpty()) {
			saveErrors(request, errors);
		}

		String properties;
		try {
			properties = TracingUtil.getProperties();
		} catch (Throwable e) {
			log.error(e);
			throw new ServletException(e);
		}

		alterGimTemplateForm.set("properties", properties);

		// Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));

	}
}
