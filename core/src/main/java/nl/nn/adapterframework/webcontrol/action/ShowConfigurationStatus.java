/*
   Copyright 2013-2018 Nationale-Nederlanden

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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.BaseConfigurationWarnings;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.extensions.esb.EsbJmsListener;
import nl.nn.adapterframework.extensions.esb.EsbUtils;
import nl.nn.adapterframework.http.RestListener;
import nl.nn.adapterframework.jdbc.JdbcSenderBase;
import nl.nn.adapterframework.jms.JmsListenerBase;
import nl.nn.adapterframework.jms.JmsMessageBrowser;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.MessageKeeperMessage;
import nl.nn.adapterframework.util.Misc;
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
 */
public final class ShowConfigurationStatus extends ActionBase {
	private static final String CONFIG_ALL = "*ALL*";
	private static final int MAX_MESSAGE_SIZE = AppConstants.getInstance().getInt("adapter.message.max.size",0); 
	private static final boolean SHOW_COUNT_MESSAGELOG = AppConstants.getInstance().getBoolean("messageLog.count.show", true);
	private static final boolean SHOW_COUNT_ERRORSTORE = AppConstants.getInstance().getBoolean("errorStore.count.show", true);

	private class ErrorStoreCounter {
		String config;
		int counter;

		public ErrorStoreCounter(String config, int counter) {
			this.config = config;
			this.counter = counter;
		}
	}

	private class ShowConfigurationStatusManager {
		boolean count = false;
		boolean alert = false;
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
		boolean stateAlert = false;
	}

	public ActionForward executeSub(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		// Initialize action
		initAction(request);
		if(ibisManager==null)return (mapping.findForward("noIbisContext"));

		ShowConfigurationStatusManager showConfigurationStatusManager = new ShowConfigurationStatusManager();
		
		String countStr = request.getParameter("count");
		showConfigurationStatusManager.count = Boolean.parseBoolean(countStr);

		String alertStr = request.getParameter("alert");
		showConfigurationStatusManager.alert = Boolean.parseBoolean(alertStr);
		
		List<Configuration> allConfigurations = ibisManager.getConfigurations();
		XmlBuilder configurationsXml = toConfigurationsXml(allConfigurations);
		request.setAttribute("configurations", configurationsXml.toXML());

		Configuration configurationSelected = null;
		List<IAdapter> registeredAdapters;
		String configurationName = request.getParameter("configuration");
		if (configurationName == null) {
			configurationName = (String)request.getSession().getAttribute("configurationName");
		}
		if (configurationName == null
				|| configurationName.equalsIgnoreCase(CONFIG_ALL)
				|| ibisManager.getConfiguration(configurationName) == null) {
			registeredAdapters = ibisManager.getRegisteredAdapters();
			request.getSession().setAttribute("configurationName", CONFIG_ALL);
			request.getSession().setAttribute("classLoaderType", null);
		} else {
			configurationSelected = ibisManager.getConfiguration(configurationName);
			registeredAdapters = configurationSelected.getRegisteredAdapters();
			request.getSession().setAttribute("configurationName", configurationSelected.getConfigurationName());
			request.getSession().setAttribute("classLoaderType", configurationSelected.getClassLoaderType());
		}

		XmlBuilder adapters = toAdaptersXml(ibisManager.getIbisContext(), allConfigurations, configurationSelected, registeredAdapters, showConfigurationStatusManager);
		request.setAttribute("adapters", adapters.toXML());
		
		// Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));
	}

	// public instead of private for the purpose of JUnit Test (will be changed in near future when struts is removed from IAF)
	public XmlBuilder toConfigurationsXml(List<Configuration> configurations) {
		XmlBuilder configurationsXml = new XmlBuilder("configurations");
		XmlBuilder configurationAllXml = new XmlBuilder("configuration");
		configurationAllXml.setValue(CONFIG_ALL);
		configurationAllXml.addAttribute("nameUC","0" + Misc.toSortName(CONFIG_ALL));
		configurationsXml.addSubElement(configurationAllXml);
		for (Configuration configuration : configurations) {
			XmlBuilder configurationXml = new XmlBuilder("configuration");
			configurationXml.setValue(configuration.getConfigurationName());
			configurationXml.addAttribute("nameUC","1" + Misc.toSortName(configuration.getConfigurationName()));
			configurationsXml.addSubElement(configurationXml);
		}
		return configurationsXml;
	}
	
