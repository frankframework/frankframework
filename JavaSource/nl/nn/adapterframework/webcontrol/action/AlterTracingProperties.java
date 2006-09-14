package nl.nn.adapterframework.webcontrol.action;

/*
 * $Log: AlterTracingProperties.java,v $
 * Revision 1.1  2006-09-14 15:28:49  europe\L190409
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
	public static final String version="$RCSfile: AlterTracingProperties.java,v $ $Revision: 1.1 $ $Date: 2006-09-14 15:28:49 $";

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
