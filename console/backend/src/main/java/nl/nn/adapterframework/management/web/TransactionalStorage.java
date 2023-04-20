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
package nl.nn.adapterframework.management.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
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
import org.springframework.messaging.Message;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.ResponseMessageBase;
import nl.nn.adapterframework.util.HttpUtils;

@Path("/")
public class TransactionalStorage extends FrankApiBase {

	public enum StorageSource {
		RECEIVERS, PIPES;

		public static StorageSource fromString(String value) {
			if(StringUtils.isNotBlank(value)) {
				try {
					return StorageSource.valueOf(value.toUpperCase());
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("invalid StorageSource option");
				}
			}
			throw new IllegalArgumentException("no StorageSource option supplied");
		}
	}

	@GET
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/adapters/{name}/{storageSource}/{storageSourceName}/stores/{processState}/messages/{messageId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response browseMessage(
				@PathParam("configuration") String configuration,
				@PathParam("name") String adapterName,
				@PathParam("storageSource") StorageSource storageSource,
				@PathParam("storageSourceName") String storageSourceName,
				@PathParam("processState") String processState,
				@PathParam("messageId") String messageId
			) {
		// messageId is double URLEncoded, because it can contain '/' in ExchangeMailListener
		messageId = HttpUtils.urlDecode(messageId);

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MESSAGE_BROWSER, BusAction.GET);
		builder.addHeader(HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(HEADER_ADAPTER_NAME_KEY, adapterName);
		if(storageSource == StorageSource.PIPES) {
			builder.addHeader("pipe", storageSourceName);
		} else {
			builder.addHeader(HEADER_RECEIVER_NAME_KEY, storageSourceName);
			builder.addHeader("processState", processState);
		}

		builder.addHeader("messageId", messageId);
		return callSyncGateway(builder);
	}

	@GET
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/adapters/{adapterName}/{storageSource}/{storageSourceName}/stores/{processState}/messages/{messageId}/download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadMessage(
			@PathParam("configuration") String configuration,
			@PathParam("adapterName") String adapterName,
			@PathParam("storageSource") StorageSource storageSource,
			@PathParam("storageSourceName") String storageSourceName,
			@PathParam("processState") String processState,
			@PathParam("messageId") String messageId
		) {

		// messageId is double URLEncoded, because it can contain '/' in ExchangeMailListener
		messageId = HttpUtils.urlDecode(messageId);

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MESSAGE_BROWSER, BusAction.DOWNLOAD);
		builder.addHeader(HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(HEADER_ADAPTER_NAME_KEY, adapterName);
		if(storageSource == StorageSource.PIPES) {
			builder.addHeader("pipe", storageSourceName);
		} else {
			builder.addHeader(HEADER_RECEIVER_NAME_KEY, storageSourceName);
			builder.addHeader("processState", processState);
		}

		builder.addHeader("messageId", messageId);
		return callSyncGateway(builder);
	}

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/adapters/{adapterName}/{storageSource}/{storageSourceName}/stores/{processState}/messages/download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadMessages(
			@PathParam("configuration") String configuration,
			@PathParam("adapterName") String adapterName,
			@PathParam("storageSource") StorageSource storageSource,
			@PathParam("storageSourceName") String storageSourceName,
			@PathParam("processState") String processState,
			MultipartBody input
		) {

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MESSAGE_BROWSER, BusAction.DOWNLOAD);
		builder.addHeader(HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(HEADER_ADAPTER_NAME_KEY, adapterName);
		if(storageSource == StorageSource.PIPES) {
			builder.addHeader("pipe", storageSourceName);
		} else {
			builder.addHeader(HEADER_RECEIVER_NAME_KEY, storageSourceName);
			builder.addHeader("processState", processState);
		}

		String[] messageIdArray = getMessageIds(input);

		StreamingOutput stream = new StreamingOutput() {
			@Override
			public void write(OutputStream out) throws IOException {
				try (ZipOutputStream zos = new ZipOutputStream(out)) {
					for (String messageId : messageIdArray) {
						// messageId is double URLEncoded, because it can contain '/' in ExchangeMailListener
						messageId = HttpUtils.urlDecode(messageId);

						builder.addHeader("messageId", messageId);
						Message<?> message = sendSyncMessage(builder);
						String mimeType = BusMessageUtils.getHeader(message, ResponseMessageBase.MIMETYPE_KEY);

						String filenameExtension = ".txt";
						if(MediaType.APPLICATION_JSON.equals(mimeType)) {
							filenameExtension = ".json";
						} else if(MediaType.APPLICATION_XML.equals(mimeType)) {
							filenameExtension = ".xml";
						}

						String payload = (String) message.getPayload();
						ZipEntry entry = new ZipEntry("msg-"+messageId+filenameExtension);
						zos.putNextEntry(entry);
						zos.write(payload.getBytes());
						zos.closeEntry();
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
	@Path("/configurations/{configuration}/adapters/{adapterName}/{storageSource}/{storageSourceName}/stores/{processState}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response browseMessages(
				@PathParam("configuration") String configuration,
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
			) {


		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MESSAGE_BROWSER, BusAction.FIND);
		builder.addHeader(HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(HEADER_ADAPTER_NAME_KEY, adapterName);
		if(storageSource == StorageSource.PIPES) {
			builder.addHeader("pipe", storageSourceName);
		} else {
			builder.addHeader(HEADER_RECEIVER_NAME_KEY, storageSourceName);
			builder.addHeader("processState", processState);
		}

		builder.addHeader("type", type);
		builder.addHeader("host", host);
		builder.addHeader("idMask", id);
		builder.addHeader("messageId", messageId);
		builder.addHeader("correlationId", correlationId);
		builder.addHeader("comment", comment);
		builder.addHeader("message", message);
		builder.addHeader("label", label);
		builder.addHeader("startDate", startDateStr);
		builder.addHeader("endDate", endDateStr);
		builder.addHeader("sort", sort);
		builder.addHeader("skip", skipMessages);
		builder.addHeader("max", maxMessages);
		return callSyncGateway(builder);
	}




	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/adapters/{adapterName}/receivers/{receiverName}/stores/Error/messages/{messageId}")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	public Response resendReceiverMessage(
			@PathParam("configuration") String configuration,
			@PathParam("adapterName") String adapter,
			@PathParam("receiverName") String receiver,
			@PathParam("messageId") String messageId
		) {

		// messageId is double URLEncoded, because it can contain '/' in ExchangeMailListener
		messageId = HttpUtils.urlDecode(messageId);

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MESSAGE_BROWSER, BusAction.STATUS);
		builder.addHeader(HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(HEADER_ADAPTER_NAME_KEY, adapter);
		builder.addHeader(HEADER_RECEIVER_NAME_KEY, receiver);
		builder.addHeader("messageId", messageId);
		return callAsyncGateway(builder);
	}

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/adapters/{adapterName}/receivers/{receiverName}/stores/Error")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response resendReceiverMessages(@PathParam("configuration") String configuration, @PathParam("adapterName") String adapter, @PathParam("receiverName") String receiver, MultipartBody input) {

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MESSAGE_BROWSER, BusAction.STATUS);
		builder.addHeader(HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(HEADER_ADAPTER_NAME_KEY, adapter);
		builder.addHeader(HEADER_RECEIVER_NAME_KEY, receiver);

		String[] messageIds = getMessageIds(input);

		List<String> errorMessages = new ArrayList<>();
		for(int i=0; i < messageIds.length; i++) {
			try {
				builder.addHeader("messageId", messageIds[i]);
				callAsyncGateway(builder);
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
	@Path("/configurations/{configuration}/adapters/{adapterName}/receivers/{receiverName}/stores/{processState}/move/{targetState}")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response changeProcessState(
			@PathParam("configuration") String configuration,
			@PathParam("adapterName") String adapter,
			@PathParam("receiverName") String receiver,
			@PathParam("processState") String processState,
			@PathParam("targetState") String targetState,
			MultipartBody input) {

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MESSAGE_BROWSER, BusAction.MANAGE);
		builder.addHeader(HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(HEADER_ADAPTER_NAME_KEY, adapter);
		builder.addHeader(HEADER_RECEIVER_NAME_KEY, receiver);
		builder.addHeader("processState", processState);
		builder.addHeader("targetState", targetState);

		String[] messageIds = getMessageIds(input);

		List<String> errorMessages = new ArrayList<>();
		for(int i=0; i < messageIds.length; i++) {
			try {
				builder.addHeader("messageId", messageIds[i]);
				callAsyncGateway(builder);
			} catch(ApiException e) { //The message of an ApiException is wrapped in HTML, try to get the original message instead!
				errorMessages.add(e.getCause().getMessage());
			} catch(Exception e) {
				errorMessages.add(e.getMessage());
			}
		}

		if(errorMessages.isEmpty()) {
			return Response.status(Response.Status.OK).build();
		}

		return Response.status(Response.Status.ACCEPTED).entity(errorMessages).build();
	}

	@DELETE
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/adapters/{adapterName}/receivers/{receiverName}/stores/Error/messages/{messageId}")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteReceiverMessage(
			@PathParam("configuration") String configuration,
			@PathParam("adapterName") String adapter,
			@PathParam("receiverName") String receiver,
			@PathParam("messageId") String messageId) {

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MESSAGE_BROWSER, BusAction.DELETE);
		builder.addHeader(HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(HEADER_ADAPTER_NAME_KEY, adapter);
		builder.addHeader(HEADER_RECEIVER_NAME_KEY, receiver);

		// messageId is double URLEncoded, because it can contain '/' in ExchangeMailListener
		messageId = HttpUtils.urlDecode(messageId);

		builder.addHeader("messageId", messageId);
		return callAsyncGateway(builder);
	}

	@DELETE
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/adapters/{adapterName}/receivers/{receiverName}/stores/Error")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response deleteReceiverMessages(
			@PathParam("configuration") String configuration,
			@PathParam("adapterName") String adapter,
			@PathParam("receiverName") String receiver,
			MultipartBody input) {

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MESSAGE_BROWSER, BusAction.DELETE);
		builder.addHeader(HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(HEADER_ADAPTER_NAME_KEY, adapter);
		builder.addHeader(HEADER_RECEIVER_NAME_KEY, receiver);

		String[] messageIds = getMessageIds(input);

		List<String> errorMessages = new ArrayList<>();
		for(int i=0; i < messageIds.length; i++) {
			try {
				builder.addHeader("messageId", messageIds[i]);
				callAsyncGateway(builder);
			} catch(ApiException e) { //The message of an ApiException is wrapped in HTML, try to get the original message instead!
				errorMessages.add(e.getCause().getMessage());
			} catch(Exception e) {
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
}
