package nl.nn.adapterframework.webcontrol.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.CalendarParserException;
import nl.nn.adapterframework.util.DateUtils;

@Path("/")
public class TransactionalStorage extends Base {

	@Context ServletConfig servletConfig;
	@Context Request request;

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

		initBase(servletConfig);

		Adapter adapter = (Adapter) ibisManager.getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		ReceiverBase receiver = (ReceiverBase) adapter.getReceiverByName(receiverName);
		if(receiver == null) {
			throw new ApiException("Receiver ["+receiverName+"] not found!");
		}

		//StorageType
		ITransactionalStorage storage;
		if(storageType.equals("messagelog"))
			storage = receiver.getMessageLog();
		else
			storage = receiver.getErrorStorage();

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
				filter.setSortDescending();

		return Response.status(Response.Status.OK).entity(getMessages(storage, filter)).build();
	}

	@GET
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}/browse/{messageId}")
	public Response browseReceiverMessage(
				@PathParam("adapterName") String adapterName,
				@PathParam("receiverName") String receiverName,
				@PathParam("messageId") String messageId
			) throws ApiException {

		initBase(servletConfig);
		Adapter adapter = (Adapter) ibisManager.getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		ReceiverBase receiver = (ReceiverBase) adapter.getReceiverByName(receiverName);
		if(receiver == null) {
			throw new ApiException("Receiver ["+receiverName+"] not found!");
		}

		ITransactionalStorage storage = receiver.getMessageLog();

		String msg = null;
		try {
			Object rawmsg = storage.browseMessage(messageId);
	
			if (receiver.getListener() != null) {
				msg = receiver.getListener().getStringFromRawMessage(rawmsg, null);
			} else {
				msg = (String) rawmsg;
			}
		} catch (ListenerException e) {
			throw new ApiException(e);
		}

		MediaType type = MediaType.TEXT_PLAIN_TYPE;
		if (StringUtils.isEmpty(msg)) {
			throw new ApiException("message not found");
		}
		else {
			if(msg.startsWith("<"))
				type = MediaType.APPLICATION_XML_TYPE;
			if(msg.startsWith("{"))
				type = MediaType.APPLICATION_JSON_TYPE;
		}

		return Response.status(Response.Status.OK).type(type).entity(msg).build();
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

		initBase(servletConfig);

		Adapter adapter = (Adapter) ibisManager.getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		MessageSendingPipe pipe = (MessageSendingPipe) adapter.getPipeLine().getPipe(pipeName);
		if(pipe == null) {
			throw new ApiException("Pipe ["+pipeName+"] not found!");
		}

		ITransactionalStorage storage = pipe.getMessageLog();

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
				filter.setSortDescending();

		return Response.status(Response.Status.OK).entity(getMessages(storage, filter)).build();
	}

	@GET
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/pipes/{pipeName}/browse/{messageId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response browsePipeMessage(
				@PathParam("adapterName") String adapterName,
				@PathParam("pipeName") String pipeName,
				@PathParam("messageId") String messageId
			) throws ApiException {

		initBase(servletConfig);
		Adapter adapter = (Adapter) ibisManager.getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		MessageSendingPipe pipe = (MessageSendingPipe) adapter.getPipeLine().getPipe(pipeName);
		if(pipe == null) {
			throw new ApiException("Pipe ["+pipeName+"] not found!");
		}

		ITransactionalStorage storage = pipe.getMessageLog();

		return Response.status(Response.Status.OK).entity(getMessage(storage)).build();
	}

	private Map<String, Object> getMessage(ITransactionalStorage transactionalStorage) {
		Map<String, Object> returnObj = new HashMap<String, Object>(3);
		
		return returnObj;
	}

	private Map<String, Object> getMessages(ITransactionalStorage transactionalStorage, MessageBrowsingFilter filter) {
		int messageCount = 0;
		try {
			messageCount = transactionalStorage.getMessageCount();
		} catch (Exception e) {
			log.warn(e);
		}

		Map<String, Object> returnObj = new HashMap<String, Object>(3);
		returnObj.put("totalMessages", messageCount);
		returnObj.put("skipMessages", filter.skipMessages());
		returnObj.put("messageCount", messageCount - filter.skipMessages());

		Date startDate = null;
		Date endDate = null;
		try {
			IMessageBrowsingIterator iterator = null;
			try {
				iterator = transactionalStorage.getIterator(startDate, endDate, filter.isSortDescending());
	
				int count;
				ArrayList<Object> messages = new ArrayList<Object>();
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
			}
			finally {
				if(iterator != null)
					iterator.close();
			}
		}
		catch (ListenerException e) {
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

		private boolean sortDescending = false;
		private ITransactionalStorage storage = null;
		private IListener listener = null;

		public MessageBrowsingFilter() {
			this(AppConstants.getInstance().getInt("browse.messages.max", 0), 0);
		}

		public MessageBrowsingFilter(int maxMessages, int skipMessages) {
			this.maxMessages = maxMessages;
			this.skipMessages = skipMessages;
		}

		public void setSortDescending() {
			sortDescending = true;
		}
		public boolean isSortDescending() {
			return sortDescending;
		}

		public boolean matchAny(IMessageBrowsingIteratorItem iterItem) throws ListenerException {
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
				matches += iterItem.getInsertDate().before(startDate) ? 1 : 0;
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

		public boolean matchMessage(IMessageBrowsingIteratorItem iterItem) throws ListenerException {
			if(message != null) {
				Object rawmsg = storage.browseMessage(iterItem.getId());
				String msg = null;
				if (listener != null) {
					msg = listener.getStringFromRawMessage(rawmsg, new HashMap<String, Object>());
				} else {
					msg = (String) rawmsg;
				}
				if (msg == null || msg.indexOf(message)<0) {
					return false;
				}
			}
			return true;
		}

		public void setMessageMask(String messageMask, ITransactionalStorage storage) {
			setMessageMask(messageMask, storage, null);
		}

		public void setMessageMask(String messageMask, ITransactionalStorage storage, IListener listener) {
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
					throw new ApiException("could not parse date from ["+startDateMask+"] msg["+ex.toString()+"]");
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
					throw new ApiException("could not parse date from ["+endDateMask+"] msg["+ex.toString()+"]");
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
