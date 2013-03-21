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
import nl.nn.adapterframework.jdbc.JdbcSenderBase;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.MessageKeeperMessage;
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
 * @version $Id$
 */
public final class ShowConfigurationStatus extends ActionBase {
	public static final String version = "$RCSfile: ShowConfigurationStatus.java,v $ $Revision: 1.30 $ $Date: 2013-03-13 14:40:02 $";

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
				if (esr==1) {
					warningXML.setValue("Errorlog contains 1 record. Service management should check whether this record has to be resent or deleted");
				} else {
					warningXML.setValue("Errorlog contains "+esr+" records. Service Management should check whether these records have to be resent or deleted");
				}
				warningXML.addAttribute("severe", true);
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
		int countMessagesInfo=0;
		int countMessagesWarn=0;
		int countMessagesError=0;
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
					if (sender instanceof JdbcSenderBase) {
						pipeElem.addAttribute("isJdbcSender","true");
					}
					IListener listener = msp.getListener();
					if (listener!=null) {
						pipeElem.addAttribute("listenerName", listener.getName());
						pipeElem.addAttribute("listenerClass", ClassUtils.nameOf(listener));
						if (listener instanceof HasPhysicalDestination) {
							String pd = ((HasPhysicalDestination)listener).getPhysicalDestinationName();
							pipeElem.addAttribute("listenerDestination", pd);
						}
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
				String level = adapter.getMessageKeeper().getMessage(t).getMessageLevel();
				adapterMessage.addAttribute("level", level);
				adapterMessages.addSubElement(adapterMessage);
				if (level.equals(MessageKeeperMessage.ERROR_LEVEL)) {
					countMessagesError++;
				} else {
					if (level.equals(MessageKeeperMessage.WARN_LEVEL)) {
						countMessagesWarn++;
					} else {
						countMessagesInfo++;
					}
				}
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
		XmlBuilder messageLevelXML=new XmlBuilder("messageLevel");
		messageLevelXML.addAttribute("error",countMessagesError+"");
		messageLevelXML.addAttribute("warn",countMessagesWarn+"");
		messageLevelXML.addAttribute("info",countMessagesInfo+"");
		summaryXML.addSubElement(messageLevelXML);
		adapters.addSubElement(summaryXML);
		request.setAttribute("adapters", adapters.toXML());

		// Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));
	}
}
