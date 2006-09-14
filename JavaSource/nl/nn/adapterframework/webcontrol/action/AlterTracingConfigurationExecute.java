/*
 * $Log: AlterTracingConfigurationExecute.java,v $
 * Revision 1.1  2006-09-14 15:28:49  europe\L190409
 * first version of TracingHandlers
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.receivers.ReceiverBase;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

public final class AlterTracingConfigurationExecute extends ActionBase {
	public static final String version="$RCSfile: AlterTracingConfigurationExecute.java,v $ $Revision: 1.1 $ $Date: 2006-09-14 15:28:49 $";

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

		DynaActionForm alterTracingConfigurationForm = (DynaActionForm) form;
		String form_adapterName = (String) alterTracingConfigurationForm.get("adapterName");
		String form_pipeName = (String) alterTracingConfigurationForm.get("pipeName");
		String form_receiverName =(String) alterTracingConfigurationForm.get("receiverName");
		int form_beforeEvent = ((Integer) alterTracingConfigurationForm.get("beforeEvent")).shortValue();
		int form_afterEvent = ((Integer) alterTracingConfigurationForm.get("afterEvent")).shortValue();
		int form_exceptionEvent = ((Integer) alterTracingConfigurationForm.get("exceptionEvent")).shortValue();

		Adapter adapter = (Adapter) config.getRegisteredAdapter(form_adapterName);

		int beforeEvent;
		int afterEvent;
		int exceptionEvent;

		if (StringUtils.isEmpty(form_pipeName)) {
			IReceiver receiver = adapter.getReceiverByName(form_receiverName);

			if (receiver instanceof ReceiverBase) {
				ReceiverBase receiverb = (ReceiverBase)receiver;

				beforeEvent = receiverb.getBeforeEvent();
				if (beforeEvent != form_beforeEvent) {
					receiverb.setBeforeEvent(form_beforeEvent);
				}
				afterEvent = receiverb.getAfterEvent();
				if (afterEvent != form_afterEvent) {
					receiverb.setAfterEvent(form_afterEvent);
				}
				exceptionEvent = receiverb.getExceptionEvent();
				if (exceptionEvent != form_exceptionEvent) {
					receiverb.setExceptionEvent(form_exceptionEvent);
				}
			}
		} else {
			IPipe pipe = adapter.getPipeLine().getPipe(form_pipeName);

			if (pipe instanceof AbstractPipe) {
				AbstractPipe ap = (AbstractPipe)pipe;

				beforeEvent = ap.getBeforeEvent();
				if (beforeEvent != form_beforeEvent) {
					ap.setBeforeEvent(form_beforeEvent);
				}
				afterEvent = ap.getAfterEvent();
				if (afterEvent != form_afterEvent) {
					ap.setAfterEvent(form_afterEvent);
				}
				exceptionEvent = ap.getExceptionEvent();
				if (exceptionEvent != form_exceptionEvent) {
					ap.setExceptionEvent(form_exceptionEvent);
				}
			}
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
