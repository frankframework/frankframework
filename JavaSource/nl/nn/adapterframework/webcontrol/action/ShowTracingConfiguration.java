/*
 * $Log: ShowTracingConfiguration.java,v $
 * Revision 1.1  2006-09-14 15:29:44  europe\L190409
 * first version of TracingHandlers
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.XmlBuilder;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * <code>Action</code> to retrieve the Tracing configuration.
 * @author  Peter Leeuwenburgh
 */

public final class ShowTracingConfiguration extends ActionBase {
	public static final String version="$RCSfile: ShowTracingConfiguration.java,v $ $Revision: 1.1 $ $Date: 2006-09-14 15:29:44 $";

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

		XmlBuilder tracingConfigurationXML =
			new XmlBuilder("tracingConfiguration");

		Iterator registeredAdapters = config.getRegisteredAdapterNames();
		XmlBuilder adapters = new XmlBuilder("registeredAdapters");
		while (registeredAdapters.hasNext()) {
			String adapterName = (String) registeredAdapters.next();
			Adapter adapter = (Adapter) config.getRegisteredAdapter(adapterName);
			XmlBuilder adapterXML = new XmlBuilder("adapter");
			adapterXML.addAttribute("name", adapter.getName());
			Iterator recIt = adapter.getReceiverIterator();
			if (recIt.hasNext()) {
				XmlBuilder receiversXML = new XmlBuilder("receivers");
				while (recIt.hasNext()) {
					ReceiverBase receiver = (ReceiverBase) recIt.next();
					XmlBuilder receiverXML = new XmlBuilder("receiver");
					receiversXML.addSubElement(receiverXML);
					receiverXML.addAttribute("name", receiver.getName());
					receiverXML.addAttribute("beforeEvent",	Integer.toString(receiver.getBeforeEvent()));
					receiverXML.addAttribute("afterEvent",	Integer.toString(receiver.getAfterEvent()));
					receiverXML.addAttribute("exceptionEvent", Integer.toString(receiver.getExceptionEvent()));
				}
				adapterXML.addSubElement(receiversXML);
			}

			XmlBuilder pipelineXML = new XmlBuilder("pipeline");
			
			Hashtable pipelineTable = adapter.getPipeLine().getPipes();
			// sort the Hashtable
			SortedSet sortedKeys = new TreeSet(pipelineTable.keySet());
			Iterator pipelineTableIter = sortedKeys.iterator();
			if (pipelineTableIter.hasNext()) {
				while (pipelineTableIter.hasNext()) {
					String pipeName = (String) pipelineTableIter.next();
					IPipe pipe = adapter.getPipeLine().getPipe(pipeName);

					XmlBuilder pipeXML = new XmlBuilder("pipe");
					pipeXML.addAttribute("name", pipe.getName());
					if (pipe instanceof AbstractPipe) {
						AbstractPipe ap = (AbstractPipe)pipe;
						pipeXML.addAttribute("beforeEvent",	Integer.toString(ap.getBeforeEvent()));
						pipeXML.addAttribute("afterEvent",  Integer.toString(ap.getAfterEvent()));
						pipeXML.addAttribute("exceptionEvent",	Integer.toString(ap.getExceptionEvent()));
	
					}
					pipelineXML.addSubElement(pipeXML);
				}
				adapterXML.addSubElement(pipelineXML);
			}
			adapters.addSubElement(adapterXML);
		}

		tracingConfigurationXML.addSubElement(adapters);

		request.setAttribute(
			"tracingConfiguration",
			tracingConfigurationXML.toXML());

		// Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));
	}
}
