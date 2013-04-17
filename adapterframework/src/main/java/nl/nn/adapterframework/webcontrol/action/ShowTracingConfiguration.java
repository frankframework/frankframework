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
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * <code>Action</code> to retrieve the Tracing configuration.
 * @author  Peter Leeuwenburgh
 * @version $Id$
 */
public final class ShowTracingConfiguration extends ActionBase {
	public static final String version="$RCSfile: ShowTracingConfiguration.java,v $ $Revision: 1.5 $ $Date: 2011-11-30 13:51:46 $";

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

		XmlBuilder adapters = new XmlBuilder("registeredAdapters");
		for(int j=0; j<config.getRegisteredAdapters().size(); j++) {
			Adapter adapter = (Adapter)config.getRegisteredAdapter(j);

			XmlBuilder adapterXML = new XmlBuilder("adapter");
			adapterXML.addAttribute("name", adapter.getName());
			adapterXML.addAttribute("nameUC",StringUtils.upperCase(adapter.getName()));
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
			
			List pipeList = adapter.getPipeLine().getPipes();
			if (pipeList.size()>0) {
				for (int i=0; i<pipeList.size(); i++) {
					IPipe pipe = adapter.getPipeLine().getPipe(i);
	
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
