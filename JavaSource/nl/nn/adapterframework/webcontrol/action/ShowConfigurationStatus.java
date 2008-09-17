/*
 * $Log: ShowConfigurationStatus.java,v $
 * Revision 1.18  2008-09-17 12:30:52  europe\L190409
 * show sender nested in listener too
 *
 * Revision 1.17  2008/08/12 15:50:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added messagesRetried
 *
 * Revision 1.16  2008/08/06 16:43:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed display of messagelog counts
 *
 * Revision 1.15  2008/07/24 12:40:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for messageCounts in messaglog
 *
 * Revision 1.14  2008/05/15 15:23:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * some support to display configuration exceptions again
 *
 * Revision 1.13  2008/02/06 16:04:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * encode message instead of CDATA
 *
 * Revision 1.12  2008/01/29 12:19:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for thread number control
 *
 * Revision 1.11  2007/11/22 09:17:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * remove check for inprocessstorage
 *
 * Revision 1.10  2007/10/08 12:26:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected date formatting
 *
 * Revision 1.9  2007/10/02 09:19:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * show physical destination names of listeners and their senders
 *
 * Revision 1.8  2007/07/19 15:18:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * list Adapters in order of configuration
 *
 * Revision 1.7  2007/06/12 11:25:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added receiver-listener info
 *
 * Revision 1.6  2007/05/29 11:11:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * add message logs
 *
 * Revision 1.5	2005/07/19 12:09:21	Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added indication of inProcessStorage and errorStorage
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Prepare the main screen of the IbisConsole.
 * 
 * @author	Johan Verrips
 * @version Id
 */
public final class ShowConfigurationStatus extends ActionBase {
	public static final String version = "$RCSfile: ShowConfigurationStatus.java,v $ $Revision: 1.18 $ $Date: 2008-09-17 12:30:52 $";

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		// Initialize action
		initAction(request);

		if (null==config) {
			return (mapping.findForward("noconfig"));
		}

