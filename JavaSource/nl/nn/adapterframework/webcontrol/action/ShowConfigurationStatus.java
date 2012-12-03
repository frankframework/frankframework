/*
 * $Log: ShowConfigurationStatus.java,v $
 * Revision 1.27  2012-12-03 08:09:42  europe\m168309
 * added configWarning when errorlog is not empty
 *
 * Revision 1.26  2012/06/14 14:07:22  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * ShowConfigurationStatus: added facility to enable count for messageLog and errorStore
 *
 * Revision 1.25  2012/04/10 07:50:18  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * truncate long adapter messages
 *
 * Revision 1.24  2011/11/30 13:51:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.22  2010/02/15 09:49:21  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added counters for Summary
 *
 * Revision 1.21  2009/03/30 12:23:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added counter for messagesRejected
 *
 * Revision 1.20  2008/12/30 17:01:13  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added configuration warnings facility (in Show configurationStatus)
 *
 * Revision 1.19  2008/10/24 14:42:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adapters are shown case insensitive sorted
 *
 * Revision 1.18  2008/09/17 12:30:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

import nl.nn.adapterframework.configuration.ConfigurationWarnings;
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
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.StringUtils;
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
	public static final String version = "$RCSfile: ShowConfigurationStatus.java,v $ $Revision: 1.27 $ $Date: 2012-12-03 08:09:42 $";

	private int maxMessageSize = AppConstants.getInstance().getInt("adapter.message.max.size",0); 
	private boolean showCountMessageLog = AppConstants.getInstance().getBoolean("messageLog.count.show", true);
	private boolean showCountErrorStore = AppConstants.getInstance().getBoolean("errorStore.count.show", true);

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		// Initialize action
		initAction(request);

		if (null==config) {
			return (mapping.findForward("noconfig"));
		}

		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();

		long esr = 0;
		if (null!=config) {
			for(Iterator adapterIt=config.getRegisteredAdapters().iterator(); adapterIt.hasNext();) {
				Adapter adapter = (Adapter)adapterIt.next();
				for(Iterator receiverIt=adapter.getReceiverIterator(); receiverIt.hasNext();) {
					ReceiverBase receiver=(ReceiverBase)receiverIt.next();
					ITransactionalStorage errorStorage=receiver.getErrorStorage();
					if (errorStorage!=null) {
						try {
							esr += errorStorage.getMessageCount();
						} catch (Exception e) {
							error("error occured on getting number of errorlog records for adapter ["+adapter.getName()+"]",e);
						    log.warn("assuming there are no errorlog records for adapter ["+adapter.getName()+"]");
						}
					}
				}
			}
		}

		
		XmlBuilder adapters=new XmlBuilder("registeredAdapters");
		if (config.getConfigurationException()!=null) {
			XmlBuilder exceptionsXML=new XmlBuilder("exceptions");
			XmlBuilder exceptionXML=new XmlBuilder("exception");
			exceptionXML.setValue(config.getConfigurationException().getMessage());
			exceptionsXML.addSubElement(exceptionXML);
			adapters.addSubElement(exceptionsXML);
		}
		if (configWarnings.size()>0 || esr>0) {
			XmlBuilder warningsXML=new XmlBuilder("warnings");
			if (esr>0) {
				XmlBuilder warningXML=new XmlBuilder("warnings");
				warningXML=new XmlBuilder("warning");
				warningXML.setValue("Errorlog contains "+esr+" records. These should be checked and after that resent or deleted");
				warningsXML.addSubElement(warningXML);
			}
			for (int j=0; j<configWarnings.size(); j++) {
				XmlBuilder warningXML=new XmlBuilder("warnings");
				warningXML=new XmlBuilder("warning");
				warningXML.setValue((String)configWarnings.get(j));
				warningsXML.addSubElement(warningXML);
			}
			adapters.addSubElement(warningsXML);
		}
		int countAdapterStateStarting=0;
		int countAdapterStateStarted=0;
		int countAdapterStateStopping=0;
		int countAdapterStateStopped=0;
		int countAdapterStateError=0;
		int countReceiverStateStarting=0;
		int countReceiverStateStarted=0;
		int countReceiverStateStopping=0;
		int countReceiverStateStopped=0;
		int countReceiverStateError=0;
		for(int j=0; j<config.getRegisteredAdapters().size(); j++) {
			Adapter adapter = (Adapter)config.getRegisteredAdapter(j);

			XmlBuilder adapterXML=new XmlBuilder("adapter");
			adapters.addSubElement(adapterXML);
		
			RunStateEnum adapterRunState = adapter.getRunState();
			
			adapterXML.addAttribute("name",adapter.getName());
			adapterXML.addAttribute("nameUC",StringUtils.upperCase(adapter.getName()));
			adapterXML.addAttribute("started", ""+(adapterRunState.equals(RunStateEnum.STARTED)));
			adapterXML.addAttribute("state", adapterRunState.toString());
			if (adapterRunState.equals(RunStateEnum.STARTING)) {
				countAdapterStateStarting++;
			} else if ((adapterRunState.equals(RunStateEnum.STARTED))) {
				countAdapterStateStarted++;
			} else if ((adapterRunState.equals(RunStateEnum.STOPPING))) {
				countAdapterStateStopping++;
			} else if ((adapterRunState.equals(RunStateEnum.STOPPED))) {
				countAdapterStateStopped++;
			} else if ((adapterRunState.equals(RunStateEnum.ERROR))) {
				countAdapterStateError++;
			}
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
					if (receiverRunState.equals(RunStateEnum.STARTING)) {
						countReceiverStateStarting++;
					} else if ((receiverRunState.equals(RunStateEnum.STARTED))) {
						countReceiverStateStarted++;
					} else if ((receiverRunState.equals(RunStateEnum.STOPPING))) {
						countReceiverStateStopping++;
					} else if ((receiverRunState.equals(RunStateEnum.STOPPED))) {
						countReceiverStateStopped++;
					} else if ((receiverRunState.equals(RunStateEnum.ERROR))) {
						countReceiverStateError++;
					}
					receiverXML.addAttribute("name",receiver.getName());
					receiverXML.addAttribute("class", ClassUtils.nameOf(receiver));
					receiverXML.addAttribute("messagesReceived", ""+receiver.getMessagesReceived());
					receiverXML.addAttribute("messagesRetried", ""+receiver.getMessagesRetried());
					receiverXML.addAttribute("messagesRejected", ""+receiver.getMessagesRejected());
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
								if (showCountErrorStore) {
									receiverXML.addAttribute("errorStorageCount", ts.getMessageCount());
								} else {
									receiverXML.addAttribute("errorStorageCount", "?");
								}
							} catch (Exception e) {
								log.warn(e);
								receiverXML.addAttribute("errorStorageCount", "error");
							}
						}
						ts=rb.getMessageLog();
						receiverXML.addAttribute("hasMessageLog", ""+(ts!=null));
						if (ts!=null) {
							try {
								if (showCountMessageLog) {
									receiverXML.addAttribute("messageLogCount", ts.getMessageCount());
								} else {
									receiverXML.addAttribute("messageLogCount", "?");
								}
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
							if (showCountMessageLog) {
								messageLogCount=""+messageLog.getMessageCount();
							} else {
								messageLogCount="?";
							}
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
				String msg = adapter.getMessageKeeper().getMessage(t).getMessageText();
				if (maxMessageSize>0 && msg.length()>maxMessageSize) {
					msg = msg.substring(0, maxMessageSize) + "...(" + (msg.length()-maxMessageSize) + " characters more)";
				}
				adapterMessage.setValue(msg,true);
				adapterMessage.addAttribute("date", DateUtils.format(adapter.getMessageKeeper().getMessage(t).getMessageDate(), DateUtils.FORMAT_FULL_GENERIC));
				adapterMessages.addSubElement(adapterMessage);
			}

		}

		XmlBuilder summaryXML=new XmlBuilder("summary");
		XmlBuilder adapterStateXML=new XmlBuilder("adapterState");
		adapterStateXML.addAttribute("starting",countAdapterStateStarting+"");
		adapterStateXML.addAttribute("started",countAdapterStateStarted+"");
		adapterStateXML.addAttribute("stopping",countAdapterStateStopping+"");
		adapterStateXML.addAttribute("stopped",countAdapterStateStopped+"");
		adapterStateXML.addAttribute("error",countAdapterStateError+"");
		summaryXML.addSubElement(adapterStateXML);
		XmlBuilder receiverStateXML=new XmlBuilder("receiverState");
		receiverStateXML.addAttribute("starting",countReceiverStateStarting+"");
		receiverStateXML.addAttribute("started",countReceiverStateStarted+"");
		receiverStateXML.addAttribute("stopping",countReceiverStateStopping+"");
		receiverStateXML.addAttribute("stopped",countReceiverStateStopped+"");
		receiverStateXML.addAttribute("error",countReceiverStateError+"");
		summaryXML.addSubElement(receiverStateXML);
		adapters.addSubElement(summaryXML);
		request.setAttribute("adapters", adapters.toXML());

		// Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));
	}
}