	// public instead of private for the purpose of JUnit Test (will be changed in near future when struts is removed from IAF)
	public XmlBuilder toAdaptersXml(IbisContext ibisContext, List<Configuration> configurations, Configuration configurationSelected, List<IAdapter> registeredAdapters) {
		return toAdaptersXml(ibisContext, configurations, configurationSelected, registeredAdapters, new ShowConfigurationStatusManager());
	}
	
	private XmlBuilder toAdaptersXml(IbisContext ibisContext, List<Configuration> configurations, Configuration configurationSelected, List<IAdapter> registeredAdapters, ShowConfigurationStatusManager showConfigurationStatusManager) {
		XmlBuilder adapters=new XmlBuilder("registeredAdapters");
		if (configurationSelected != null) {
			adapters.addAttribute("all", false);
		} else {
			adapters.addAttribute("all", true);
		}
		adapters.addAttribute("alert", showConfigurationStatusManager.alert);
		
		XmlBuilder configurationMessagesXml=toConfigurationMessagesXml(ibisContext, configurationSelected);
		adapters.addSubElement(configurationMessagesXml);

		XmlBuilder configurationExceptionsXml = toConfigurationExceptionsXml(configurations, configurationSelected);
		if (configurationExceptionsXml!=null) {
			adapters.addSubElement(configurationExceptionsXml);
		}
		
		XmlBuilder configurationWarningsXml = toConfigurationWarningsXml(configurations, configurationSelected);
		if (configurationWarningsXml!=null) {
			adapters.addSubElement(configurationWarningsXml);
		}
		
		for(IAdapter iAdapter : registeredAdapters) {
			Adapter adapter = (Adapter)iAdapter;
			XmlBuilder adapterXml = toAdapterXml(configurationSelected, adapter, showConfigurationStatusManager);
			adapters.addSubElement(adapterXml);
		}

		XmlBuilder summaryXml = toSummaryXml(showConfigurationStatusManager);
		adapters.addSubElement(summaryXml);
		return adapters;
	}

	private XmlBuilder toConfigurationMessagesXml(IbisContext ibisContext, Configuration configurationSelected) {
		XmlBuilder configurationMessages=new XmlBuilder("configurationMessages");
		MessageKeeper messageKeeper;
		if (configurationSelected != null) {
			messageKeeper = ibisContext.getMessageKeeper(configurationSelected.getName());
		} else {
			messageKeeper = ibisContext.getMessageKeeper(CONFIG_ALL);
		}

		for (int t=0; t<messageKeeper.size(); t++) {
			XmlBuilder configurationMessage=new XmlBuilder("configurationMessage");
			String msg = messageKeeper.getMessage(t).getMessageText();
			if (MAX_MESSAGE_SIZE>0 && msg.length()>MAX_MESSAGE_SIZE) {
				msg = msg.substring(0, MAX_MESSAGE_SIZE) + "...(" + (msg.length()-MAX_MESSAGE_SIZE) + " characters more)";
			}
			configurationMessage.setValue(msg,true);
			configurationMessage.addAttribute("date", DateUtils.format(messageKeeper.getMessage(t).getMessageDate(), DateUtils.FORMAT_FULL_GENERIC));
			String level = messageKeeper.getMessage(t).getMessageLevel();
			configurationMessage.addAttribute("level", level);
			configurationMessages.addSubElement(configurationMessage);
		}
		return configurationMessages;
	}