		XmlBuilder adapters=new XmlBuilder("registeredAdapters");
		if (config.getConfigurationException()!=null) {
			XmlBuilder exceptionsXML=new XmlBuilder("exceptions");
			XmlBuilder exceptionXML=new XmlBuilder("exception");
			exceptionXML.setValue(config.getConfigurationException().getMessage());
			exceptionsXML.addSubElement(exceptionXML);
			adapters.addSubElement(exceptionsXML);
		}
		for(int j=0; j<config.getRegisteredAdapters().size(); j++) {
			Adapter adapter = (Adapter)config.getRegisteredAdapter(j);

			XmlBuilder adapterXML=new XmlBuilder("adapter");
			adapters.addSubElement(adapterXML);
		
			RunStateEnum adapterRunState = adapter.getRunState();
			
			adapterXML.addAttribute("name",adapter.getName());
			adapterXML.addAttribute("started", ""+(adapterRunState.equals(RunStateEnum.STARTED)));
			adapterXML.addAttribute("state", adapterRunState.toString());
			adapterXML.addAttribute("configured", ""+adapter.configurationSucceeded());
			adapterXML.addAttribute("upSince", adapter.getStatsUpSince());
			adapterXML.addAttribute("lastMessageDate", adapter.getLastMessageDate());
			adapterXML.addAttribute("messagesProcessed", ""+adapter.getNumOfMessagesProcessed());
			adapterXML.addAttribute("messagesInError", ""+adapter.getNumOfMessagesInError());
		
			Iterator recIt=adapter.getReceiverIterator();
			if (recIt.hasNext()){
				XmlBuilder receiversXML=new XmlBuilder("receivers");
				while (recIt.hasNext()){
					IReceiver receiver=(IReceiver) recIt.next();
					XmlBuilder receiverXML=new XmlBuilder("receiver");
					receiversXML.addSubElement(receiverXML);
	
					RunStateEnum receiverRunState = receiver.getRunState();
					 
					receiverXML.addAttribute("isStarted", ""+(receiverRunState.equals(RunStateEnum.STARTED)));
					receiverXML.addAttribute("state", receiverRunState.toString());
					receiverXML.addAttribute("name",receiver.getName());
					receiverXML.addAttribute("class", ClassUtils.nameOf(receiver));
					receiverXML.addAttribute("messagesReceived", ""+receiver.getMessagesReceived());
					receiverXML.addAttribute("messagesRetried", ""+receiver.getMessagesRetried());
					ISender sender=null;
					if (receiver instanceof ReceiverBase ) {
						ReceiverBase rb = (ReceiverBase) receiver;
						IListener listener=rb.getListener();
						receiverXML.addAttribute("listenerClass", ClassUtils.nameOf(listener));
						if (listener instanceof HasPhysicalDestination) {
							String pd = ((HasPhysicalDestination)rb.getListener()).getPhysicalDestinationName();
							receiverXML.addAttribute("listenerDestination", pd);
						}
						if (listener instanceof HasSender) {
							sender = ((HasSender)listener).getSender();
						}
						//receiverXML.addAttribute("hasInprocessStorage", ""+(rb.getInProcessStorage()!=null));
						ITransactionalStorage ts;
						ts=rb.getErrorStorage();
						receiverXML.addAttribute("hasErrorStorage", ""+(ts!=null));
						if (ts!=null) {
							try {
								receiverXML.addAttribute("errorStorageCount", ts.getMessageCount());
							} catch (Exception e) {
								log.warn(e);
								receiverXML.addAttribute("errorStorageCount", "error");
							}
						}
						ts=rb.getMessageLog();
						receiverXML.addAttribute("hasMessageLog", ""+(ts!=null));
						if (ts!=null) {
							try {
								receiverXML.addAttribute("messageLogCount", ts.getMessageCount());
							} catch (Exception e) {
								log.warn(e);
								receiverXML.addAttribute("messageLogCount", "error");
							}
						}
					}

					if (receiver instanceof HasSender) {
						ISender rsender = ((HasSender) receiver).getSender();
						if (rsender!=null) { // this sender has preference, but avoid overwriting listeners sender with null
							sender=rsender; 
						}
					}
					if (sender != null) { 
						receiverXML.addAttribute("senderName", sender.getName());
						receiverXML.addAttribute("senderClass", ClassUtils.nameOf(sender));
						if (sender instanceof HasPhysicalDestination) {
							String pd = ((HasPhysicalDestination)sender).getPhysicalDestinationName();
							receiverXML.addAttribute("senderDestination", pd);
						}
					}
					if (receiver instanceof IThreadCountControllable) {
						IThreadCountControllable tcc = (IThreadCountControllable)receiver;
						if (tcc.isThreadCountReadable()) {
							receiverXML.addAttribute("threadCount", tcc.getCurrentThreadCount()+"");
							receiverXML.addAttribute("maxThreadCount", tcc.getMaxThreadCount()+"");
						}
						if (tcc.isThreadCountControllable()) {
							receiverXML.addAttribute("threadCountControllable", "true");
						}
					}
				}
				adapterXML.addSubElement(receiversXML); 
			}

			// make list of pipes to be displayed in configuration status
			XmlBuilder pipesElem = new XmlBuilder("pipes");
			adapterXML.addSubElement(pipesElem);
			PipeLine pipeline = adapter.getPipeLine();
			for (int i=0; i<pipeline.getPipes().size(); i++) {
				IPipe pipe = pipeline.getPipe(i);
				String pipename=pipe.getName();
				if (pipe instanceof MessageSendingPipe) {
					MessageSendingPipe msp=(MessageSendingPipe)pipe;
					XmlBuilder pipeElem = new XmlBuilder("pipe");
					pipeElem.addAttribute("name",pipename);
					pipesElem.addSubElement(pipeElem);
					ISender sender = msp.getSender();
					pipeElem.addAttribute("sender",ClassUtils.nameOf(sender));
					if (sender instanceof HasPhysicalDestination) {
						pipeElem.addAttribute("destination",((HasPhysicalDestination)sender).getPhysicalDestinationName());
					}
					ITransactionalStorage messageLog = msp.getMessageLog();
					if (messageLog!=null) {
						pipeElem.addAttribute("hasMessageLog","true");
						String messageLogCount;
						try {
							messageLogCount=""+messageLog.getMessageCount();
						} catch (Exception e) {
							log.warn(e);
							messageLogCount="error";
						}
						pipeElem.addAttribute("messageLogCount",messageLogCount);
						XmlBuilder browserElem = new XmlBuilder("browser");
						browserElem.addAttribute("name",messageLog.getName());
						browserElem.addAttribute("type","log");
						browserElem.addAttribute("slotId",messageLog.getSlotId());
						browserElem.addAttribute("count", messageLogCount);
						pipeElem.addSubElement(browserElem);
					}
				}
			}
		
			// retrieve messages from adapters				
			XmlBuilder adapterMessages=new XmlBuilder("adapterMessages");
			adapterXML.addSubElement(adapterMessages);
			for (int t=0; t<adapter.getMessageKeeper().size(); t++) {
				XmlBuilder adapterMessage=new XmlBuilder("adapterMessage");
				adapterMessage.setValue(adapter.getMessageKeeper().getMessage(t).getMessageText(),true);
				adapterMessage.addAttribute("date", DateUtils.format(adapter.getMessageKeeper().getMessage(t).getMessageDate(), DateUtils.FORMAT_FULL_GENERIC));
				adapterMessages.addSubElement(adapterMessage);
			}

		}
		request.setAttribute("adapters", adapters.toXML());

		// Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));
	}
}
