/*
   Copyright 2018, 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.webcontrol.pipes;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.BaseConfigurationWarnings;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.extensions.esb.EsbJmsListener;
import nl.nn.adapterframework.extensions.esb.EsbUtils;
import nl.nn.adapterframework.http.RestListener;
import nl.nn.adapterframework.http.RestServiceDispatcher;
import nl.nn.adapterframework.http.WebServiceListener;
import nl.nn.adapterframework.http.rest.ApiDispatchConfig;
import nl.nn.adapterframework.http.rest.ApiListener;
import nl.nn.adapterframework.http.rest.ApiServiceDispatcher;
import nl.nn.adapterframework.jdbc.JdbcSenderBase;
import nl.nn.adapterframework.jms.JmsBrowser;
import nl.nn.adapterframework.jms.JmsListenerBase;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.task.TimeoutGuard;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.MessageKeeper.MessageKeeperLevel;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Prepare the main screen of the IbisConsole.
 * 
 * @author Johan Verrips
 * @author Peter Leeuwenburgh
 */

public class ShowConfigurationStatus extends ConfigurationBase {
	private static final int MAX_MESSAGE_SIZE = AppConstants.getInstance().getInt("adapter.message.max.size", 0);
	private static final boolean SHOW_COUNT_MESSAGELOG = AppConstants.getInstance().getBoolean("messageLog.count.show",
			true);
	private static final boolean SHOW_COUNT_ERRORSTORE = AppConstants.getInstance().getBoolean("errorStore.count.show",
			true);

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
		Boolean alert = null;
		boolean log = false;
		int countAdapterStateStarting = 0;
		int countAdapterStateStarted = 0;
		int countAdapterStateStopping = 0;
		int countAdapterStateStopped = 0;
		int countAdapterStateError = 0;
		int countReceiverStateStarting = 0;
		int countReceiverStateStarted = 0;
		int countReceiverStateStartedNotAvailable = 0;
		int countReceiverStateStopping = 0;
		int countReceiverStateStopped = 0;
		int countReceiverStateError = 0;
		int countMessagesInfo = 0;
		int countMessagesWarn = 0;
		int countMessagesError = 0;
		int countLastMsgProcessOk = 0;
		int countLastMsgProcessError = 0;
		int countLastMsgProcessNotApplicable = 0;
	}

	private class ShowConfigurationStatusAdapterManager {
		boolean stateAlert = false;
		// stateAlert = true when adapter.runState<>'STARTED' or receiver.runState<>'STARTED' or adapter.lastMessageProcessState='ERROR'
		boolean logAlert = false;
		// logAlert = true when (lastMessageLevel='ERROR' or 'WARN')
	}

	@Override
	protected String doGet(IPipeLineSession session) throws PipeRunException {
		IbisManager ibisManager = retrieveIbisManager();

		String configurationName = null;

		ShowConfigurationStatusManager showConfigurationStatusManager = new ShowConfigurationStatusManager();

		String countStr = (String) session.get("count");
		showConfigurationStatusManager.count = Boolean.parseBoolean(countStr);

		String alertStr = (String) session.get("alert");
		if (alertStr != null) {
			showConfigurationStatusManager.alert = Boolean.parseBoolean(alertStr);
			configurationName = CONFIG_ALL;
		}

		String logStr = (String) session.get("log");
		showConfigurationStatusManager.log = Boolean.parseBoolean(logStr);
		
		if (configurationName == null) {
			configurationName = retrieveConfigurationName(session);
		}
		Configuration configuration = null;
		boolean configAll;
		if (configurationName == null || configurationName.equalsIgnoreCase(CONFIG_ALL)) {
			configAll = true;
		} else {
			configuration = ibisManager.getConfiguration(configurationName);
			if (configuration == null) {
				configAll = true;
			} else {
				configAll = false;
			}
		}

		List<Configuration> allConfigurations = ibisManager.getConfigurations();
		XmlBuilder configurationsXml = toConfigurationsXml(allConfigurations);

		List<IAdapter> registeredAdapters = retrieveRegisteredAdapters(ibisManager, configAll, configuration);
		storeConfiguration(session, configAll, configuration);

		XmlBuilder adapters = toAdaptersXml(ibisManager, allConfigurations, configuration, registeredAdapters,
				showConfigurationStatusManager);

		XmlBuilder root = new XmlBuilder("root");
		root.addSubElement(configurationsXml);
		root.addSubElement(adapters);

		return root.toXML();
	}

	private List<IAdapter> retrieveRegisteredAdapters(IbisManager ibisManager, boolean configAll,
			Configuration configuration) {
		if (configAll) {
			return ibisManager.getRegisteredAdapters();
		} else {
			return configuration.getRegisteredAdapters();
		}
	}

	private XmlBuilder toAdaptersXml(IbisManager ibisManager, List<Configuration> configurations,
			Configuration configurationSelected, List<IAdapter> registeredAdapters,
			ShowConfigurationStatusManager showConfigurationStatusManager) {
		XmlBuilder adapters = new XmlBuilder("registeredAdapters");
		if (configurationSelected != null) {
			adapters.addAttribute("all", false);
		} else {
			adapters.addAttribute("all", true);
		}
		if (showConfigurationStatusManager.alert != null) {
			adapters.addAttribute("alert", showConfigurationStatusManager.alert);
		}
		adapters.addAttribute("log", showConfigurationStatusManager.log);

		XmlBuilder configurationMessagesXml = toConfigurationMessagesXml(ibisManager, configurationSelected);
		adapters.addSubElement(configurationMessagesXml);

		XmlBuilder configurationExceptionsXml = toConfigurationExceptionsXml(configurations, configurationSelected);
		if (configurationExceptionsXml != null) {
			adapters.addSubElement(configurationExceptionsXml);
		}

		XmlBuilder configurationWarningsXml = toConfigurationWarningsXml(configurations, configurationSelected);
		if (configurationWarningsXml != null) {
			adapters.addSubElement(configurationWarningsXml);
		}

		for (IAdapter iAdapter : registeredAdapters) {
			Adapter adapter = (Adapter) iAdapter;
			XmlBuilder adapterXml = toAdapterXml(configurationSelected, adapter, showConfigurationStatusManager);
			adapters.addSubElement(adapterXml);
		}

		XmlBuilder summaryXml = toSummaryXml(showConfigurationStatusManager);
		adapters.addSubElement(summaryXml);
		return adapters;
	}

	private XmlBuilder toConfigurationMessagesXml(IbisManager ibisManager, Configuration configurationSelected) {
		XmlBuilder configurationMessages = new XmlBuilder("configurationMessages");
		MessageKeeper messageKeeper;
		if (configurationSelected != null) {
			messageKeeper = ibisManager.getIbisContext().getMessageKeeper(configurationSelected.getName());
		} else {
			messageKeeper = ibisManager.getIbisContext().getMessageKeeper(CONFIG_ALL);
		}

		for (int t = 0; t < messageKeeper.size(); t++) {
			XmlBuilder configurationMessage = new XmlBuilder("configurationMessage");
			String msg = messageKeeper.getMessage(t).getMessageText();
			if (MAX_MESSAGE_SIZE > 0 && msg.length() > MAX_MESSAGE_SIZE) {
				msg = msg.substring(0, MAX_MESSAGE_SIZE) + "...(" + (msg.length() - MAX_MESSAGE_SIZE)
						+ " characters more)";
			}
			configurationMessage.setValue(msg, true);
			configurationMessage.addAttribute("date",
					DateUtils.format(messageKeeper.getMessage(t).getMessageDate(), DateUtils.FORMAT_FULL_GENERIC));
			String level = messageKeeper.getMessage(t).getMessageLevel();
			configurationMessage.addAttribute("level", level);
			configurationMessages.addSubElement(configurationMessage);
		}
		return configurationMessages;
	}

	private XmlBuilder toConfigurationExceptionsXml(List<Configuration> configurations,
			Configuration configurationSelected) {
		List<String[]> configurationExceptions = new ArrayList<String[]>();
		if (configurationSelected != null) {
			if (configurationSelected.getConfigurationException() != null) {
				String[] item = new String[2];
				item[0] = configurationSelected.getName();
				item[1] = configurationSelected.getConfigurationException().getMessage();
				configurationExceptions.add(item);
			}
		} else {
			for (Configuration configuration : configurations) {
				if (configuration.getConfigurationException() != null) {
					String[] item = new String[2];
					item[0] = configuration.getName();
					item[1] = configuration.getConfigurationException().getMessage();
					configurationExceptions.add(item);
				}
			}
		}
		if (!configurationExceptions.isEmpty()) {
			XmlBuilder exceptionsXML = new XmlBuilder("exceptions");
			for (int j = 0; j < configurationExceptions.size(); j++) {
				XmlBuilder exceptionXML = new XmlBuilder("exception");
				exceptionXML.addAttribute("config", configurationExceptions.get(j)[0]);
				exceptionXML.setValue(configurationExceptions.get(j)[1]);
				exceptionsXML.addSubElement(exceptionXML);
			}
			return exceptionsXML;
		}
		return null;
	}

	private XmlBuilder toConfigurationWarningsXml(List<Configuration> configurations,
			Configuration configurationSelected) {
		ConfigurationWarnings globalConfigurationWarnings = ConfigurationWarnings.getInstance();

		List<ErrorStoreCounter> errorStoreCounters = retrieveErrorStoreCounters(configurations, configurationSelected);

		List<String[]> selectedConfigurationWarnings = new ArrayList<String[]>();
		if (configurationSelected != null) {
			BaseConfigurationWarnings configWarns = configurationSelected.getConfigurationWarnings();
			for (int j = 0; j < configWarns.size(); j++) {
				String[] item = new String[2];
				item[0] = configurationSelected.getName();
				item[1] = (String) configWarns.get(j);
				selectedConfigurationWarnings.add(item);
			}
		} else {
			for (Configuration configuration : configurations) {
				BaseConfigurationWarnings configWarns = configuration.getConfigurationWarnings();
				for (int j = 0; j < configWarns.size(); j++) {
					String[] item = new String[2];
					item[0] = configuration.getName();
					item[1] = (String) configWarns.get(j);
					selectedConfigurationWarnings.add(item);
				}
			}
		}
		if (!globalConfigurationWarnings.isEmpty() || errorStoreCounters == null || !errorStoreCounters.isEmpty() || !SHOW_COUNT_ERRORSTORE
				|| !selectedConfigurationWarnings.isEmpty()) {
			XmlBuilder warningsXML = new XmlBuilder("warnings");
			if (!SHOW_COUNT_ERRORSTORE) {
				XmlBuilder warningXML = new XmlBuilder("warning");
				warningXML.setValue(
						"Errorlog might contain records. This is unknown because errorStore.count.show is not set to true");
				warningXML.addAttribute("severe", true);
				warningsXML.addSubElement(warningXML);
			}
			if (errorStoreCounters == null) {
				XmlBuilder warningXML = new XmlBuilder("warning");
				warningXML.setValue(
						"Errorlog could not be retrieved. See logging for more information");
				warningXML.addAttribute("severe", true);
				warningsXML.addSubElement(warningXML);
			} else {
				for (int j = 0; j < errorStoreCounters.size(); j++) {
					ErrorStoreCounter esr = errorStoreCounters.get(j);
					XmlBuilder warningXML = new XmlBuilder("warning");
					warningXML.addAttribute("config", esr.config);
					if (esr.counter == 1) {
						warningXML.setValue(
								"Errorlog contains 1 record. Service management should check whether this record has to be resent or deleted");
					} else {
						warningXML.setValue("Errorlog contains " + esr.counter
								+ " records. Service Management should check whether these records have to be resent or deleted");
					}
					warningXML.addAttribute("severe", true);
					warningsXML.addSubElement(warningXML);
				}
			}
			for (int j = 0; j < globalConfigurationWarnings.size(); j++) {
				XmlBuilder warningXML = new XmlBuilder("warning");
				warningXML.setValue((String) globalConfigurationWarnings.get(j));
				warningsXML.addSubElement(warningXML);
			}
			for (int j = 0; j < selectedConfigurationWarnings.size(); j++) {
				XmlBuilder warningXML = new XmlBuilder("warning");
				warningXML.addAttribute("config", selectedConfigurationWarnings.get(j)[0]);
				warningXML.setValue((String) selectedConfigurationWarnings.get(j)[1]);
				warningsXML.addSubElement(warningXML);
			}
			return warningsXML;
		}
		return null;
	}

	private List<ErrorStoreCounter> retrieveErrorStoreCounters(List<Configuration> configurations,
			Configuration configurationSelected) {
		List<ErrorStoreCounter> errorStoreCounters = new ArrayList<ErrorStoreCounter>();
		if (SHOW_COUNT_ERRORSTORE) {
			errorStoreCounters = new ArrayList<ErrorStoreCounter>();
			if (configurationSelected != null) {
				String config = configurationSelected.getName();
				int totalCounter = 0;
				for (Iterator adapterIt = configurationSelected.getRegisteredAdapters().iterator(); adapterIt
						.hasNext();) {
					Adapter adapter = (Adapter) adapterIt.next();
					int counter = retrieveErrorStoragesMessageCount(adapter);
					if (counter < 0) {
						// error occured
						return null;
					} else {
						totalCounter += counter;
					}
				}
				if (totalCounter > 0) {
					errorStoreCounters.add(new ErrorStoreCounter(config, totalCounter));
				}
			} else {
				for (Configuration configuration : configurations) {
					String config = configuration.getName();
					int totalCounter = 0;
					for (Iterator adapterIt = configuration.getRegisteredAdapters().iterator(); adapterIt.hasNext();) {
						Adapter adapter = (Adapter) adapterIt.next();
						int counter = retrieveErrorStoragesMessageCount(adapter);
						if (counter < 0) {
							// error occured
							return null;
						} else {
							totalCounter += counter;
						}
					}
					if (totalCounter > 0) {
						errorStoreCounters.add(new ErrorStoreCounter(config, totalCounter));
					}
				}
			}
		}
		return errorStoreCounters;
	}

	private int retrieveErrorStoragesMessageCount(Adapter adapter) {
		int totalCounter = 0;
		for (Iterator receiverIt = adapter.getReceiverIterator(); receiverIt.hasNext();) {
			ReceiverBase receiver = (ReceiverBase) receiverIt.next();
			IMessageBrowser errorStorage = receiver.getErrorStorageBrowser();
			if (errorStorage != null) {
				int counter;
				try {
					counter = getErrorStorageMessageCountWithTimeout(errorStorage, 10);
				} catch (Exception e) {
					log.warn("error occured on getting number of errorlog records for adapter [" + adapter.getName() + "]", e);
					return -1;
				}
				totalCounter += counter;
			}
		}
		return totalCounter;
	}

	private int getErrorStorageMessageCountWithTimeout(IMessageBrowser errorStorage, int timeout) throws ListenerException, TimeOutException {
		if (timeout <= 0) {
			return errorStorage.getMessageCount();
		}
		TimeoutGuard tg = new TimeoutGuard("ErrorMessageCount ");
		try {
			tg.activateGuard(timeout);
			return errorStorage.getMessageCount();
		} finally {
			if (tg.cancel()) {
				throw new TimeOutException("thread has been interrupted");
			}
		}
	}
	
	private XmlBuilder toAdapterXml(Configuration configurationSelected, Adapter adapter,
			ShowConfigurationStatusManager showConfigurationStatusManager) {
		ShowConfigurationStatusAdapterManager showConfigurationStatusAdapterManager = new ShowConfigurationStatusAdapterManager();

		XmlBuilder adapterXML = new XmlBuilder("adapter");
		RunStateEnum adapterRunState = adapter.getRunState();
		String adapterName = adapter.getName();
		adapterXML.addAttribute("name", adapterName);
		String adapterDescription = adapter.getDescription();
		adapterXML.addAttribute("description", adapterDescription);
		adapterXML.addAttribute("config", adapter.getConfiguration().getName());
		adapterXML.addAttribute("configUC", Misc.toSortName(adapter.getConfiguration().getName()));
		adapterXML.addAttribute("nameUC", Misc.toSortName(adapterName));
		adapterXML.addAttribute("started", "" + (adapterRunState.equals(RunStateEnum.STARTED)));
		adapterXML.addAttribute("state", adapterRunState.toString());
		adapterXML.addAttribute("lastMsgProcessState", adapter.getLastMessageProcessingState());
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
			showConfigurationStatusAdapterManager.stateAlert = true;
		}
		String lastMsgProcessingState = adapter.getLastMessageProcessingState();
		if (StringUtils.isNotEmpty(lastMsgProcessingState)) {
			adapterXML.addAttribute("lastMsgProcessState", lastMsgProcessingState);
		} else {
			adapterXML.addAttribute("lastMsgProcessState", "-");
		}
		if (Adapter.PROCESS_STATE_OK.equals(lastMsgProcessingState)) {
			showConfigurationStatusManager.countLastMsgProcessOk++;
		} else if (Adapter.PROCESS_STATE_ERROR.equals(lastMsgProcessingState)) {
			showConfigurationStatusManager.countLastMsgProcessError++;
			if (!showConfigurationStatusAdapterManager.stateAlert) {
				showConfigurationStatusAdapterManager.stateAlert = true;
			}
		} else {
			showConfigurationStatusManager.countLastMsgProcessNotApplicable++;
		}
		adapterXML.addAttribute("configured", "" + adapter.configurationSucceeded());
		Date statsUpSinceDate = adapter.getStatsUpSinceDate();

		if (statsUpSinceDate != null) {
			if (statsUpSinceDate.getTime() > 0) {
				adapterXML.addAttribute("upSince", adapter.getStatsUpSince(DateUtils.FORMAT_GENERICDATETIME));
				adapterXML.addAttribute("upSinceAge", Misc.getAge(statsUpSinceDate.getTime()));
			} else
				adapterXML.addAttribute("upSince", "-");
		}
		adapterXML.addAttribute("lastMessageDate", adapter.getLastMessageDate(DateUtils.FORMAT_GENERICDATETIME));
		Date lastMessageDate = adapter.getLastMessageDateDate();
		if (lastMessageDate != null) {
			adapterXML.addAttribute("lastMessageDateAge", Misc.getAge(lastMessageDate.getTime()));
		}
		adapterXML.addAttribute("messagesInProcess", "" + adapter.getNumOfMessagesInProcess());
		adapterXML.addAttribute("messagesProcessed", "" + adapter.getNumOfMessagesProcessed());
		adapterXML.addAttribute("messagesInError", "" + adapter.getNumOfMessagesInError());

		XmlBuilder receiversXml = toReceiversXml(configurationSelected, adapter, showConfigurationStatusManager,
				showConfigurationStatusAdapterManager);
		if (receiversXml != null) {
			adapterXML.addSubElement(receiversXml);
		}

		if (configurationSelected != null) {
			XmlBuilder pipesElem = toPipesXml(adapter);
			adapterXML.addSubElement(pipesElem);

			XmlBuilder adapterMessages = toAdapterMessagesXmlSelected(adapter, showConfigurationStatusManager);
			adapterXML.addSubElement(adapterMessages);
		} else {
			XmlBuilder adapterMessages = toAdapterMessagesXmlAll(adapter, showConfigurationStatusManager,
					showConfigurationStatusAdapterManager);
			adapterXML.addSubElement(adapterMessages);
		}
		adapterXML.addAttribute("stateAlert", showConfigurationStatusAdapterManager.stateAlert);
		adapterXML.addAttribute("logAlert", showConfigurationStatusAdapterManager.logAlert);
		return adapterXML;
	}

	private XmlBuilder toReceiversXml(Configuration configurationSelected, Adapter adapter, ShowConfigurationStatusManager showConfigurationStatusManager, ShowConfigurationStatusAdapterManager showConfigurationStatusAdapterManager) {
		Iterator recIt = adapter.getReceiverIterator();
		if (!recIt.hasNext()) {
			return null;
		}
		XmlBuilder receiversXML = new XmlBuilder("receivers");
		while (recIt.hasNext()) {
			IReceiver receiver = (IReceiver) recIt.next();
			XmlBuilder receiverXML = new XmlBuilder("receiver");
			receiversXML.addSubElement(receiverXML);

			RunStateEnum receiverRunState = receiver.getRunState();
			boolean isAvailable = true;

			receiverXML.addAttribute("isStarted", "" + (receiverRunState.equals(RunStateEnum.STARTED)));
			receiverXML.addAttribute("state", receiverRunState.toString());
			if (receiverRunState.equals(RunStateEnum.STARTING)) {
				showConfigurationStatusManager.countReceiverStateStarting++;
			} else if ((receiverRunState.equals(RunStateEnum.STARTED))) {
				isAvailable = isAvailable(receiver);
				if (isAvailable) {
					showConfigurationStatusManager.countReceiverStateStarted++;
				} else {
					showConfigurationStatusManager.countReceiverStateStartedNotAvailable++;
				}
			} else if ((receiverRunState.equals(RunStateEnum.STOPPING))) {
				showConfigurationStatusManager.countReceiverStateStopping++;
			} else if ((receiverRunState.equals(RunStateEnum.STOPPED))) {
				showConfigurationStatusManager.countReceiverStateStopped++;
			} else if ((receiverRunState.equals(RunStateEnum.ERROR))) {
				showConfigurationStatusManager.countReceiverStateError++;
			}
			if (!showConfigurationStatusAdapterManager.stateAlert && !(receiverRunState.equals(RunStateEnum.STARTED) && isAvailable)) {
				showConfigurationStatusAdapterManager.stateAlert = true;
			}
			receiverXML.addAttribute("name", receiver.getName());
			receiverXML.addAttribute("class", ClassUtils.nameOf(receiver));
			receiverXML.addAttribute("messagesReceived", "" + receiver.getMessagesReceived());
			receiverXML.addAttribute("messagesRetried", "" + receiver.getMessagesRetried());
			receiverXML.addAttribute("messagesRejected", "" + receiver.getMessagesRejected());
			if (!isAvailable) {
				receiverXML.addAttribute("isAvailable", false);
			}
			if (configurationSelected != null) {
				ISender sender = null;
				if (receiver instanceof ReceiverBase) {
					ReceiverBase rb = (ReceiverBase) receiver;
					IListener listener = rb.getListener();
					receiverXML.addAttribute("listenerClass", ClassUtils.nameOf(listener));
					if (listener instanceof HasPhysicalDestination) {
						String pd = ((HasPhysicalDestination) rb.getListener()).getPhysicalDestinationName();
						receiverXML.addAttribute("listenerDestination", pd);
					}
					if (listener instanceof HasSender) {
						sender = ((HasSender) listener).getSender();
					}
					IMessageBrowser ts = rb.getErrorStorageBrowser();
					receiverXML.addAttribute("hasErrorStorage", "" + (ts != null));
					if (ts != null) {
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
					ts = rb.getMessageLogBrowser();
					receiverXML.addAttribute("hasMessageLog", "" + (ts != null));
					if (ts != null) {
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
						receiverXML.addAttribute("isView", (rl.isView() != null));
					}
					if (showConfigurationStatusManager.count) {
						if (listener instanceof JmsListenerBase) {
							JmsListenerBase jlb = (JmsListenerBase) listener;
							JmsBrowser<javax.jms.Message> jmsBrowser;
							if (StringUtils.isEmpty(jlb.getMessageSelector())) {
								jmsBrowser = new JmsBrowser<>();
							} else {
								jmsBrowser = new JmsBrowser<>(jlb.getMessageSelector());
							}
							jmsBrowser.setName("MessageBrowser_" + jlb.getName());
							jmsBrowser.setJmsRealm(jlb.getJmsRealName());
							jmsBrowser.setDestinationName(jlb.getDestinationName());
							jmsBrowser.setDestinationType(jlb.getDestinationType());
							String numMsgs;
							try {
								int messageCount = jmsBrowser.getMessageCount();
								numMsgs = String.valueOf(messageCount);
							} catch (Throwable t) {
								log.warn(t);
								numMsgs = "?";
							}
							receiverXML.addAttribute("pendingMessagesCount", numMsgs);
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
							receiverXML.addAttribute("esbPendingMessagesCount", esbNumMsgs);
						}
					}
					receiverXML.addAttribute("isEsbJmsFFListener", isEsbJmsFFListener);
				}

				if (receiver instanceof HasSender) {
					ISender rsender = ((HasSender) receiver).getSender();
					if (rsender != null) { // this sender has preference, but
											// avoid overwriting listeners
											// sender with null
						sender = rsender;
					}
				}
				if (sender != null) {
					receiverXML.addAttribute("senderName", sender.getName());
					receiverXML.addAttribute("senderClass", ClassUtils.nameOf(sender));
					if (sender instanceof HasPhysicalDestination) {
						String pd = ((HasPhysicalDestination) sender).getPhysicalDestinationName();
						receiverXML.addAttribute("senderDestination", pd);
					}
				}
				if (receiver instanceof IThreadCountControllable) {
					IThreadCountControllable tcc = (IThreadCountControllable) receiver;
					if (tcc.isThreadCountReadable()) {
						receiverXML.addAttribute("threadCount", tcc.getCurrentThreadCount() + "");
						receiverXML.addAttribute("maxThreadCount", tcc.getMaxThreadCount() + "");
					}
					if (tcc.isThreadCountControllable()) {
						receiverXML.addAttribute("threadCountControllable", "true");
					}
				}
			}
		}
		return receiversXML;

	}

	private boolean isAvailable(IReceiver receiver) {
		if (receiver instanceof ReceiverBase) {
			ReceiverBase rb = (ReceiverBase) receiver;
			IListener listener = rb.getListener();
			boolean isRestListener = (listener instanceof RestListener);
			boolean isJavaListener = (listener instanceof JavaListener);
			boolean isWebServiceListener = (listener instanceof WebServiceListener);
			boolean isApiListener = (listener instanceof ApiListener);
			if (isRestListener) {
				RestListener rl = (RestListener) listener;
				String matchingPattern = RestServiceDispatcher.getInstance().findMatchingPattern("/" + rl.getUriPattern());
				return matchingPattern != null;
			} else if (isJavaListener) {
				JavaListener jl = (JavaListener) listener;
				if (StringUtils.isNotEmpty(jl.getName())) {
					JavaListener jlRegister = JavaListener.getListener(jl.getName());
					return jlRegister == jl;
				}
			} else if (isWebServiceListener) {
				WebServiceListener wsl = (WebServiceListener) listener;
				if (StringUtils.isNotEmpty(wsl.getServiceNamespaceURI())) {
					WebServiceListener wslRegister = (WebServiceListener) ServiceDispatcher.getInstance().getListener(wsl.getServiceNamespaceURI());
					return wslRegister == wsl;
				} else {
					WebServiceListener wslRegister = (WebServiceListener) ServiceDispatcher.getInstance().getListener(wsl.getName());
					return wslRegister == wsl;
				}
			} else if (isApiListener) {
				ApiListener al = (ApiListener) listener;
				ApiDispatchConfig apiDispatchConfig = ApiServiceDispatcher.getInstance().findConfigForUri(al.getUriPattern());
				if (apiDispatchConfig == null) {
					return false;
				}
				ApiListener alRegister = apiDispatchConfig.getApiListener(al.getMethod());
				return alRegister == al;
			}
		}
		// default a receiver is available
		return true;
	}
	
	private XmlBuilder toPipesXml(Adapter adapter) {
		XmlBuilder pipesElem = new XmlBuilder("pipes");
		PipeLine pipeline = adapter.getPipeLine();
		for (int i = 0; i < pipeline.getPipes().size(); i++) {
			IPipe pipe = pipeline.getPipe(i);
			String pipename = pipe.getName();
			if (pipe instanceof MessageSendingPipe) {
				MessageSendingPipe msp = (MessageSendingPipe) pipe;
				XmlBuilder pipeElem = new XmlBuilder("pipe");
				pipeElem.addAttribute("name", pipename);
				pipesElem.addSubElement(pipeElem);
				ISender sender = msp.getSender();
				pipeElem.addAttribute("sender", ClassUtils.nameOf(sender));
				if (sender instanceof HasPhysicalDestination) {
					pipeElem.addAttribute("destination",
							((HasPhysicalDestination) sender).getPhysicalDestinationName());
				}
				if (sender instanceof JdbcSenderBase) {
					pipeElem.addAttribute("isJdbcSender", "true");
				}
				IListener listener = msp.getListener();
				if (listener != null) {
					pipeElem.addAttribute("listenerName", listener.getName());
					pipeElem.addAttribute("listenerClass", ClassUtils.nameOf(listener));
					if (listener instanceof HasPhysicalDestination) {
						String pd = ((HasPhysicalDestination) listener).getPhysicalDestinationName();
						pipeElem.addAttribute("listenerDestination", pd);
					}
				}
				ITransactionalStorage messageLog = msp.getMessageLog();
				if (messageLog != null) {
					pipeElem.addAttribute("hasMessageLog", "true");
					String messageLogCount;
					try {
						if (SHOW_COUNT_MESSAGELOG) {
							messageLogCount = "" + messageLog.getMessageCount();
						} else {
							messageLogCount = "?";
						}
					} catch (Exception e) {
						log.warn(e);
						messageLogCount = "error";
					}
					pipeElem.addAttribute("messageLogCount", messageLogCount);
					XmlBuilder browserElem = new XmlBuilder("browser");
					browserElem.addAttribute("name", messageLog.getName());
					browserElem.addAttribute("type", "log");
					browserElem.addAttribute("slotId", messageLog.getSlotId());
					browserElem.addAttribute("count", messageLogCount);
					pipeElem.addSubElement(browserElem);
				}
			}
		}
		return pipesElem;
	}

	private XmlBuilder toAdapterMessagesXmlSelected(Adapter adapter,
			ShowConfigurationStatusManager showConfigurationStatusManager) {
		XmlBuilder adapterMessages = new XmlBuilder("adapterMessages");
		for (int t = 0; t < adapter.getMessageKeeper().size(); t++) {
			XmlBuilder adapterMessage = new XmlBuilder("adapterMessage");
			String msg = XmlUtils.replaceNonValidXmlCharacters(adapter.getMessageKeeper().getMessage(t).getMessageText());
			if (MAX_MESSAGE_SIZE > 0 && msg.length() > MAX_MESSAGE_SIZE) {
				msg = msg.substring(0, MAX_MESSAGE_SIZE) + "...(" + (msg.length() - MAX_MESSAGE_SIZE)
						+ " characters more)";
			}
			adapterMessage.setValue(msg, true);
			adapterMessage.addAttribute("date", DateUtils
					.format(adapter.getMessageKeeper().getMessage(t).getMessageDate(), DateUtils.FORMAT_FULL_GENERIC));
			String level = adapter.getMessageKeeper().getMessage(t).getMessageLevel();
			adapterMessage.addAttribute("level", level);
			adapterMessages.addSubElement(adapterMessage);
			if (level.equals(MessageKeeperLevel.ERROR.name())) {
				showConfigurationStatusManager.countMessagesError++;
			} else {
				if (level.equals(MessageKeeperLevel.WARN.name())) {
					showConfigurationStatusManager.countMessagesWarn++;
				} else {
					showConfigurationStatusManager.countMessagesInfo++;
				}
			}
		}
		return adapterMessages;
	}

	private XmlBuilder toAdapterMessagesXmlAll(Adapter adapter,
			ShowConfigurationStatusManager showConfigurationStatusManager,
			ShowConfigurationStatusAdapterManager showConfigurationStatusAdapterManager) {
		XmlBuilder adapterMessages = new XmlBuilder("adapterMessages");
		int cme = 0;
		int cmw = 0;
		int cmi = 0;
		for (int t = 0; t < adapter.getMessageKeeper().size(); t++) {
			String level = adapter.getMessageKeeper().getMessage(t).getMessageLevel();
			if (level.equals(MessageKeeperLevel.ERROR.name())) {
				cme++;
			} else {
				if (level.equals(MessageKeeperLevel.WARN.name())) {
					cmw++;
				} else {
					cmi++;
				}
			}
		}
		adapterMessages.addAttribute("error", cme + "");
		adapterMessages.addAttribute("warn", cmw + "");
		adapterMessages.addAttribute("info", cmi + "");
		showConfigurationStatusManager.countMessagesError += cme;
		showConfigurationStatusManager.countMessagesWarn += cmw;
		showConfigurationStatusManager.countMessagesInfo += cmi;
		if (!adapter.getMessageKeeper().isEmpty()) {
			String lastMessageLevel = adapter.getMessageKeeper().getMessage(adapter.getMessageKeeper().size() - 1)
					.getMessageLevel();
			adapterMessages.addAttribute("lastMessageLevel", lastMessageLevel.toLowerCase());
			if (lastMessageLevel.equals(MessageKeeperLevel.ERROR.name())
					|| lastMessageLevel.equals(MessageKeeperLevel.WARN.name())) {
				showConfigurationStatusAdapterManager.logAlert = true;
			}
		}

		return adapterMessages;
	}

	private XmlBuilder toSummaryXml(ShowConfigurationStatusManager showConfigurationStatusManager) {
		XmlBuilder summaryXML = new XmlBuilder("summary");
		XmlBuilder adapterStateXML = new XmlBuilder("adapterState");
		adapterStateXML.addAttribute("starting", showConfigurationStatusManager.countAdapterStateStarting + "");
		adapterStateXML.addAttribute("started", showConfigurationStatusManager.countAdapterStateStarted + "");
		adapterStateXML.addAttribute("startedNotAvailable", "0");
		adapterStateXML.addAttribute("stopping", showConfigurationStatusManager.countAdapterStateStopping + "");
		adapterStateXML.addAttribute("stopped", showConfigurationStatusManager.countAdapterStateStopped + "");
		adapterStateXML.addAttribute("error", showConfigurationStatusManager.countAdapterStateError + "");
		summaryXML.addSubElement(adapterStateXML);
		XmlBuilder lastMsgProcessStateXML = new XmlBuilder("lastMsgProcessState");
		lastMsgProcessStateXML.addAttribute("ok", showConfigurationStatusManager.countLastMsgProcessOk + "");
		lastMsgProcessStateXML.addAttribute("notApplicable", showConfigurationStatusManager.countLastMsgProcessNotApplicable + "");
		lastMsgProcessStateXML.addAttribute("error", showConfigurationStatusManager.countLastMsgProcessError + "");
		summaryXML.addSubElement(lastMsgProcessStateXML);
		XmlBuilder receiverStateXML = new XmlBuilder("receiverState");
		receiverStateXML.addAttribute("starting", showConfigurationStatusManager.countReceiverStateStarting + "");
		receiverStateXML.addAttribute("started", showConfigurationStatusManager.countReceiverStateStarted + "");
		receiverStateXML.addAttribute("startedNotAvailable", showConfigurationStatusManager.countReceiverStateStartedNotAvailable + "");
		receiverStateXML.addAttribute("stopping", showConfigurationStatusManager.countReceiverStateStopping + "");
		receiverStateXML.addAttribute("stopped", showConfigurationStatusManager.countReceiverStateStopped + "");
		receiverStateXML.addAttribute("error", showConfigurationStatusManager.countReceiverStateError + "");
		summaryXML.addSubElement(receiverStateXML);
		XmlBuilder messageLevelXML = new XmlBuilder("messageLevel");
		messageLevelXML.addAttribute("error", showConfigurationStatusManager.countMessagesError + "");
		messageLevelXML.addAttribute("warn", showConfigurationStatusManager.countMessagesWarn + "");
		messageLevelXML.addAttribute("info", showConfigurationStatusManager.countMessagesInfo + "");
		summaryXML.addSubElement(messageLevelXML);
		return summaryXML;
	}
}