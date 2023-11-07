/*
   Copyright 2022-2023 WeAreFrank!

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
package nl.nn.adapterframework.management.bus.endpoints;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowser.SortOrder;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.management.bus.ActionSelector;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.JsonResponseMessage;
import nl.nn.adapterframework.management.bus.StringResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.management.bus.dto.ProcessStateDTO;
import nl.nn.adapterframework.management.bus.dto.StorageItemDTO;
import nl.nn.adapterframework.management.bus.dto.StorageItemsDTO;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.util.MessageBrowsingFilter;
import nl.nn.adapterframework.util.MessageBrowsingUtil;
import nl.nn.adapterframework.util.Misc;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.MESSAGE_BROWSER)
public class BrowseMessageBrowsers extends BusEndpointBase {
	public static final String HEADER_MESSAGEID_KEY = "messageId";
	public static final String HEADER_RECEIVER_NAME_KEY = "receiver";
	public static final String HEADER_PIPE_NAME_KEY = "pipe";
	public static final String HEADER_PROCESSSTATE_KEY = "processState";

	private static final TransactionDefinition TXNEW_DEFINITION = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

	@ActionSelector(BusAction.GET)
	public Message<String> getMessageById(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, IbisManager.ALL_CONFIGS_KEY);
		String adapterName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_ADAPTER_NAME_KEY);
		Adapter adapter = getAdapterByName(configurationName, adapterName);
		String messageId = BusMessageUtils.getHeader(message, HEADER_MESSAGEID_KEY);

		String pipeName = BusMessageUtils.getHeader(message, HEADER_PIPE_NAME_KEY);
		String receiverName = BusMessageUtils.getHeader(message, HEADER_RECEIVER_NAME_KEY);

		IMessageBrowser<?> storage = null;
		StorageItemDTO storageItem = null;
		if(StringUtils.isNotEmpty(pipeName)) {
			storage = getStorageFromPipe(adapter, pipeName);
			storageItem = getMessageWithMetadata(storage, null, messageId);
		} else if(StringUtils.isNotEmpty(receiverName)) {
			ProcessState processState = BusMessageUtils.getEnumHeader(message, HEADER_PROCESSSTATE_KEY, ProcessState.class);
			Receiver<?> receiver = getReceiverByName(adapter, receiverName);

			storage = receiver.getMessageBrowser(processState);
			storageItem = getMessageWithMetadata(storage, receiver.getListener(), messageId);
		} else {
			throw new BusException("no StorageSource provided");
		}

		return new JsonResponseMessage(storageItem);
	}

	@ActionSelector(BusAction.DOWNLOAD)
	public StringResponseMessage downloadMessageById(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, IbisManager.ALL_CONFIGS_KEY);
		String adapterName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_ADAPTER_NAME_KEY);
		Adapter adapter = getAdapterByName(configurationName, adapterName);
		String messageId = BusMessageUtils.getHeader(message, HEADER_MESSAGEID_KEY);

		String pipeName = BusMessageUtils.getHeader(message, HEADER_PIPE_NAME_KEY);
		String receiverName = BusMessageUtils.getHeader(message, HEADER_RECEIVER_NAME_KEY);

		String storageItem = null;
		if(StringUtils.isNotEmpty(pipeName)) {
			storageItem = getMessage(getStorageFromPipe(adapter, pipeName), messageId);
		} else if(StringUtils.isNotEmpty(receiverName)) {
			ProcessState processState = BusMessageUtils.getEnumHeader(message, HEADER_PROCESSSTATE_KEY, ProcessState.class);
			Receiver<?> receiver = getReceiverByName(adapter, receiverName);

			IMessageBrowser<?> storage = receiver.getMessageBrowser(processState);
			storageItem = getMessage(storage, receiver.getListener(), messageId);
		} else {
			throw new BusException("no StorageSource provided");
		}

		MediaType mediaType = getMediaType(storageItem);
		String filename = getFilename(mediaType, messageId);

		StringResponseMessage response = new StringResponseMessage(storageItem, mediaType);
		response.setFilename(filename);
		return response;
	}

	@ActionSelector(BusAction.FIND)
	public Message<String> browseMessages(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, IbisManager.ALL_CONFIGS_KEY);
		String adapterName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_ADAPTER_NAME_KEY);
		Adapter adapter = getAdapterByName(configurationName, adapterName);

		String pipeName = BusMessageUtils.getHeader(message, HEADER_PIPE_NAME_KEY);
		String receiverName = BusMessageUtils.getHeader(message, HEADER_RECEIVER_NAME_KEY);

		IListener<?> listener = null;
		IMessageBrowser<?> storage = null;
		Map<ProcessState, ProcessStateDTO> targetPSInfo = null;
		if(StringUtils.isNotEmpty(pipeName)) {
			storage = getStorageFromPipe(adapter, pipeName);
		} else if(StringUtils.isNotEmpty(receiverName)) {
			ProcessState processState = BusMessageUtils.getEnumHeader(message, HEADER_PROCESSSTATE_KEY, ProcessState.class);
			Receiver<?> receiver = getReceiverByName(adapter, receiverName);
			listener = receiver.getListener();

			storage = receiver.getMessageBrowser(processState);
			targetPSInfo = getTargetProcessStateInfo(receiver.targetProcessStates().get(processState));
		} else {
			throw new BusException("no StorageSource provided");
		}

		String type = BusMessageUtils.getHeader(message, "type");
		String host = BusMessageUtils.getHeader(message, "host");
		String id = BusMessageUtils.getHeader(message, "idMask");
		String messageId = BusMessageUtils.getHeader(message, HEADER_MESSAGEID_KEY);
		String correlationId = BusMessageUtils.getHeader(message, "correlationId");
		String comment = BusMessageUtils.getHeader(message, "comment");
		String messageMask = BusMessageUtils.getHeader(message, "message");
		String label = BusMessageUtils.getHeader(message, "label");
		String startDate = BusMessageUtils.getHeader(message, "startDate");
		String endDate = BusMessageUtils.getHeader(message, "endDate");
		SortOrder sortOrder = BusMessageUtils.getEnumHeader(message, "sort", SortOrder.class, SortOrder.NONE);
		int skipMessages = BusMessageUtils.getIntHeader(message, "skip", 0);
		int maxMessages = BusMessageUtils.getIntHeader(message, "max", 100);

		//Apply filters
		MessageBrowsingFilter filter = new MessageBrowsingFilter(maxMessages, skipMessages);
		filter.setTypeMask(type);
		filter.setHostMask(host);
		filter.setIdMask(id);
		filter.setMessageIdMask(messageId);
		filter.setCorrelationIdMask(correlationId);
		filter.setCommentMask(comment);
		filter.setMessageMask(messageMask, storage, listener);
		filter.setLabelMask(label);
		filter.setStartDateMask(startDate);
		filter.setEndDateMask(endDate);
		filter.setSortOrder(sortOrder);

		StorageItemsDTO dto = new StorageItemsDTO(storage, filter);
		if(targetPSInfo != null && !targetPSInfo.isEmpty()) {
			dto.setTargetStates(targetPSInfo);
		}

		return new JsonResponseMessage(dto);
	}

	@ActionSelector(BusAction.STATUS)
	public void resend(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, IbisManager.ALL_CONFIGS_KEY);
		String adapterName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_ADAPTER_NAME_KEY);
		String receiverName = BusMessageUtils.getHeader(message, HEADER_RECEIVER_NAME_KEY);
		String messageId = BusMessageUtils.getHeader(message, HEADER_MESSAGEID_KEY);

		Adapter adapter = getAdapterByName(configurationName, adapterName);
		Receiver<?> receiver = getReceiverByName(adapter, receiverName);

		try {
			receiver.retryMessage(messageId);
		} catch (ListenerException e) {
			throw new BusException("unable to retry message with id ["+messageId+"]", e);
		}
	}

	/**
	 * Keep the transaction local to this method. Roll back when any error occurs.
	 * The Manager should only be retrieved when this method is being called. Else it may prevent the application from starting up.
	 */
	@ActionSelector(BusAction.DELETE)
	public void delete(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, IbisManager.ALL_CONFIGS_KEY);
		String adapterName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_ADAPTER_NAME_KEY);
		String receiverName = BusMessageUtils.getHeader(message, HEADER_RECEIVER_NAME_KEY);
		String messageId = BusMessageUtils.getHeader(message, HEADER_MESSAGEID_KEY);

		Adapter adapter = getAdapterByName(configurationName, adapterName);
		Receiver<?> receiver = getReceiverByName(adapter, receiverName);
		IMessageBrowser<?> browser = receiver.getMessageBrowser(ProcessState.ERROR);

		PlatformTransactionManager transactionManager = getApplicationContext().getBean("txManager", PlatformTransactionManager.class);
		TransactionStatus txStatus = transactionManager.getTransaction(TXNEW_DEFINITION);
		try {
			browser.deleteMessage(messageId);
		} catch (Exception e) {
			txStatus.setRollbackOnly();
			throw new BusException("unable to delete message with id ["+messageId+"]", e);
		} finally {
			transactionManager.commit(txStatus);
		}
	}

	@ActionSelector(BusAction.MANAGE)
	public void changeProcessState(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, IbisManager.ALL_CONFIGS_KEY);
		String adapterName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_ADAPTER_NAME_KEY);
		String receiverName = BusMessageUtils.getHeader(message, HEADER_RECEIVER_NAME_KEY);
		String messageId = BusMessageUtils.getHeader(message, HEADER_MESSAGEID_KEY);
		ProcessState processState = BusMessageUtils.getEnumHeader(message, HEADER_PROCESSSTATE_KEY, ProcessState.class);
		ProcessState targetState = BusMessageUtils.getEnumHeader(message, "targetState", ProcessState.class);

		Adapter adapter = getAdapterByName(configurationName, adapterName);
		Receiver<?> receiver = getReceiverByName(adapter, receiverName);

		Set<ProcessState> availableTargetStates = receiver.targetProcessStates().get(processState);
		if(availableTargetStates != null && availableTargetStates.contains(targetState)) {
			IMessageBrowser<?> store = receiver.getMessageBrowser(processState);
			try {
				RawMessageWrapper rawMessage = store.browseMessage(messageId);
				if (receiver.changeProcessState(rawMessage, targetState, "admin requested move")==null) { //Why do I need to provide a reason? //Why do I need to provide the raw message?
					throw new BusException("could not move message ["+messageId+"]");
				}
			} catch (ListenerException e) {
				throw new BusException("could not move message ["+messageId+"]", e);
			}
		} else {
			throw new BusException("it is not allowed to move messages from ["+processState+"] to ["+targetState+"]");
		}
	}

	private IMessageBrowser<?> getStorageFromPipe(Adapter adapter, String pipeName) {
		IPipe pipe = getPipeByName(adapter, pipeName);
		if(pipe instanceof MessageSendingPipe) {
			return getPipeMessageLog((MessageSendingPipe) pipe);
		}
		throw new BusException("pipe does not have a MessageBrowser");
	}

	private IMessageBrowser<?> getPipeMessageLog(MessageSendingPipe pipe) {
		IMessageBrowser<?> storage = pipe.getMessageLog();
		if(storage == null) {
			ISender sender = pipe.getSender();
			if(sender instanceof IMessageBrowser<?>) {
				storage = (IMessageBrowser<?>) sender;
			}
		}
		if(storage == null) {
			throw new BusException("Unable to fetch the message log for pipe ["+pipe.getName()+"]");
		}
		return storage;
	}

	private StorageItemDTO getMessageWithMetadata(IMessageBrowser<?> storage, IListener<?> listener, String messageId) {
		String message = getMessage(storage, listener, messageId);
		try(IMessageBrowsingIteratorItem item = storage.getContext(messageId)) {
			StorageItemDTO dto = new StorageItemDTO(item);
			dto.setMessage(message);
			return dto;
		} catch(ListenerException e) {
			throw new BusException("unable to read message context ["+messageId+"]", e);
		}
	}

	private String getMessage(IMessageBrowser<?> messageBrowser, String messageId) {
		return getMessage(messageBrowser, null, messageId);
	}
	private String getMessage(IMessageBrowser<?> messageBrowser, IListener<?> listener, String messageId) {
		if(messageBrowser == null) {
			throw new BusException("no MessageBrowser found");
		}

		String msg;
		try {
			RawMessageWrapper<?> rawmsg = messageBrowser.browseMessage(messageId);
			msg = MessageBrowsingUtil.getMessageText(rawmsg, listener);
		} catch(ListenerException | IOException e) {
			throw new BusException("unable to find or read message ["+messageId+"]", e);
		}

		if (StringUtils.isEmpty(msg)) {
			msg = "<no message found/>";
		} else {
			msg = Misc.cleanseMessage(msg, messageBrowser.getHideRegex(), messageBrowser.getHideMethod());
		}

		return msg;
	}

	private MediaType getMediaType(String msg) {
		MediaType type = MediaType.TEXT_PLAIN;
		if (StringUtils.isEmpty(msg)) {
			throw new BusException("message not found");
		}
		if(msg.startsWith("<")) {
			type = MediaType.APPLICATION_XML;
		} else if(msg.startsWith("{") || msg.startsWith("[")) {
			type = MediaType.APPLICATION_JSON;
		}
		return type;
	}

	private String getFilename(MediaType type, String filename) {
		String extension="txt";
		if(type == MediaType.APPLICATION_XML) {
			extension = "xml";
		} else if(type == MediaType.APPLICATION_JSON) {
			extension = "json";
		}

		return "msg-"+filename+"."+extension;

	}

	@Nonnull
	private Map<ProcessState, ProcessStateDTO> getTargetProcessStateInfo(Set<ProcessState> targetProcessStates) {
		if(targetProcessStates == null) {
			return Collections.emptyMap();
		}

		Map<ProcessState, ProcessStateDTO> result = new EnumMap<>(ProcessState.class);
		for (ProcessState ps : targetProcessStates) {
			result.put(ps, new ProcessStateDTO(ps));
		}

		return result;
	}
}