	private XmlBuilder toConfigurationExceptionsXml(List<Configuration> configurations, Configuration configurationSelected) {
		List<String[]> configurationExceptions = new ArrayList<String[]>();
		if (configurationSelected!=null) {
			if (configurationSelected.getConfigurationException()!=null) {
				String[] item = new String[2];
				item[0]=configurationSelected.getName();
				item[1]=configurationSelected.getConfigurationException().getMessage();
				configurationExceptions.add(item);
			}
		} else {
			for (Configuration configuration : configurations) {
				if (configuration.getConfigurationException()!=null) {
					String[] item = new String[2];
					item[0]=configuration.getName();
					item[1]=configuration.getConfigurationException().getMessage();
					configurationExceptions.add(item);
				}
			}
		}
		if (!configurationExceptions.isEmpty()) {
			XmlBuilder exceptionsXML=new XmlBuilder("exceptions");
			for (int j=0; j<configurationExceptions.size(); j++) {
				XmlBuilder exceptionXML=new XmlBuilder("exception");
				exceptionXML.addAttribute("config",configurationExceptions.get(j)[0]);
				exceptionXML.setValue(configurationExceptions.get(j)[1]);
				exceptionsXML.addSubElement(exceptionXML);
			}
			return exceptionsXML;
		}
		return null;
	}

	private XmlBuilder toConfigurationWarningsXml(List<Configuration> configurations, Configuration configurationSelected) {
		ConfigurationWarnings globalConfigurationWarnings = ConfigurationWarnings.getInstance();

		List<ErrorStoreCounter> errorStoreCounters = retrieveErrorStoreCounters(configurations, configurationSelected);

		List<String[]> selectedConfigurationWarnings = new ArrayList<String[]>();
		if (configurationSelected != null) {
			BaseConfigurationWarnings configWarns = configurationSelected
					.getConfigurationWarnings();
			for (int j = 0; j < configWarns.size(); j++) {
				String[] item = new String[2];
				item[0]=configurationSelected.getName();
				item[1]=(String) configWarns.get(j);
				selectedConfigurationWarnings.add(item);
			}
		} else {
			for (Configuration configuration : configurations) {
				BaseConfigurationWarnings configWarns = configuration
						.getConfigurationWarnings();
				for (int j = 0; j < configWarns.size(); j++) {
					String[] item = new String[2];
					item[0]=configuration.getName();
					item[1]=(String) configWarns.get(j);
					selectedConfigurationWarnings.add(item);
				}
			}
		}
		if (!globalConfigurationWarnings.isEmpty() || !errorStoreCounters.isEmpty() || !SHOW_COUNT_ERRORSTORE || !selectedConfigurationWarnings.isEmpty()) {
			XmlBuilder warningsXML=new XmlBuilder("warnings");
			if (!SHOW_COUNT_ERRORSTORE) {
				XmlBuilder warningXML=new XmlBuilder("warning");
				warningXML.setValue("Errorlog might contain records. This is unknown because errorStore.count.show is not set to true");
				warningXML.addAttribute("severe", true);
				warningsXML.addSubElement(warningXML);
			}
			for (int j=0; j<errorStoreCounters.size(); j++) {
				ErrorStoreCounter esr = errorStoreCounters.get(j);
				XmlBuilder warningXML=new XmlBuilder("warning");
				warningXML.addAttribute("config", esr.config);
				if (esr.counter==1) {
					warningXML.setValue("Errorlog contains 1 record. Service management should check whether this record has to be resent or deleted");
				} else {
					warningXML.setValue("Errorlog contains "+esr.counter+" records. Service Management should check whether these records have to be resent or deleted");
				}
				warningXML.addAttribute("severe", true);
				warningsXML.addSubElement(warningXML);
			}
			for (int j=0; j<globalConfigurationWarnings.size(); j++) {
				XmlBuilder warningXML=new XmlBuilder("warning");
				warningXML.setValue((String)globalConfigurationWarnings.get(j));
				warningsXML.addSubElement(warningXML);
			}
			for (int j=0; j<selectedConfigurationWarnings.size(); j++) {
				XmlBuilder warningXML=new XmlBuilder("warning");
				warningXML.addAttribute("config",selectedConfigurationWarnings.get(j)[0]);
				warningXML.setValue((String)selectedConfigurationWarnings.get(j)[1]);
				warningsXML.addSubElement(warningXML);
			}
			return warningsXML;
		}
		return null;
	}

