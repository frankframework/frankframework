/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.console.controllers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.annotation.security.RolesAllowed;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import org.frankframework.console.ApiException;
import org.frankframework.console.Relation;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.console.util.RequestUtils;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.util.HttpUtils;

@RestController
public class TransactionalStorage {

	private final FrankApiService frankApiService;

	public TransactionalStorage(FrankApiService frankApiService) {
		this.frankApiService = frankApiService;
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@GetMapping(value = "/configurations/{configuration}/adapters/{adapterName}/{storageSource}/{storageSourceName}/stores/{processState}/messages/{messageId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> browseMessage(@PathVariable("configuration") String configuration, @PathVariable("adapterName") String adapterName,
										   @PathVariable("storageSource") StorageSource storageSource, @PathVariable("storageSourceName") String storageSourceName,
										   @PathVariable("processState") String processState, @PathVariable("messageId") String messageId) {
		// messageId is double URLEncoded, because it can contain '/' in ExchangeMailListener
		return getMessageResponseEntity(configuration, adapterName, storageSource, storageSourceName, processState, HttpUtils.urlDecode(messageId),
				RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.GET)
		);
	}


	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@GetMapping(value = "/configurations/{configuration}/adapters/{adapterName}/{storageSource}/{storageSourceName}/stores/{processState}/messages/{messageId}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<?> downloadMessage(@PathVariable("configuration") String configuration, @PathVariable("adapterName") String adapterName,
											 @PathVariable("storageSource") StorageSource storageSource, @PathVariable("storageSourceName") String storageSourceName,
											 @PathVariable("processState") String processState, @PathVariable("messageId") String messageId) {
		// messageId is double URLEncoded, because it can contain '/' in ExchangeMailListener
		return getMessageResponseEntity(configuration, adapterName, storageSource, storageSourceName, processState, HttpUtils.urlDecode(messageId),
				RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.DOWNLOAD)
		);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@PostMapping(value = "/configurations/{configuration}/adapters/{adapterName}/{storageSource}/{storageSourceName}/stores/{processState}/messages/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<StreamingResponseBody> downloadMessages(@PathVariable("configuration") String configuration, @PathVariable("adapterName") String adapterName,
																  @PathVariable("storageSource") StorageSource storageSource, @PathVariable("storageSourceName") String storageSourceName,
																  @PathVariable("processState") String processState, @RequestPart("messageIds") String messageIdsPart) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.DOWNLOAD);
		addHeaders(configuration, adapterName, storageSource, storageSourceName, processState, builder);

		String[] messageIdArray = getMessageIds(messageIdsPart);

		StreamingResponseBody stream = out -> {
			try (ZipOutputStream zos = new ZipOutputStream(out)) {
				for (String messageId : messageIdArray) {
					// messageId is double URLEncoded, because it can contain '/' in ExchangeMailListener
					String decodedMessageId = HttpUtils.urlDecode(messageId);

					builder.addHeader("messageId", decodedMessageId);
					Message<?> message = frankApiService.sendSyncMessage(builder);
					String mimeType = BusMessageUtils.getHeader(message, MessageBase.MIMETYPE_KEY);

					String filenameExtension = ".txt";
					if (MediaType.APPLICATION_JSON_VALUE.equals(mimeType)) {
						filenameExtension = ".json";
					} else if (MediaType.APPLICATION_XML_VALUE.equals(mimeType)) {
						filenameExtension = ".xml";
					}

					String payload = (String) message.getPayload();
					ZipEntry entry = new ZipEntry("msg-" + decodedMessageId + filenameExtension);
					zos.putNextEntry(entry);
					zos.write(payload.getBytes(StandardCharsets.UTF_8));
					zos.closeEntry();
				}
			} catch (IOException e) {
				throw new ApiException("Failed to create zip file with messages.", e);
			}
		};

		ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
		responseBuilder.contentType(MediaType.APPLICATION_OCTET_STREAM);
		responseBuilder.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"messages.zip\"");
		return responseBuilder.body(stream);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@GetMapping(value = "/configurations/{configuration}/adapters/{adapterName}/{storageSource}/{storageSourceName}/stores/{processState}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> browseMessages(
			@PathVariable("configuration") String configuration,
			@PathVariable("adapterName") String adapterName,
			@PathVariable("storageSource") StorageSource storageSource,
			@PathVariable("storageSourceName") String storageSourceName,
			@PathVariable("processState") String processState,
			@RequestParam(value = "type", required = false) String type,
			@RequestParam(value = "host", required = false) String host,
			@RequestParam(value = "id", required = false) String id,
			@RequestParam(value = "messageId", required = false) String messageId,
			@RequestParam(value = "correlationId", required = false) String correlationId,
			@RequestParam(value = "comment", required = false) String comment,
			@RequestParam(value = "message", required = false) String message,
			@RequestParam(value = "label", required = false) String label,
			@RequestParam(value = "startDate", required = false) String startDateStr,
			@RequestParam(value = "endDate", required = false) String endDateStr,
			@RequestParam(value = "sort", required = false) String sort,
			@RequestParam(value = "skip", required = false, defaultValue = "0") int skipMessages,
			@RequestParam(value = "max", required = false, defaultValue = "0") int maxMessages
	) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.FIND);
		addHeaders(configuration, adapterName, storageSource, storageSourceName, processState, builder);

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
		return frankApiService.callSyncGateway(builder);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("pipeline")
	@PutMapping(value = "/configurations/{configuration}/adapters/{adapterName}/receivers/{receiverName}/stores/Error/messages/{messageId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> resendReceiverMessage(@PathVariable("configuration") String configuration, @PathVariable("adapterName") String adapter,
												   @PathVariable("receiverName") String receiver, @PathVariable("messageId") String messageId) {

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.STATUS);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter);
		builder.addHeader(BusMessageUtils.HEADER_RECEIVER_NAME_KEY, receiver);

		// messageId is double URLEncoded, because it can contain '/' in ExchangeMailListener
		builder.addHeader("messageId", HttpUtils.urlDecode(messageId));
		return frankApiService.callAsyncGateway(builder);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("pipeline")
	@PostMapping(value = "/configurations/{configuration}/adapters/{adapterName}/receivers/{receiverName}/stores/Error", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> resendReceiverMessages(@PathVariable("configuration") String configuration, @PathVariable("adapterName") String adapter,
													@PathVariable("receiverName") String receiver, @RequestPart("messageIds") String messageIdsPart) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.STATUS);

		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter);
		builder.addHeader(BusMessageUtils.HEADER_RECEIVER_NAME_KEY, receiver);

		return getResponseForMessageIds(messageIdsPart, builder);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("pipeline")
	@PostMapping(value = "/configurations/{configuration}/adapters/{adapterName}/receivers/{receiverName}/stores/{processState}/move/{targetState}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> changeProcessState(@PathVariable("configuration") String configuration, @PathVariable("adapterName") String adapter,
												@PathVariable("receiverName") String receiver, @PathVariable("processState") String processState,
												@PathVariable("targetState") String targetState, @RequestPart("messageIds") String messageIdsPart) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.MANAGE);

		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter);
		builder.addHeader(BusMessageUtils.HEADER_RECEIVER_NAME_KEY, receiver);

		builder.addHeader("processState", processState);
		builder.addHeader("targetState", targetState);

		return getResponseForMessageIds(messageIdsPart, builder);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("pipeline")
	@DeleteMapping(value = "/configurations/{configuration}/adapters/{adapterName}/receivers/{receiverName}/stores/Error/messages/{messageId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> deleteReceiverMessage(@PathVariable("configuration") String configuration, @PathVariable("adapterName") String adapter,
												   @PathVariable("receiverName") String receiver, @PathVariable("messageId") String messageId) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.DELETE);

		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter);
		builder.addHeader(BusMessageUtils.HEADER_RECEIVER_NAME_KEY, receiver);

		// messageId is double URLEncoded, because it can contain '/' in ExchangeMailListener
		messageId = HttpUtils.urlDecode(messageId);

		builder.addHeader("messageId", messageId);
		return frankApiService.callAsyncGateway(builder);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("pipeline")
	@DeleteMapping(value = "/configurations/{configuration}/adapters/{adapterName}/receivers/{receiverName}/stores/Error", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> deleteReceiverMessages(@PathVariable("configuration") String configuration, @PathVariable("adapterName") String adapter,
													@PathVariable("receiverName") String receiver, @RequestPart("messageIds") String messageIdsPart) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.DELETE);

		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter);
		builder.addHeader(BusMessageUtils.HEADER_RECEIVER_NAME_KEY, receiver);

		return getResponseForMessageIds(messageIdsPart, builder);
	}

	private ResponseEntity<?> getResponseForMessageIds(String messageIdsPart, RequestMessageBuilder builder) {
		String[] messageIds = getMessageIds(messageIdsPart);

		List<String> errorMessages = new ArrayList<>();
		for (String messageId : messageIds) {
			try {
				builder.addHeader("messageId", messageId);
				frankApiService.callAsyncGateway(builder);
			} catch (ApiException e) { //The message of an ApiException is wrapped in HTML, try to get the original message instead!
				errorMessages.add(e.getCause().getMessage());
			} catch (Exception e) {
				errorMessages.add(e.getMessage());
			}
		}

		if (errorMessages.isEmpty()) {
			return ResponseEntity.status(HttpStatus.OK).build();
		}

		return ResponseEntity.status(HttpStatus.ACCEPTED).body(errorMessages);
	}

	private String[] getMessageIds(String messageIdsPart) {
		String messageIds = RequestUtils.resolveRequiredProperty("messageIds", messageIdsPart, null);
		return messageIds.split(",");
	}

	private ResponseEntity<?> getMessageResponseEntity(String configuration, String adapterName,
													   StorageSource storageSource, String storageSourceName,
													   String processState, String messageId, RequestMessageBuilder builder) {
		addHeaders(configuration, adapterName, storageSource, storageSourceName, processState, builder);
		builder.addHeader("messageId", messageId);

		return frankApiService.callSyncGateway(builder);
	}

	private void addHeaders(String configuration, String adapterName, StorageSource storageSource, String storageSourceName, String processState, RequestMessageBuilder builder) {
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapterName);

		if (storageSource == StorageSource.PIPES) {
			builder.addHeader("pipe", storageSourceName);
		} else {
			builder.addHeader(BusMessageUtils.HEADER_RECEIVER_NAME_KEY, storageSourceName);
			builder.addHeader("processState", processState);
		}
	}

	public enum StorageSource {
		RECEIVERS, PIPES
	}
}
