/*
   Copyright 2022-2024 WeAreFrank!

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
package org.frankframework.management.bus.endpoints;

import java.io.InputStream;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.Configuration;
import org.frankframework.core.Adapter;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.HasSender;
import org.frankframework.core.IListener;
import org.frankframework.core.IMessageBrowser;
import org.frankframework.core.IPipe;
import org.frankframework.core.ISender;
import org.frankframework.core.ITransactionalStorage;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLine;
import org.frankframework.core.ProcessState;
import org.frankframework.encryption.HasKeystore;
import org.frankframework.encryption.KeystoreType;
import org.frankframework.http.RestListener;
import org.frankframework.jdbc.AbstractJdbcSender;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.dto.ProcessStateDTO;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.pipes.AsyncSenderWithListenerPipe;
import org.frankframework.pipes.MessageSendingPipe;
import org.frankframework.receivers.Receiver;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.MessageKeeperMessage;
import org.frankframework.util.RunState;
import org.springframework.messaging.Message;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.security.RolesAllowed;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.ADAPTER)
public class AdapterStatus extends BusEndpointBase {
	private boolean showCountMessageLog = AppConstants.getInstance().getBoolean("messageLog.count.show", true);

	private static final String RECEIVERS="receivers";
	private static final String PIPES="pipes";
	private static final String MESSAGES="messages";

	public enum Expanded {
		NONE, ALL, RECEIVERS, MESSAGES, PIPES
	}

	@ActionSelector(BusAction.GET)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> getAdapters(Message<?> message) {
		Expanded expanded = BusMessageUtils.getEnumHeader(message, "expanded", Expanded.class, Expanded.NONE);
		boolean showPendingMsgCount = BusMessageUtils.getBooleanHeader(message, "showPendingMsgCount", false);

		TreeMap<String, Object> adapterList = new TreeMap<>();
		for(Configuration config : getIbisManager().getConfigurations()) {
			for(Adapter adapter: config.getRegisteredAdapters()) {
				Map<String, Object> adapterInfo = getAdapterInformation(adapter, expanded, showPendingMsgCount);
				String uniqueKey = String.format("%s/%s", adapter.getConfiguration().getName(), adapter.getName());
				adapterList.put(uniqueKey, adapterInfo);
			}
		}

		return new JsonMessage(adapterList);
	}

	@ActionSelector(BusAction.FIND)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> getAdapter(Message<?> message) {
		Expanded expanded = BusMessageUtils.getEnumHeader(message, "expanded", Expanded.class, Expanded.NONE);
		boolean showPendingMsgCount = BusMessageUtils.getBooleanHeader(message, "showPendingMsgCount", false);
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		String adapterName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_ADAPTER_NAME_KEY);

		Adapter adapter = getAdapterByName(configurationName, adapterName);
		Map<String, Object> adapterInfo = getAdapterInformation(adapter, expanded, showPendingMsgCount);
		return new JsonMessage(adapterInfo);
	}

	private Map<String, Object> getAdapterInformation(Adapter adapter, Expanded expandedFilter, boolean showPendingMsgCount) {
		Map<String, Object> adapterInfo = mapAdapter(adapter);
		switch (expandedFilter) {
		case ALL:
			adapterInfo.put(RECEIVERS, mapAdapterReceivers(adapter, showPendingMsgCount));
			adapterInfo.put(PIPES, mapAdapterPipes(adapter));
			adapterInfo.put(MESSAGES, mapAdapterMessages(adapter));
			break;
		case RECEIVERS:
			adapterInfo.put(RECEIVERS, mapAdapterReceivers(adapter, showPendingMsgCount));
			break;
		case PIPES:
			adapterInfo.put(PIPES, mapAdapterPipes(adapter));
			break;
		case MESSAGES:
			adapterInfo.put(MESSAGES, mapAdapterMessages(adapter));
		break;

		case NONE:
		default:
			//Don't add additional info
		}
		return adapterInfo;
	}

	@Nullable
	private Map<String, Object> addCertificateInfo(@Nonnull HasKeystore s) {
		String certificate = s.getKeystore();
		if (certificate == null || StringUtils.isEmpty(certificate))
			return null;

		Map<String, Object> certElem = new HashMap<>(4);
		certElem.put("name", certificate);
		String certificateAuthAlias = s.getKeystoreAuthAlias();
		certElem.put("authAlias", certificateAuthAlias);
		URL certificateUrl = ClassLoaderUtils.getResourceURL(s, s.getKeystore());
		if (certificateUrl == null) {
			certElem.put("url", "");
			certElem.put("info", "*** ERROR ***");
		} else {
			certElem.put("url", certificateUrl.toString());
			String certificatePassword = s.getKeystorePassword();
			CredentialFactory certificateCf = new CredentialFactory(certificateAuthAlias, null, certificatePassword);
			KeystoreType keystoreType = s.getKeystoreType();
			certElem.put("info", getCertificateInfo(certificateUrl, certificateCf.getPassword(), keystoreType, "Certificate chain"));
		}
		return certElem;
	}

	private ArrayList<Object> getCertificateInfo(final URL url, final String password, KeystoreType keystoreType, String prefix) {
		ArrayList<Object> certificateList = new ArrayList<>();
		try (InputStream stream = url.openStream()) {
			KeyStore keystore = KeyStore.getInstance(keystoreType.name());
			keystore.load(stream, password != null ? password.toCharArray() : null);
			if (log.isInfoEnabled()) {
				Enumeration<String> aliases = keystore.aliases();
				while (aliases.hasMoreElements()) {
					String alias =  aliases.nextElement();
					ArrayList<Object> infoElem = new ArrayList<>();
					infoElem.add(prefix + " '" + alias + "':");
					Certificate trustedcert = keystore.getCertificate(alias);
					if (trustedcert instanceof X509Certificate cert) {
						infoElem.add("Subject DN: " + cert.getSubjectDN());
						infoElem.add("Signature Algorithm: " + cert.getSigAlgName());
						infoElem.add("Valid from: " + cert.getNotBefore());
						infoElem.add("Valid until: " + cert.getNotAfter());
						infoElem.add("Issuer: " + cert.getIssuerDN());
					}
					certificateList.add(infoElem);
				}
			}
		} catch (Exception e) {
			certificateList.add("*** ERROR ***");
		}
		return certificateList;
	}

	@Nullable
	private ArrayList<Object> mapAdapterPipes(@Nonnull Adapter adapter) {
		if(!adapter.isConfigured())
			return null;
		PipeLine pipeline = adapter.getPipeLine();
		int totalPipes = pipeline.getPipes().size();
		ArrayList<Object> pipes = new ArrayList<>(totalPipes);

		for (int i=0; i<totalPipes; i++) {
			Map<String, Object> pipesInfo = new HashMap<>();
			IPipe pipe = pipeline.getPipe(i);
			Map<String, PipeForward> pipeForwards = pipe.getForwards();

			String pipeName = pipe.getName();

			Map<String, String> forwards = new HashMap<>();
			for (PipeForward fwrd : pipeForwards.values()) {
				forwards.put(fwrd.getName(), fwrd.getPath());
			}

			pipesInfo.put("name", pipeName);
			pipesInfo.put("forwards", forwards);
			if (pipe instanceof HasKeystore s) {
				Map<String, Object> certInfo = addCertificateInfo(s);
				if(certInfo != null)
					pipesInfo.put("certificate", certInfo);
			}
			if (pipe instanceof MessageSendingPipe msp) {
				ISender sender = msp.getSender();
				pipesInfo.put("sender", ClassUtils.nameOf(sender));
				if (sender instanceof HasKeystore s) {
					Map<String, Object> certInfo = addCertificateInfo(s);
					if(certInfo != null)
						pipesInfo.put("certificate", certInfo);
				}
				if (sender instanceof HasPhysicalDestination destination) {
					pipesInfo.put("destination",destination.getPhysicalDestinationName());
				}
				if (sender instanceof AbstractJdbcSender) {
					pipesInfo.put("isJdbcSender", true);
				}
				if (pipe instanceof AsyncSenderWithListenerPipe slp) {
					IListener<?> listener = slp.getListener();
					if (listener!=null) {
						pipesInfo.put("listenerName", listener.getName());
						pipesInfo.put("listenerClass", ClassUtils.nameOf(listener));
						if (listener instanceof HasPhysicalDestination destination) {
							String pd = destination.getPhysicalDestinationName();
							pipesInfo.put("listenerDestination", pd);
						}
					}
				}
				ITransactionalStorage<?> messageLog = msp.getMessageLog();
				if (messageLog!=null) {
					mapPipeMessageLog(messageLog, pipesInfo, adapter.getRunState() == RunState.STARTED);
				} else if(sender instanceof ITransactionalStorage store) {
					mapPipeMessageLog(store, pipesInfo, adapter.getRunState() == RunState.STARTED);
					pipesInfo.put("isSenderTransactionalStorage", true);
				}
			}
			pipes.add(pipesInfo);
		}
		return pipes;
	}

	private void mapPipeMessageLog(ITransactionalStorage<?> store, Map<String, Object> data, boolean isStarted) {
		data.put("hasMessageLog", true);
		String messageLogCount;
		try {
			if (showCountMessageLog && isStarted) {
				messageLogCount=""+store.getMessageCount();
			} else {
				messageLogCount="?";
			}
		} catch (Exception e) {
			log.warn("Cannot determine number of messages in messageLog [{}]", store.getName(), e);
			messageLogCount="error";
		}
		data.put("messageLogCount", messageLogCount);

		Map<String, Object> message = new HashMap<>();
		message.put("name", store.getName());
		message.put("type", "log");
		message.put("slotId", store.getSlotId());
		message.put("count", messageLogCount);
		data.put("message", message);
	}

	private Object getMessageCount(RunState runState, IMessageBrowser<?> ts) {
		if(runState == RunState.STARTED) {
			try {
				return ts.getMessageCount();
			} catch (Exception e) {
				log.warn("Cannot determine number of messages in MessageBrowser [{}]", ClassUtils.nameOf(ts), e);
				return "error";
			}
		} else {
			return "?";
		}
	}

	private ArrayList<Object> mapAdapterReceivers(Adapter adapter, boolean showPendingMsgCount) {
		ArrayList<Object> receivers = new ArrayList<>();

		for (Receiver<?> receiver: adapter.getReceivers()) {
			Map<String, Object> receiverInfo = new HashMap<>();

			RunState receiverRunState = receiver.getRunState();

			receiverInfo.put("name", receiver.getName());
			receiverInfo.put("state", receiverRunState.name().toLowerCase());

			Map<String, Object> messages = new HashMap<>(3);
			messages.put("received", receiver.getMessagesReceived());
			messages.put("retried", receiver.getMessagesRetried());
			messages.put("rejected", receiver.getMessagesRejected());
			receiverInfo.put(MESSAGES, messages);

			Set<ProcessState> knownStates = receiver.knownProcessStates();
			Map<ProcessState, Object> tsInfo = new LinkedHashMap<>();
			for (ProcessState state : knownStates) {
				IMessageBrowser<?> ts = receiver.getMessageBrowser(state);
				if(ts != null) {
					ProcessStateDTO psDto = new ProcessStateDTO(state);
					psDto.setMessageCount(getMessageCount(receiverRunState, ts));
					tsInfo.put(state, psDto);
				}
			}
			receiverInfo.put("transactionalStores", tsInfo);

			ISender sender=null;
			IListener<?> listener=receiver.getListener();
			if(listener != null) {
				Map<String, Object> listenerInfo = new HashMap<>();
				listenerInfo.put("name", listener.getName());
				listenerInfo.put("class", ClassUtils.nameOf(listener));
				if (listener instanceof HasPhysicalDestination && receiver.isConfigured()) {
					String pd = ((HasPhysicalDestination)receiver.getListener()).getPhysicalDestinationName();
					listenerInfo.put("destination", pd);
				}
				if (listener instanceof HasSender hasSender) {
					sender = hasSender.getSender();
				}

				boolean isRestListener = listener instanceof RestListener;
				listenerInfo.put("isRestListener", isRestListener);
				if (isRestListener) {
					RestListener rl = (RestListener) listener;
					listenerInfo.put("restUriPattern", rl.getRestUriPattern());
				}

				receiverInfo.put("listener", listenerInfo);
			}

			ISender rsender = receiver.getSender();
			if (rsender!=null) { // this sender has preference, but avoid overwriting listeners sender with null
				sender=rsender;
			}
			if (sender != null) {
				receiverInfo.put("senderName", sender.getName());
				receiverInfo.put("senderClass", ClassUtils.nameOf(sender));
				if (sender instanceof HasPhysicalDestination destination && receiver.isConfigured()) {
					String pd = destination.getPhysicalDestinationName();
					receiverInfo.put("senderDestination", pd);
				}
			}
			if (receiver.isThreadCountReadable() && receiver.isConfigured()) {
				receiverInfo.put("threadCount", receiver.getCurrentThreadCount());
				receiverInfo.put("maxThreadCount", receiver.getMaxThreadCount());

				if (receiver.isThreadCountControllable()) {
					receiverInfo.put("threadCountControllable", true);
				}
			}
			receivers.add(receiverInfo);
		}
		return receivers;
	}

	private ArrayList<Object> mapAdapterMessages(Adapter adapter) {
		int totalMessages = adapter.getMessageKeeper().size();
		ArrayList<Object> messages = new ArrayList<>(totalMessages);
		for (int t=0; t<totalMessages; t++) {
			Map<String, Object> message = new HashMap<>();
			MessageKeeperMessage msg = adapter.getMessageKeeper().getMessage(t);

			message.put("message", msg.getMessageText());
			message.put("date", Date.from(msg.getMessageDate()));
			message.put("level", msg.getMessageLevel());
			message.put("capacity", adapter.getMessageKeeper().capacity());

			messages.add(message);
		}
		return messages;
	}

	private Map<String, Object> mapAdapter(Adapter adapter) {
		Map<String, Object> adapterInfo = new HashMap<>();
		Configuration config = adapter.getConfiguration();

		adapterInfo.put("name", adapter.getName());
		adapterInfo.put("description", adapter.getDescription());
		adapterInfo.put("configuration", config.getName() );
		RunState adapterRunState = adapter.getRunState();
		adapterInfo.put("started", adapterRunState==RunState.STARTED);
		String state = adapterRunState.toString().toLowerCase().replace("*", "");
		adapterInfo.put("state", state);

		adapterInfo.put("configured", adapter.isConfigured());
		adapterInfo.put("upSince", adapter.getStatsUpSinceDate().getTime());
		Date lastMessage = adapter.getLastMessageDateDate();
		if(lastMessage != null) {
			adapterInfo.put("lastMessage", lastMessage.getTime());
			adapterInfo.put("messagesInProcess", adapter.getNumOfMessagesInProcess());
			adapterInfo.put("messagesProcessed", adapter.getNumOfMessagesProcessed());
			adapterInfo.put("messagesInError", adapter.getNumOfMessagesInError());
		}

		Iterator<Receiver<?>> it = adapter.getReceivers().iterator();
		int errorStoreMessageCount = 0;
		int messageLogMessageCount = 0;
		while(it.hasNext()) {
			Receiver<?> rcv = it.next();
			if(rcv.isNumberOfExceptionsCaughtWithoutMessageBeingReceivedThresholdReached()) {
				adapterInfo.put("receiverReachedMaxExceptions", "true");
			}

			if(rcv.getRunState() == RunState.STARTED) {
				IMessageBrowser<?> esmb = rcv.getMessageBrowser(ProcessState.ERROR);
				if(esmb != null) {
					try {
						errorStoreMessageCount += esmb.getMessageCount();
					} catch (ListenerException e) {
						// Only log the stacktrace when loglevel == INFO. Otherwise it will pollute the log too much.
						if(log.isInfoEnabled()) log.warn("Cannot determine number of messages in errorstore of [{}]", rcv.getName(), e);
						else log.warn("Cannot determine number of messages in errorstore of [{}]: {}", rcv::getName, e::getMessage);
					}
				}
				IMessageBrowser<?> mlmb = rcv.getMessageBrowser(ProcessState.DONE);
				if(mlmb != null) {
					try {
						messageLogMessageCount += mlmb.getMessageCount();
					} catch (ListenerException e) {
						// Only log the stacktrace when loglevel == INFO. Otherwise it will pollute the log too much.
						if(log.isInfoEnabled()) log.warn("Cannot determine number of messages in errorstore of [{}]", rcv.getName(), e);
						else log.warn("Cannot determine number of messages in errorstore of [{}]: {}", rcv::getName, e::getMessage);
					}
				}
			}
		}
		if(errorStoreMessageCount != 0) {
			adapterInfo.put("errorStoreMessageCount", errorStoreMessageCount);
		}
		if(messageLogMessageCount != 0) {
			adapterInfo.put("messageLogMessageCount", messageLogMessageCount);
		}

		return adapterInfo;
	}
}