	private List<ErrorStoreCounter> retrieveErrorStoreCounters(List<Configuration> configurations, Configuration configurationSelected) {
		List<ErrorStoreCounter> errorStoreCounters = new ArrayList<ErrorStoreCounter>();
		if (SHOW_COUNT_ERRORSTORE) {
			errorStoreCounters = new ArrayList<ErrorStoreCounter>();
			if (configurationSelected!=null) {
				String config = configurationSelected.getName();
				int counter = 0;
				for(Iterator adapterIt=configurationSelected.getRegisteredAdapters().iterator(); adapterIt.hasNext();) {
					Adapter adapter = (Adapter)adapterIt.next();
					counter += retrieveErrorStoragesMessageCount(adapter);
				}
				if (counter>0) {
					errorStoreCounters.add(new ErrorStoreCounter(config, counter));
				}
			} else {
				for (Configuration configuration : configurations) {
					String config = configuration.getName();
					int counter = 0;
					for(Iterator adapterIt=configuration.getRegisteredAdapters().iterator(); adapterIt.hasNext();) {
						Adapter adapter = (Adapter)adapterIt.next();
						counter += retrieveErrorStoragesMessageCount(adapter);
					}
					if (counter>0) {
						errorStoreCounters.add(new ErrorStoreCounter(config, counter));
					}
				}
			}
		}
		return errorStoreCounters;
	}

	private int retrieveErrorStoragesMessageCount(Adapter adapter) {
		int counter = 0;
		for(Iterator receiverIt=adapter.getReceiverIterator(); receiverIt.hasNext();) {
			ReceiverBase receiver=(ReceiverBase)receiverIt.next();
			ITransactionalStorage errorStorage=receiver.getErrorStorage();
			if (errorStorage!=null) {
				try {
					counter += errorStorage.getMessageCount();
				} catch (Exception e) {
					error("error occured on getting number of errorlog records for adapter ["+adapter.getName()+"]",e);
					log.warn("assuming there are no errorlog records for adapter ["+adapter.getName()+"]");
				}
			}
		}
		return counter;
	}

