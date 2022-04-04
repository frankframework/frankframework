/*
Copyright 2018-2022 WeAreFrank!

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
package nl.nn.adapterframework.webcontrol.api;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowser.SortOrder;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.MessageBrowsingFilter;
import nl.nn.adapterframework.util.Misc;

@Path("/")
public class TransactionalStorage extends Base {

	protected static final TransactionDefinition TXNEW = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

	public enum StorageSource {
		RECEIVERS, PIPES;

		public static StorageSource fromString(String value) {
			return EnumUtils.parse(StorageSource.class, value);
		}
	}

	@GET
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/{storageSource}/{storageSourceName}/stores/{processState}/messages/{messageId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response browseMessage(
				@PathParam("adapterName") String adapterName,
				@PathParam("storageSource") StorageSource storageSource,
				@PathParam("storageSourceName") String storageSourceName,
				@PathParam("processState") String processState,
				@PathParam("messageId") String messageId
			) throws ApiException {

		Adapter adapter = getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		String message = null;
		IMessageBrowser<?> storage = null;
		// messageId is double URLEncoded, because it can contain '/' in ExchangeMailListener
		messageId = Misc.urlDecode(messageId);

		if(storageSource == StorageSource.PIPES) {
			MessageSendingPipe pipe = (MessageSendingPipe) adapter.getPipeLine().getPipe(storageSourceName);
			if(pipe == null) {
				throw new ApiException("Pipe ["+storageSourceName+"] not found!");
			}
			storage = pipe.getMessageLog();
			message = getMessage(storage, messageId);
		} else {
			Receiver<?> receiver = adapter.getReceiverByName(storageSourceName);
			if(receiver == null) {
				throw new ApiException("Receiver ["+storageSourceName+"] not found!");
			}
			storage = receiver.getMessageBrowser(ProcessState.getProcessStateFromName(processState));
			message = getMessage(storage, receiver.getListener(), messageId);
		}

		try {
			StorageItemDTO entity = getMessageMetadata(storage, messageId, message);
			return Response.status(Response.Status.OK).entity(entity).build();
		} catch(ListenerException e) {
			throw new ApiException("Could not get message metadata", e);
		}
	}

	private StorageItemDTO getMessageMetadata(IMessageBrowser<?> storage, String messageId, String message) throws ListenerException {
		try(IMessageBrowsingIteratorItem item = storage.getContext(messageId)) {
			StorageItemDTO dto = new StorageItemDTO(item);
			dto.setMessage(message);
			return dto;
		}
	}

	public static class StorageItemDTO {
		private @Getter String id; //MessageId
		private @Getter String displayedId; // just to display truncated data
		private @Getter String originalId; //Made up Id?
		private @Getter String correlationId;
		private @Getter String type;
		private @Getter String host;
		private @Getter Date insertDate;
		private @Getter Date expiryDate;
		private @Getter String comment;
		private @Getter String label;

		// Optional fields (with setters, should only be displayed when !NULL
		private @Getter(onMethod_={@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)}) @Setter Integer position;
		private @Getter(onMethod_={@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)}) @Setter String message;

		public StorageItemDTO(IMessageBrowsingIteratorItem item) throws ListenerException {
			id = item.getId();
			displayedId = item.getId(); // just to display truncated data
			originalId = item.getOriginalId();
			correlationId = item.getCorrelationId();
			type = item.getType();
			host = item.getHost();
			insertDate = item.getInsertDate();
			expiryDate = item.getExpiryDate();
			comment = item.getCommentString();
			label = item.getLabel();
		}
	}

	@GET
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/{storageSource}/{storageSourceName}/stores/{processState}/messages/{messageId}/download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadMessage(
			@PathParam("adapterName") String adapterName,
			@PathParam("storageSource") StorageSource storageSource,
			@PathParam("storageSourceName") String storageSourceName,
			@PathParam("processState") String processState,
			@PathParam("messageId") String messageId
		) throws ApiException {

		Adapter adapter = getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		// messageId is double URLEncoded, because it can contain '/' in ExchangeMailListener
		messageId = Misc.urlDecode(messageId);
		String message;
		if(storageSource == StorageSource.PIPES) {
			MessageSendingPipe pipe = (MessageSendingPipe) adapter.getPipeLine().getPipe(storageSourceName);
			if(pipe == null) {
				throw new ApiException("Pipe ["+storageSourceName+"] not found!");
			}

			message = getMessage(pipe.getMessageLog(), messageId);
		} else {
			Receiver<?> receiver = adapter.getReceiverByName(storageSourceName);
			if(receiver == null) {
				throw new ApiException("Receiver ["+storageSourceName+"] not found!");
			}

			IMessageBrowser<?> storage = receiver.getMessageBrowser(ProcessState.getProcessStateFromName(processState));
			message = getMessage(storage, receiver.getListener(), messageId);
		}

		MediaType mediaType = getMediaType(message);
		String contentDispositionHeader = getContentDispositionHeader(mediaType, messageId);

		return Response
				.status(Response.Status.OK)
				.type(mediaType)
				.entity(message)
				.header(HttpHeaders.CONTENT_DISPOSITION, contentDispositionHeader)
				.build();
	}

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/{storageSource}/{storageSourceName}/stores/{processState}/messages/download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadMessages(
			@PathParam("adapterName") String adapterName,
			@PathParam("storageSource") StorageSource storageSource,
			@PathParam("storageSourceName") String storageSourceName,
			@PathParam("processState") String processState,
			MultipartBody input
		) throws ApiException {

		Adapter adapter = getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		IMessageBrowser<?> storage;
		IListener<?> listener;
		if(storageSource == StorageSource.PIPES) {
			MessageSendingPipe pipe = (MessageSendingPipe) adapter.getPipeLine().getPipe(storageSourceName);
			if(pipe == null) {
				throw new ApiException("Pipe ["+storageSourceName+"] not found!");
			}

			storage = pipe.getMessageLog();
			listener=null;
		} else {
			Receiver<?> receiver = adapter.getReceiverByName(storageSourceName);
			if(receiver == null) {
				throw new ApiException("Receiver ["+storageSourceName+"] not found!");
			}

			storage = receiver.getMessageBrowser(ProcessState.getProcessStateFromName(processState));
			listener = receiver.getListener();
		}

		String[] messageIdArray = getMessageIds(input);

		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream out) throws IOException {
				try (ZipOutputStream zos = new ZipOutputStream(out)) {
					for (String messageId : messageIdArray) {
						// messageId is double URLEncoded, because it can contain '/' in ExchangeMailListener
						messageId = Misc.urlDecode(messageId);
						String message = getMessage(storage, listener, messageId);
						if(message != null) {
							String filenameExtension = ".txt";
							MediaType mediaType = getMediaType(message);
							if(mediaType.equals(MediaType.APPLICATION_JSON_TYPE)) {
								filenameExtension = ".json";
							} else if(mediaType.equals(MediaType.APPLICATION_XML_TYPE)) {
								filenameExtension = ".xml";
							}

							ZipEntry entry = new ZipEntry("msg-"+messageId+filenameExtension);
							zos.putNextEntry(entry);
							zos.write(message.getBytes());
							zos.closeEntry();
						}
					}
				} catch (IOException e) {
					throw new ApiException("Failed to create zip file with messages.", e);
				}
			}
		};

		return Response.ok(stream)
				.type(MediaType.APPLICATION_OCTET_STREAM)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"messages.zip\"")
				.build();
	}

	@GET
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/{storageSource}/{storageSourceName}/stores/{processState}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response browseMessages(
				@PathParam("adapterName") String adapterName,
				@PathParam("storageSource") StorageSource storageSource,
				@PathParam("storageSourceName") String storageSourceName,
				@PathParam("processState") String processState,
				@QueryParam("type") String type,
				@QueryParam("host") String host,
				@QueryParam("id") String id,
				@QueryParam("messageId") String messageId,
				@QueryParam("correlationId") String correlationId,
				@QueryParam("comment") String comment,
				@QueryParam("message") String message,
				@QueryParam("label") String label,
				@QueryParam("startDate") String startDateStr,
				@QueryParam("endDate") String endDateStr,
				@QueryParam("sort") String sort,
				@QueryParam("skip") int skipMessages,
				@QueryParam("max") int maxMessages
			) throws ApiException {

		Adapter adapter = getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		IMessageBrowser<?> storage = null;
		IListener<?> listener = null;
		Map<ProcessState, Map<String, String>> targetPSInfo = null;
		if(storageSource == StorageSource.PIPES) {
			MessageSendingPipe pipe = (MessageSendingPipe) adapter.getPipeLine().getPipe(storageSourceName);
			if(pipe == null) {
				throw new ApiException("Pipe ["+storageSourceName+"] not found!");
			}

			storage = pipe.getMessageLog();
		} else {
			Receiver<?> receiver = adapter.getReceiverByName(storageSourceName);
			if(receiver == null) {
				throw new ApiException("Receiver ["+storageSourceName+"] not found!");
			}
			
			ProcessState state = ProcessState.getProcessStateFromName(processState); 
			storage = receiver.getMessageBrowser(state);
			targetPSInfo = getTargetProcessStateInfo(receiver.targetProcessStates().get(state));
			listener = receiver.getListener();
		}

		if(storage == null) {
			throw new ApiException("no IMessageBrowser found");
		}

		//Apply filters
		MessageBrowsingFilter filter = new MessageBrowsingFilter(maxMessages, skipMessages);
		filter.setTypeMask(type);
		filter.setHostMask(host);
		filter.setIdMask(id);
		filter.setMessageIdMask(messageId);
		filter.setCorrelationIdMask(correlationId);
		filter.setCommentMask(comment);
		filter.setMessageMask(message, storage, listener);
		filter.setLabelMask(label);
		filter.setStartDateMask(startDateStr);
		filter.setEndDateMask(endDateStr);

		if("desc".equalsIgnoreCase(sort))
			filter.setSortOrder(SortOrder.DESC);
		if("asc".equalsIgnoreCase(sort))
			filter.setSortOrder(SortOrder.ASC);

		Map<String, Object> resultObj = getMessages(storage, filter);
		if(targetPSInfo != null && targetPSInfo.size()>0) {
			resultObj.put("targetStates", targetPSInfo);
		}

		return Response.status(Response.Status.OK).entity(resultObj).build();
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}/stores/Error/messages/{messageId}")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	public Response resendReceiverMessage(
			@PathParam("adapterName") String adapterName,
			@PathParam("receiverName") String receiverName,
			@PathParam("messageId") String messageId
		) throws ApiException {

		Adapter adapter = getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		Receiver<?> receiver = adapter.getReceiverByName(receiverName);
		if(receiver == null) {
			throw new ApiException("Receiver ["+receiverName+"] not found!");
		}

		// messageId is double URLEncoded, because it can contain '/' in ExchangeMailListener
		messageId = Misc.urlDecode(messageId);

		resendMessage(receiver, messageId);

		return Response.status(Response.Status.OK).build();
	}

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}/stores/Error")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response resendReceiverMessages(
			@PathParam("adapterName") String adapterName,
			@PathParam("receiverName") String receiverName,
			MultipartBody input
		) throws ApiException {

		Adapter adapter = getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		Receiver<?> receiver = adapter.getReceiverByName(receiverName);
		if(receiver == null) {
			throw new ApiException("Receiver ["+receiverName+"] not found!");
		}

		String[] messageIds = getMessageIds(input);

		List<String> errorMessages = new ArrayList<>();
		for(int i=0; i < messageIds.length; i++) {
			try {
				resendMessage(receiver, messageIds[i]);
			}
			catch(ApiException e) { //The message of an ApiException is wrapped in HTML, try to get the original message instead!
				errorMessages.add(e.getCause().getMessage());
			}
			catch(Exception e) {
				errorMessages.add(e.getMessage());
			}
		}

		if(errorMessages.isEmpty()) {
			return Response.status(Response.Status.OK).build();
		}

		return Response.status(Response.Status.ACCEPTED).entity(errorMessages).build();
	}

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}/stores/{processState}/move/{targetState}")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response changeProcessState(
			@PathParam("adapterName") String adapterName,
			@PathParam("receiverName") String receiverName,
			@PathParam("processState") String processState,
			@PathParam("targetState") String targetState,
			MultipartBody input
		) throws ApiException {

		Adapter adapter = getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		Receiver<?> receiver = adapter.getReceiverByName(receiverName);
		if(receiver == null) {
			throw new ApiException("Receiver ["+receiverName+"] not found!");
		}
		String[] messageIds = getMessageIds(input);

		ProcessState currentState = ProcessState.getProcessStateFromName(processState);
		Set<ProcessState> targetProcessStates = receiver.targetProcessStates().get(currentState);
		ProcessState targetPS = ProcessState.getProcessStateFromName(targetState);

		List<String> errorMessages = new ArrayList<>();
		if(targetProcessStates != null && targetProcessStates.contains(targetPS)) {
			IMessageBrowser<?> store = receiver.getMessageBrowser(currentState);
			for(int i=0; i < messageIds.length; i++) {
				try {
					if (receiver.changeProcessState(store.browseMessage(messageIds[i]), targetPS, "admin requested move")==null) {
						errorMessages.add("could not move message ["+messageIds[i]+"]");
					}
				} catch (ListenerException e) {
					errorMessages.add(e.getMessage());
				}
			}
		} else {
			throw new ApiException("It is not allowed to move messages from ["+processState+"] " + "to ["+targetState+"]");
		}

		if(errorMessages.isEmpty()) {
			return Response.status(Response.Status.OK).build();
		}

		return Response.status(Response.Status.ACCEPTED).entity(errorMessages).build();
	}

	@DELETE
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}/stores/Error/messages/{messageId}")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteReceiverMessage(
			@PathParam("adapterName") String adapterName,
			@PathParam("receiverName") String receiverName,
			@PathParam("messageId") String messageId
		) throws ApiException {

		Adapter adapter = getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		Receiver<?> receiver = adapter.getReceiverByName(receiverName);
		if(receiver == null) {
			throw new ApiException("Receiver ["+receiverName+"] not found!");
		}

		// messageId is double URLEncoded, because it can contain '/' in ExchangeMailListener
		messageId = Misc.urlDecode(messageId);

		deleteMessage(receiver.getMessageBrowser(ProcessState.ERROR), messageId);

		return Response.status(Response.Status.OK).build();
	}

	@DELETE
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}/stores/Error")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response deleteReceiverMessages(
			@PathParam("adapterName") String adapterName,
			@PathParam("receiverName") String receiverName,
			MultipartBody input
		) throws ApiException {

		Adapter adapter = getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		Receiver<?> receiver = adapter.getReceiverByName(receiverName);
		if(receiver == null) {
			throw new ApiException("Receiver ["+receiverName+"] not found!");
		}

		String[] messageIds = getMessageIds(input);

		List<String> errorMessages = new ArrayList<>();
		for(int i=0; i < messageIds.length; i++) {
			try {
				deleteMessage(receiver.getMessageBrowser(ProcessState.ERROR), messageIds[i]);
			}
			catch(ApiException e) { //The message of an ApiException is wrapped in HTML, try to get the original message instead!
				errorMessages.add(e.getCause().getMessage());
			}
			catch(Exception e) {
				errorMessages.add(e.getMessage());
			}
		}

		if(errorMessages.isEmpty()) {
			return Response.status(Response.Status.OK).build();
		}

		return Response.status(Response.Status.ACCEPTED).entity(errorMessages).build();
	}

	private String[] getMessageIds(MultipartBody inputDataMap) {
		String messageIds = resolveStringFromMap(inputDataMap, "messageIds");
		return messageIds.split(",");
	}

	private void deleteMessage(IMessageBrowser<?> storage, String messageId) {
		PlatformTransactionManager transactionManager = getIbisManager().getTransactionManager();
		TransactionStatus txStatus = null;
		try {
			txStatus = transactionManager.getTransaction(TXNEW);
			storage.deleteMessage(messageId);
		} catch (Exception e) {
			if (txStatus!=null) {
				txStatus.setRollbackOnly();
			}
			throw new ApiException(e);
		} finally {
			transactionManager.commit(txStatus);
		}
	}

	private void resendMessage(Receiver<?> receiver, String messageId) {
		try {
			receiver.retryMessage(messageId);
		} catch (ListenerException e) {
			throw new ApiException(e);
		}
	}

	private String getMessage(IMessageBrowser<?> messageBrowser, String messageId) {
		return getMessage(messageBrowser, null, messageId);
	}

	private String getMessage(IMessageBrowser<?> messageBrowser, IListener<?> listener, String messageId) {
		return getMessageText(messageBrowser, listener, messageId);
	}

	private String getMessageText(IMessageBrowser<?> messageBrowser, IListener listener, String messageId) {
		Object rawmsg = null;
		try {
			rawmsg = messageBrowser.browseMessage(messageId);
		}
		catch(ListenerException e) {
			throw new ApiException(e, 404);
		}

		String msg = null;
		if (rawmsg != null) {
			if(rawmsg instanceof MessageWrapper) {
				try {
					MessageWrapper<?> msgsgs = (MessageWrapper<?>) rawmsg;
					msg = msgsgs.getMessage().asString();
				} catch (IOException e) {
					throw new ApiException(e, 500);
				}
			} else if(rawmsg instanceof Message) { // For backwards compatibility: earlier MessageLog messages were stored as Message.
				try {
					msg = ((Message)rawmsg).asString();
				} catch (IOException e) {
					throw new ApiException(e, 500);
				}
			} else {
				try {
					if (listener!=null) {
						msg = listener.extractMessage(rawmsg, null).asString();
					}
				} catch (Exception e) {
					log.warn("Exception reading value raw message", e);
				}
				try {
					if (StringUtils.isEmpty(msg)) {
						msg = Message.asString(rawmsg);
					}
				} catch (Exception e) {
					log.warn("Cannot convert message", e);
					msg = rawmsg.toString();
				}
			}
		}
		if (StringUtils.isEmpty(msg)) {
			msg = "<no message found/>";
		} else {
			msg=Misc.cleanseMessage(msg, messageBrowser.getHideRegex(), messageBrowser.getHideMethod());
		}

		return msg;
	}

	private MediaType getMediaType(String msg) {
		MediaType type = MediaType.TEXT_PLAIN_TYPE;
		if (StringUtils.isEmpty(msg)) {
			throw new ApiException("message not found");
		} 
		if(msg.startsWith("<")) {
			type = MediaType.APPLICATION_XML_TYPE;
		} else if(msg.startsWith("{") || msg.startsWith("[")) {
			type = MediaType.APPLICATION_JSON_TYPE;
		}
		return type;
	}
	
	private String getContentDispositionHeader(MediaType type, String filename) {
		String extension="txt";
		if(MediaType.APPLICATION_XML_TYPE.equals(type)) {
			extension = "xml";
		} else if(MediaType.APPLICATION_JSON_TYPE.equals(type)) {
			extension = "json";
		}

		return "attachment; filename=\"msg-"+filename+"."+extension+"\"";

	}

	private Map<String, Object> getMessages(IMessageBrowser<?> transactionalStorage, MessageBrowsingFilter filter) {
		int messageCount = 0;
		try {
			messageCount = transactionalStorage.getMessageCount();
		} catch (Exception e) {
			log.warn("unable to get messagecount from storage", e);
			messageCount = -1;
		}

		Map<String, Object> returnObj = new HashMap<>(3);
		returnObj.put("totalMessages", messageCount);
		returnObj.put("skipMessages", filter.skipMessages());
		returnObj.put("messageCount", messageCount - filter.skipMessages());

		Date startDate = null;
		Date endDate = null;
		try (IMessageBrowsingIterator iterator = transactionalStorage.getIterator(startDate, endDate, filter.getSortOrder())) {
			int count;
			List<StorageItemDTO> messages = new LinkedList<>();

			for (count=0; iterator.hasNext(); ) {
				try (IMessageBrowsingIteratorItem iterItem = iterator.next()) {
					if(!filter.matchAny(iterItem))
						continue;

					count++;
					if (count > filter.skipMessages() && messages.size() < filter.maxMessages()) {
						StorageItemDTO dto = new StorageItemDTO(iterItem);
						dto.setPosition(count);
						messages.add(dto);
					}
				}
			}

			returnObj.put("recordsFiltered", count);
			returnObj.put("messages", messages);
		} catch (ListenerException|IOException e) {
			throw new ApiException(e);
		}

		return returnObj;
	}

	public Map<ProcessState, Map<String, String>> getTargetProcessStateInfo(Set<ProcessState> targetProcessStates) {
		if(targetProcessStates == null) {
			return null;
		}
		Map<ProcessState, Map<String, String>> result = new LinkedHashMap<>();
		for (ProcessState ps : targetProcessStates) {
			Map<String, String> psInfo = new HashMap<>();
			psInfo.put("name", ps.getName());
			result.put(ps, psInfo);
		}
		return result;
	}

}
