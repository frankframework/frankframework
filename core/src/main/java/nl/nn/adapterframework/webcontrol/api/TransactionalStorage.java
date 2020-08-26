/*
Copyright 2018-2020 WeAreFrank!

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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowser.SortOrder;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.MessageWrapper;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.CalendarParserException;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.Misc;

@Path("/")
public class TransactionalStorage extends Base {

	protected static final TransactionDefinition TXNEW = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

	@GET
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}/{storageType:messagelog|errorstorage}/{messageId}")
	public Response browseReceiverMessage(
				@PathParam("adapterName") String adapterName,
				@PathParam("receiverName") String receiverName,
				@PathParam("storageType") String storageType,
				@PathParam("messageId") String messageId
			) throws ApiException {

		Adapter adapter = (Adapter) getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		ReceiverBase receiver = (ReceiverBase) adapter.getReceiverByName(receiverName);
		if(receiver == null) {
			throw new ApiException("Receiver ["+receiverName+"] not found!");
		}

		//StorageType
		IMessageBrowser storage;
		if(storageType.equals("messagelog"))
			storage = receiver.getMessageLogBrowser();
		else
			storage = receiver.getErrorStorageBrowser();

		return getMessage(storage, receiver.getListener(), messageId);
	}

	@GET
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}/{storageType:messagelog|errorstorage}/{messageId}/download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadMessage(
			@PathParam("adapterName") String adapterName,
			@PathParam("receiverName") String receiverName,
			@PathParam("storageType") String storageType,
			@PathParam("messageId") String messageId
		) throws ApiException {

		Adapter adapter = (Adapter) getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		ReceiverBase receiver = (ReceiverBase) adapter.getReceiverByName(receiverName);
		if(receiver == null) {
			throw new ApiException("Receiver ["+receiverName+"] not found!");
		}

		//StorageType
		IMessageBrowser storage;
		if(storageType.equals("messagelog"))
			storage = receiver.getMessageLogBrowser();
		else
			storage = receiver.getErrorStorageBrowser();

		return getMessage(storage, receiver.getListener(), messageId);
	}

	@GET
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}/{storageType:messagelog|errorstorage}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response browseReceiverMessages(
				@PathParam("adapterName") String adapterName,
				@PathParam("receiverName") String receiverName,
				@PathParam("storageType") String storageType,
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

		Adapter adapter = (Adapter) getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		ReceiverBase receiver = (ReceiverBase) adapter.getReceiverByName(receiverName);
		if(receiver == null) {
			throw new ApiException("Receiver ["+receiverName+"] not found!");
		}

		//StorageType
		IMessageBrowser storage;
		if(storageType.equals("messagelog"))
			storage = receiver.getMessageLogBrowser();
		else
			storage = receiver.getErrorStorageBrowser();

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
		filter.setMessageMask(message, storage, receiver.getListener());
		filter.setLabelMask(label);
		filter.setStartDateMask(startDateStr);
		filter.setEndDateMask(endDateStr);
	
		if("desc".equalsIgnoreCase(sort))
			filter.setSortOrder(SortOrder.DESC);
		if("asc".equalsIgnoreCase(sort))
			filter.setSortOrder(SortOrder.ASC);

		return Response.status(Response.Status.OK).entity(getMessages(storage, filter)).build();
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}/errorstorage/{messageId}")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	public Response resendReceiverMessage(
			@PathParam("adapterName") String adapterName,
			@PathParam("receiverName") String receiverName,
			@PathParam("messageId") String messageId
		) throws ApiException {

		Adapter adapter = (Adapter) getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		ReceiverBase receiver = (ReceiverBase) adapter.getReceiverByName(receiverName);
		if(receiver == null) {
			throw new ApiException("Receiver ["+receiverName+"] not found!");
		}

		resendMessage(receiver, messageId);

		return Response.status(Response.Status.OK).build();
	}

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}/errorstorage")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response resendReceiverMessages(
			@PathParam("adapterName") String adapterName,
			@PathParam("receiverName") String receiverName,
			MultipartBody input
		) throws ApiException {

		Adapter adapter = (Adapter) getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		ReceiverBase receiver = (ReceiverBase) adapter.getReceiverByName(receiverName);
		if(receiver == null) {
			throw new ApiException("Receiver ["+receiverName+"] not found!");
		}

		String[] messageIds = getMessages(input);

		List<String> errorMessages = new ArrayList<String>();
		for(int i=0; i < messageIds.length; i++) {
			try {
				resendMessage(receiver, messageIds[i]);
			}
			catch(Exception e) {
				if(e instanceof ApiException) {
					//The message of an ApiException is wrapped in HTML, try to get the original message instead!
					errorMessages.add(e.getCause().getMessage());
				}
				else
					errorMessages.add(e.getMessage());
			}
		}

		if(errorMessages.size() == 0)
			return Response.status(Response.Status.OK).build();

		return Response.status(Response.Status.ACCEPTED).entity(errorMessages).build();
	}

	@DELETE
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}/errorstorage/{messageId}")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteReceiverMessage(
			@PathParam("adapterName") String adapterName,
			@PathParam("receiverName") String receiverName,
			@PathParam("messageId") String messageId
		) throws ApiException {

		Adapter adapter = (Adapter) getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		ReceiverBase receiver = (ReceiverBase) adapter.getReceiverByName(receiverName);
		if(receiver == null) {
			throw new ApiException("Receiver ["+receiverName+"] not found!");
		}

		deleteMessage(receiver.getErrorStorageBrowser(), messageId);

		return Response.status(Response.Status.OK).build();
	}

	@DELETE
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}/errorstorage")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response deleteReceiverMessages(
			@PathParam("adapterName") String adapterName,
			@PathParam("receiverName") String receiverName,
			MultipartBody input
		) throws ApiException {

		Adapter adapter = (Adapter) getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		ReceiverBase receiver = (ReceiverBase) adapter.getReceiverByName(receiverName);
		if(receiver == null) {
			throw new ApiException("Receiver ["+receiverName+"] not found!");
		}

		String[] messageIds = getMessages(input);

		List<String> errorMessages = new ArrayList<String>();
		for(int i=0; i < messageIds.length; i++) {
			try {
				deleteMessage(receiver.getErrorStorageBrowser(), messageIds[i]);
			}
			catch(Exception e) {
				if(e instanceof ApiException) {
					//The message of an ApiException is wrapped in HTML, try to get the original message instead!
					errorMessages.add(e.getCause().getMessage());
				}
				else
					errorMessages.add(e.getMessage());
			}
		}

		if(errorMessages.size() == 0)
			return Response.status(Response.Status.OK).build();

		return Response.status(Response.Status.ACCEPTED).entity(errorMessages).build();
	}

	@GET
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/pipes/{pipeName}/messagelog/{messageId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response browsePipeMessage(
				@PathParam("adapterName") String adapterName,
				@PathParam("pipeName") String pipeName,
				@PathParam("messageId") String messageId
			) throws ApiException {

		Adapter adapter = (Adapter) getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		MessageSendingPipe pipe = (MessageSendingPipe) adapter.getPipeLine().getPipe(pipeName);
		if(pipe == null) {
			throw new ApiException("Pipe ["+pipeName+"] not found!");
		}

		return getMessage(pipe.getMessageLog(), messageId);
	}

	@GET
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/pipes/{pipeName}/messagelog/{messageId}/download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadPipeMessage(
			@PathParam("adapterName") String adapterName,
			@PathParam("pipeName") String pipeName,
			@PathParam("messageId") String messageId
		) throws ApiException {

		Adapter adapter = (Adapter) getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		MessageSendingPipe pipe = (MessageSendingPipe) adapter.getPipeLine().getPipe(pipeName);
		if(pipe == null) {
			throw new ApiException("Pipe ["+pipeName+"] not found!");
		}

		return getMessage(pipe.getMessageLog(), messageId);
	}

	@GET
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/pipes/{pipeName}/messagelog")
	@Produces(MediaType.APPLICATION_JSON)
	public Response browsePipeMessages(
				@PathParam("adapterName") String adapterName,
				@PathParam("pipeName") String pipeName,
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

		Adapter adapter = (Adapter) getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		MessageSendingPipe pipe = (MessageSendingPipe) adapter.getPipeLine().getPipe(pipeName);
		if(pipe == null) {
			throw new ApiException("Pipe ["+pipeName+"] not found!");
		}

		IMessageBrowser storage = pipe.getMessageLog();

		//Apply filters
		MessageBrowsingFilter filter = new MessageBrowsingFilter(maxMessages, skipMessages);
		filter.setTypeMask(type);
		filter.setHostMask(host);
		filter.setIdMask(id);
		filter.setMessageIdMask(messageId);
		filter.setCorrelationIdMask(correlationId);
		filter.setCommentMask(comment);
		filter.setMessageMask(message, storage);
		filter.setLabelMask(label);
		filter.setStartDateMask(startDateStr);
		filter.setEndDateMask(endDateStr);

		if("desc".equalsIgnoreCase(sort))
			filter.setSortOrder(SortOrder.DESC);
		if("asc".equalsIgnoreCase(sort))
			filter.setSortOrder(SortOrder.ASC);

		return Response.status(Response.Status.OK).entity(getMessages(storage, filter)).build();
	}

	private String[] getMessages(MultipartBody inputDataMap) {
		String messageIds = resolveStringFromMap(inputDataMap, "messageIds");
		return messageIds.split(",");
	}

	private void deleteMessage(IMessageBrowser storage, String messageId) {
		PlatformTransactionManager transactionManager = getIbisManager().getTransactionManager();
		TransactionStatus txStatus = null;
		try {
			txStatus = transactionManager.getTransaction(TXNEW);
			storage.deleteMessage(messageId);
		} catch (Exception e) {
			txStatus.setRollbackOnly();
			throw new ApiException(e);
		} finally { 
			transactionManager.commit(txStatus);
		}
	}

	private void resendMessage(ReceiverBase receiver, String messageId) {
		try {
			receiver.retryMessage(messageId);
		} catch (ListenerException e) {
			throw new ApiException(e);
		}
	}

	private Response getMessage(IMessageBrowser messageBrowser, String messageId) {
		return getMessage(messageBrowser, null, messageId);
	}

	private Response getMessage(IMessageBrowser messageBrowser, IListener listener, String messageId) {
		return buildResponse(getRawMessage(messageBrowser, listener, messageId), messageId);
	}

	private String getRawMessage(IMessageBrowser messageBrowser, IListener listener, String messageId) {
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
					MessageWrapper msgsgs = (MessageWrapper) rawmsg;
					msg = msgsgs.getMessage().asString();
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

	private Response buildResponse(String msg, String fileName) {
		MediaType type = MediaType.TEXT_PLAIN_TYPE;
		String fileNameExtension = "txt";
		if (StringUtils.isEmpty(msg)) {
			throw new ApiException("message not found");
		} else {
			if(msg.startsWith("<")) {
				type = MediaType.APPLICATION_XML_TYPE;
				fileNameExtension = "xml";
			} else if(msg.startsWith("{") || msg.startsWith("[")) {
				type = MediaType.APPLICATION_JSON_TYPE;
				fileNameExtension = "json";
			}
		}

		return Response.status(Response.Status.OK)
				.type(type)
				.entity(msg)
				.header("Content-Disposition", "attachment; filename=\"msg-"+fileName+"."+fileNameExtension+"\"")
				.build();
	}

	private Map<String, Object> getMessages(IMessageBrowser transactionalStorage, MessageBrowsingFilter filter) {
		int messageCount = 0;
		try {
			messageCount = ((ITransactionalStorage) transactionalStorage).getMessageCount();
		} catch (Exception e) {
			log.warn(e);
			messageCount = -1;
		}

		Map<String, Object> returnObj = new HashMap<String, Object>(3);
		returnObj.put("totalMessages", messageCount);
		returnObj.put("skipMessages", filter.skipMessages());
		returnObj.put("messageCount", messageCount - filter.skipMessages());

		Date startDate = null;
		Date endDate = null;
		try (IMessageBrowsingIterator iterator = transactionalStorage.getIterator(startDate, endDate, filter.getSortOrder())) {
			int count;
			List<Object> messages = new LinkedList<Object>();
			
			for (count=0; iterator.hasNext(); ) {
				IMessageBrowsingIteratorItem iterItem = iterator.next();
				try {
					if(!filter.matchAny(iterItem))
						continue;

					count++;
					if (count > filter.skipMessages()) { 
						Map<String, Object> message = new HashMap<String, Object>(3);

						message.put("id", iterItem.getId());
						message.put("pos", count);
						message.put("originalId", iterItem.getOriginalId());
						message.put("correlationId", iterItem.getCorrelationId());
						message.put("type", iterItem.getType());
						message.put("host", iterItem.getHost());
						message.put("insertDate", iterItem.getInsertDate());
						message.put("expiryDate", iterItem.getExpiryDate());
						message.put("comment", iterItem.getCommentString());
						message.put("label", iterItem.getLabel());
						messages.add(message);
					}
	
					if (filter.maxMessages() > 0 && count >= (filter.maxMessages() + filter.skipMessages())) {
						log.warn("stopped iterating messages after ["+count+"]: limit reached");
						break;
					}
				} finally {
					iterItem.release();
				}
			}
			returnObj.put("messages", messages);
		} catch (ListenerException|IOException e) {
			throw new ApiException(e);
		}

		return returnObj;
	}

	public class MessageBrowsingFilter {
		private String type = null;
		private String host = null;
		private String id = null;
		private String messageId = null;
		private String correlationId = null;
		private String comment = null;
		private String message = null;
		private String label = null;
		private Date startDate = null;
		private Date endDate = null;

		private int maxMessages = 0;
		private int skipMessages = 0;

		private SortOrder sortOrder = SortOrder.NONE;
		private IMessageBrowser storage = null;
		private IListener listener = null;

		public MessageBrowsingFilter() {
			this(AppConstants.getInstance().getInt("browse.messages.max", 0), 0);
		}

		public MessageBrowsingFilter(int maxMessages, int skipMessages) {
			this.maxMessages = maxMessages;
			this.skipMessages = skipMessages;
		}

		public void setSortOrder(SortOrder order) {
			sortOrder = order;
		}
		public SortOrder getSortOrder() {
			return sortOrder;
		}

		public boolean matchAny(IMessageBrowsingIteratorItem iterItem) throws ListenerException, IOException {
			int count = 0;
			int matches = 0;

			if(type != null) {
				count++;
				matches += iterItem.getType().startsWith(type) ? 1 : 0;
			}
			if(host != null) {
				count++;
				matches += iterItem.getHost().startsWith(host) ? 1 : 0;
			}
			if(id != null) {
				count++;
				matches += iterItem.getId().startsWith(id) ? 1 : 0;
			}
			if(messageId != null) {
				count++;
				matches += iterItem.getOriginalId().startsWith(messageId) ? 1 : 0;
			}
			if(correlationId != null) {
				count++;
				matches += iterItem.getCorrelationId().startsWith(correlationId) ? 1 : 0;
			}
			if(comment != null) {
				count++;
				matches += (StringUtils.isNotEmpty(iterItem.getCommentString()) && iterItem.getCommentString().indexOf(comment)>-1) ? 1 : 0;
			}
			if(label != null) {
				count++;
				matches += StringUtils.isNotEmpty(iterItem.getLabel()) && iterItem.getLabel().startsWith(label) ? 1 : 0;
			}
			if(startDate != null && endDate == null) {
				count++;
				matches += iterItem.getInsertDate().after(startDate) ? 1 : 0;
			}
			if(startDate == null && endDate != null) {
				count++;
				matches += iterItem.getInsertDate().before(endDate) ? 1 : 0;
			}
			if(startDate != null && endDate != null) {
				count++;
				matches += (iterItem.getInsertDate().after(startDate) && iterItem.getInsertDate().before(endDate)) ? 1 : 0;
			}
			if(message != null) {
				count++;
				matches += matchMessage(iterItem) ? 1 : 0;
			}

			return count == matches;
		}

		public void setTypeMask(String typeMask) {
			if(!StringUtils.isEmpty(typeMask))
				type = typeMask;
		}

		public void setHostMask(String hostMask) {
			if(!StringUtils.isEmpty(hostMask))
				host = hostMask;
		}

		public void setIdMask(String idMask) {
			if(!StringUtils.isEmpty(idMask))
				id = idMask;
		}

		public void setMessageIdMask(String messageIdMask) {
			if(!StringUtils.isEmpty(messageIdMask))
				messageId = messageIdMask;
		}

		public void setCorrelationIdMask(String correlationIdMask) {
			if(!StringUtils.isEmpty(correlationIdMask))
				correlationId = correlationIdMask;
		}

		public void setCommentMask(String commentMask) {
			if(!StringUtils.isEmpty(commentMask))
				comment = commentMask;
		}

		public boolean matchMessage(IMessageBrowsingIteratorItem iterItem) throws ListenerException, IOException {
			if(message != null) {
				Object rawmsg = storage.browseMessage(iterItem.getId());
				String msg = null;
				if (listener != null) {
					msg = listener.extractMessage(rawmsg, new HashMap<String, Object>()).asString();
				} else {
					msg = Message.asString(rawmsg);
				}
				if (msg == null || msg.indexOf(message)<0) {
					return false;
				}
			}
			return true;
		}

		public void setMessageMask(String messageMask, IMessageBrowser storage) {
			setMessageMask(messageMask, storage, null);
		}

		public void setMessageMask(String messageMask, IMessageBrowser storage, IListener listener) {
			if(StringUtils.isNotEmpty(messageMask)) {
				this.message = messageMask;
				this.storage = storage;
				this.listener = listener;
			}
		}

		public void setLabelMask(String labelMask) {
			if(!StringUtils.isEmpty(labelMask))
				label = labelMask;
		}

		public void setStartDateMask(String startDateMask) {
			if(!StringUtils.isEmpty(startDateMask)) {
				try {
					startDate = DateUtils.parseAnyDate(startDateMask);
					if(startDate == null)
						throw new ApiException("could not to parse date from ["+startDateMask+"]");
				}
				catch(CalendarParserException ex) {
					throw new ApiException("could not parse date from ["+startDateMask+"] msg["+ex.getMessage()+"]");
				}
			}
		}

		public void setEndDateMask(String endDateMask) {
			if(!StringUtils.isEmpty(endDateMask)) {
				try {
					endDate = DateUtils.parseAnyDate(endDateMask);
					if(endDate == null)
						throw new ApiException("could not to parse date from ["+endDateMask+"]");
				}
				catch(CalendarParserException ex) {
					throw new ApiException("could not parse date from ["+endDateMask+"] msg["+ex.getMessage()+"]");
				}
			}
		}

		public int skipMessages() {
			return skipMessages;
		}

		public int maxMessages() {
			return maxMessages;
		}

		public String toString() {
			return ToStringBuilder.reflectionToString(this);
//			return (new ReflectionToStringBuilder(this) {
//				protected boolean accept(Field f) {
//					return super.accept(f) && !f.getName().equals("passwd");
//				}
//			}).toString();
		}
	}
}