	private XmlBuilder toAdapterXml(Configuration configurationSelected, Adapter adapter, ShowConfigurationStatusManager showConfigurationStatusManager) {
		XmlBuilder adapterXML=new XmlBuilder("adapter");
		RunStateEnum adapterRunState = adapter.getRunState();
		String adapterName = adapter.getName();
		adapterXML.addAttribute("name",adapterName);
		String adapterDescription = adapter.getDescription();
		adapterXML.addAttribute("description",adapterDescription);
		adapterXML.addAttribute("config",adapter.getConfiguration().getName());
		adapterXML.addAttribute("configUC",Misc.toSortName(adapter.getConfiguration().getName()));
		adapterXML.addAttribute("nameUC",Misc.toSortName(adapterName));
		adapterXML.addAttribute("started", ""+(adapterRunState.equals(RunStateEnum.STARTED)));
		adapterXML.addAttribute("state", adapterRunState.toString());
		if (adapterRunState.equals(RunStateEnum.STARTING)) {
			showConfigurationStatusManager.countAdapterStateStarting++;
		} else if ((adapterRunState.equals(RunStateEnum.STARTED))) {
			showConfigurationStatusManager.countAdapterStateStarted++;
		} else if ((adapterRunState.equals(RunStateEnum.STOPPING))) {
			showConfigurationStatusManager.countAdapterStateStopping++;
		} else if ((adapterRunState.equals(RunStateEnum.STOPPED))) {
			showConfigurationStatusManager.countAdapterStateStopped++;
		} else if ((adapterRunState.equals(RunStateEnum.ERROR))) {
			showConfigurationStatusManager.countAdapterStateError++;
		}
		if (!(adapterRunState.equals(RunStateEnum.STARTED))) {
			showConfigurationStatusManager.stateAlert = true;
		}
		adapterXML.addAttribute("configured", ""+adapter.configurationSucceeded());
		Date statsUpSinceDate = adapter.getStatsUpSinceDate();
		
		if (statsUpSinceDate!=null) {
			if(statsUpSinceDate.getTime() > 0) {
				adapterXML.addAttribute("upSince", adapter.getStatsUpSince(DateUtils.FORMAT_GENERICDATETIME));
				adapterXML.addAttribute("upSinceAge", Misc.getAge(statsUpSinceDate.getTime()));
			}
			else
				adapterXML.addAttribute("upSince", "-");
		}
		adapterXML.addAttribute("lastMessageDate", adapter.getLastMessageDate(DateUtils.FORMAT_GENERICDATETIME));
		Date lastMessageDate = adapter.getLastMessageDateDate();
		if (lastMessageDate!=null) {
			adapterXML.addAttribute("lastMessageDateAge", Misc.getAge(lastMessageDate.getTime()));
		}
		adapterXML.addAttribute("messagesInProcess", ""+adapter.getNumOfMessagesInProcess());
		adapterXML.addAttribute("messagesProcessed", ""+adapter.getNumOfMessagesProcessed());
		adapterXML.addAttribute("messagesInError", ""+adapter.getNumOfMessagesInError());
	
		XmlBuilder receiversXml = toReceiversXml(configurationSelected, adapter, showConfigurationStatusManager);
		if (receiversXml!=null) {
			adapterXML.addSubElement(receiversXml); 
		}
		adapterXML.addAttribute("stateAlert",showConfigurationStatusManager.stateAlert);
		
		if (configurationSelected != null) {
			XmlBuilder pipesElem = toPipesXml(adapter);
			adapterXML.addSubElement(pipesElem);
		
			XmlBuilder adapterMessages=toAdapterMessagesXmlSelected(adapter, showConfigurationStatusManager);
			adapterXML.addSubElement(adapterMessages);
		} else {
			XmlBuilder adapterMessages=toAdapterMessagesXmlAll(adapter, showConfigurationStatusManager);
			adapterXML.addSubElement(adapterMessages);
		}
		return adapterXML;
	}

	private XmlBuilder toReceiversXml(Configuration configurationSelected, Adapter adapter, ShowConfigurationStatusManager showConfigurationStatusManager) {
		Iterator recIt=adapter.getReceiverIterator();
		if (!recIt.hasNext()){
			return null;
		}
		XmlBuilder receiversXML=new XmlBuilder("receivers");
		while (recIt.hasNext()){
			IReceiver receiver=(IReceiver) recIt.next();
			XmlBuilder receiverXML=new XmlBuilder("receiver");
			receiversXML.addSubElement(receiverXML);

			RunStateEnum receiverRunState = receiver.getRunState();
			 
			receiverXML.addAttribute("isStarted", ""+(receiverRunState.equals(RunStateEnum.STARTED)));
			receiverXML.addAttribute("state", receiverRunState.toString());
			if (receiverRunState.equals(RunStateEnum.STARTING)) {
				showConfigurationStatusManager.countReceiverStateStarting++;
			} else if ((receiverRunState.equals(RunStateEnum.STARTED))) {
				showConfigurationStatusManager.countReceiverStateStarted++;
			} else if ((receiverRunState.equals(RunStateEnum.STOPPING))) {
				showConfigurationStatusManager.countReceiverStateStopping++;
			} else if ((receiverRunState.equals(RunStateEnum.STOPPED))) {
				showConfigurationStatusManager.countReceiverStateStopped++;
			} else if ((receiverRunState.equals(RunStateEnum.ERROR))) {
				showConfigurationStatusManager.countReceiverStateError++;
			}
			if (!showConfigurationStatusManager.stateAlert) {
				if (!(receiverRunState.equals(RunStateEnum.STARTED))) {
					showConfigurationStatusManager.stateAlert = true;
				}
			}
			receiverXML.addAttribute("name",receiver.getName());
			receiverXML.addAttribute("class", ClassUtils.nameOf(receiver));
			receiverXML.addAttribute("messagesReceived", ""+receiver.getMessagesReceived());
			receiverXML.addAttribute("messagesRetried", ""+receiver.getMessagesRetried());
			receiverXML.addAttribute("messagesRejected", ""+receiver.getMessagesRejected());
			if (configurationSelected != null) {
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
					ITransactionalStorage ts;
					ts=rb.getErrorStorage();
					receiverXML.addAttribute("hasErrorStorage", ""+(ts!=null));
					if (ts!=null) {
						try {
							if (SHOW_COUNT_ERRORSTORE) {
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
							if (SHOW_COUNT_MESSAGELOG) {
								receiverXML.addAttribute("messageLogCount", ts.getMessageCount());
							} else {
								receiverXML.addAttribute("messageLogCount", "?");
							}
						} catch (Exception e) {
							log.warn(e);
							receiverXML.addAttribute("messageLogCount", "error");
						}
					}
					boolean isRestListener = (listener instanceof RestListener);
					receiverXML.addAttribute("isRestListener", isRestListener);
					if (isRestListener) {
						RestListener rl = (RestListener) listener;
						receiverXML.addAttribute("restUriPattern", rl.getRestUriPattern());
						receiverXML.addAttribute("isView", (rl.isView()==null?false:rl.isView()));
					}
					if (showConfigurationStatusManager.count) {
						if (listener instanceof JmsListenerBase) {
							JmsListenerBase jlb = (JmsListenerBase) listener;
							JmsMessageBrowser jmsBrowser;
							if (StringUtils.isEmpty(jlb
									.getMessageSelector())) {
								jmsBrowser = new JmsMessageBrowser();
							} else {
								jmsBrowser = new JmsMessageBrowser(
										jlb.getMessageSelector());
							}
							jmsBrowser.setName("MessageBrowser_"
									+ jlb.getName());
							jmsBrowser.setJmsRealm(jlb.getJmsRealName());
							jmsBrowser.setDestinationName(jlb
									.getDestinationName());
							jmsBrowser.setDestinationType(jlb
									.getDestinationType());
							String numMsgs;
							try {
								int messageCount = jmsBrowser
										.getMessageCount();
								numMsgs = String.valueOf(messageCount);
							} catch (Throwable t) {
								log.warn(t);
								numMsgs = "?";
							}
							receiverXML.addAttribute(
									"pendingMessagesCount", numMsgs);
						}
					}
					boolean isEsbJmsFFListener = false;
					if (listener instanceof EsbJmsListener) {
						EsbJmsListener ejl = (EsbJmsListener) listener;
						if ("FF".equalsIgnoreCase(ejl.getMessageProtocol())) {
							isEsbJmsFFListener = true;
						}
						if (showConfigurationStatusManager.count) {
							String esbNumMsgs = EsbUtils.getQueueMessageCount(ejl);
							if (esbNumMsgs == null) {
								esbNumMsgs = "?";
							}
							receiverXML.addAttribute(
									"esbPendingMessagesCount", esbNumMsgs);
						}
					}
					receiverXML.addAttribute("isEsbJmsFFListener", isEsbJmsFFListener);
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
		}
		return receiversXML;
	}

	private XmlBuilder toPipesXml(Adapter adapter) {
		XmlBuilder pipesElem = new XmlBuilder("pipes");
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
						if (SHOW_COUNT_MESSAGELOG) {
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
		return pipesElem;
	}

	private XmlBuilder toAdapterMessagesXmlSelected(Adapter adapter, ShowConfigurationStatusManager showConfigurationStatusManager) {
		XmlBuilder adapterMessages=new XmlBuilder("adapterMessages");
		for (int t=0; t<adapter.getMessageKeeper().size(); t++) {
			XmlBuilder adapterMessage=new XmlBuilder("adapterMessage");
			String msg = adapter.getMessageKeeper().getMessage(t).getMessageText();
			if (MAX_MESSAGE_SIZE>0 && msg.length()>MAX_MESSAGE_SIZE) {
				msg = msg.substring(0, MAX_MESSAGE_SIZE) + "...(" + (msg.length()-MAX_MESSAGE_SIZE) + " characters more)";
			}
			adapterMessage.setValue(msg,true);
			adapterMessage.addAttribute("date", DateUtils.format(adapter.getMessageKeeper().getMessage(t).getMessageDate(), DateUtils.FORMAT_FULL_GENERIC));
			String level = adapter.getMessageKeeper().getMessage(t).getMessageLevel();
			adapterMessage.addAttribute("level", level);
			adapterMessages.addSubElement(adapterMessage);
			if (level.equals(MessageKeeperMessage.ERROR_LEVEL)) {
				showConfigurationStatusManager.countMessagesError++;
			} else {
				if (level.equals(MessageKeeperMessage.WARN_LEVEL)) {
					showConfigurationStatusManager.countMessagesWarn++;
				} else {
					showConfigurationStatusManager.countMessagesInfo++;
				}
			}
		}
		return adapterMessages;
	}

	private XmlBuilder toAdapterMessagesXmlAll(Adapter adapter, ShowConfigurationStatusManager showConfigurationStatusManager) {
		XmlBuilder adapterMessages=new XmlBuilder("adapterMessages");
		int cme=0;
		int cmw=0;
		int cmi=0;
		for (int t=0; t<adapter.getMessageKeeper().size(); t++) {
			String level = adapter.getMessageKeeper().getMessage(t).getMessageLevel();
			if (level.equals(MessageKeeperMessage.ERROR_LEVEL)) {
				cme++;
			} else {
				if (level.equals(MessageKeeperMessage.WARN_LEVEL)) {
					cmw++;
				} else {
					cmi++;
				}
			}
		}
		adapterMessages.addAttribute("error", cme+"");
		adapterMessages.addAttribute("warn", cmw+"");
		adapterMessages.addAttribute("info", cmi+"");
		showConfigurationStatusManager.countMessagesError += cme;
		showConfigurationStatusManager.countMessagesWarn += cmw;
		showConfigurationStatusManager.countMessagesInfo += cmi;
		return adapterMessages;
	}

	private XmlBuilder toSummaryXml(ShowConfigurationStatusManager showConfigurationStatusManager) {
		XmlBuilder summaryXML=new XmlBuilder("summary");
		XmlBuilder adapterStateXML=new XmlBuilder("adapterState");
		adapterStateXML.addAttribute("starting",showConfigurationStatusManager.countAdapterStateStarting+"");
		adapterStateXML.addAttribute("started",showConfigurationStatusManager.countAdapterStateStarted+"");
		adapterStateXML.addAttribute("stopping",showConfigurationStatusManager.countAdapterStateStopping+"");
		adapterStateXML.addAttribute("stopped",showConfigurationStatusManager.countAdapterStateStopped+"");
		adapterStateXML.addAttribute("error",showConfigurationStatusManager.countAdapterStateError+"");
		summaryXML.addSubElement(adapterStateXML);
		XmlBuilder receiverStateXML=new XmlBuilder("receiverState");
		receiverStateXML.addAttribute("starting",showConfigurationStatusManager.countReceiverStateStarting+"");
		receiverStateXML.addAttribute("started",showConfigurationStatusManager.countReceiverStateStarted+"");
		receiverStateXML.addAttribute("stopping",showConfigurationStatusManager.countReceiverStateStopping+"");
		receiverStateXML.addAttribute("stopped",showConfigurationStatusManager.countReceiverStateStopped+"");
		receiverStateXML.addAttribute("error",showConfigurationStatusManager.countReceiverStateError+"");
		summaryXML.addSubElement(receiverStateXML);
		XmlBuilder messageLevelXML=new XmlBuilder("messageLevel");
		messageLevelXML.addAttribute("error",showConfigurationStatusManager.countMessagesError+"");
		messageLevelXML.addAttribute("warn",showConfigurationStatusManager.countMessagesWarn+"");
		messageLevelXML.addAttribute("info",showConfigurationStatusManager.countMessagesInfo+"");
		summaryXML.addSubElement(messageLevelXML);
		return summaryXML;
	}

}
