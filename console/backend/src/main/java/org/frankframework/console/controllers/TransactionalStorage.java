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
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.annotation.security.RolesAllowed;

import lombok.Getter;

import lombok.Setter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

@RestController
public class TransactionalStorage {

	private final FrankApiService frankApiService;

	public TransactionalStorage(FrankApiService frankApiService) {
		this.frankApiService = frankApiService;
	}

	public static String decodeBase64(String input) {
		byte[] decoded = Base64.getDecoder().decode(input);
		return new String(decoded, StandardCharsets.UTF_8);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@GetMapping(value = "/configurations/{configuration}/adapters/{adapterName}/{storageSource}/{storageSourceName}/stores/{processState}/messages/{messageId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> browseMessage(TransactionStoragePathVariables path) {
		// messageId is Base64 encoded, because it can contain '/' in ExchangeMailListener
		return getMessageResponseEntity(path.configuration, path.adapterName, path.storageSource, path.storageSourceName, path.processState, decodeBase64(path.messageId),
				RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.GET)
		);
	}


	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@GetMapping(value = "/configurations/{configuration}/adapters/{adapterName}/{storageSource}/{storageSourceName}/stores/{processState}/messages/{messageId}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<?> downloadMessage(TransactionStoragePathVariables path) {
		// messageId is Base64 encoded, because it can contain '/' in ExchangeMailListener
		return getMessageResponseEntity(path.configuration, path.adapterName, path.storageSource, path.storageSourceName, path.processState, decodeBase64(path.messageId),
				RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.DOWNLOAD)
		);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@PostMapping(value = "/configurations/{configuration}/adapters/{adapterName}/{storageSource}/{storageSourceName}/stores/{processState}/messages/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<StreamingResponseBody> downloadMessages(TransactionStoragePathVariables path, @RequestPart("messageIds") String messageIdsPart) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.DOWNLOAD);
		addHeaders(path.configuration, path.adapterName, path.storageSource, path.storageSourceName, path.processState, builder);

		String[] messageIdArray = getMessageIds(messageIdsPart);

		StreamingResponseBody stream = out -> {
			try (ZipOutputStream zos = new ZipOutputStream(out)) {
				for (String messageId : messageIdArray) {
					// messageId is Base64 encoded, because it can contain '/' in ExchangeMailListener
					String decodedMessageId = decodeBase64(messageId);

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
			TransactionStoragePathVariables path,
			BrowseMessagesParams params
	) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.FIND);
		addHeaders(path.configuration, path.adapterName, path.storageSource, path.storageSourceName, path.processState, builder);

		builder.addHeader("skip", params.skip);
		builder.addHeader("max", params.max);

		builder.addHeader("type", params.type);
		builder.addHeader("host", params.host);
		builder.addHeader("idMask", params.id);
		builder.addHeader("messageId", params.messageId);
		builder.addHeader("correlationId", params.correlationId);
		builder.addHeader("comment", params.comment);
		builder.addHeader("message", params.message);
		builder.addHeader("label", params.label);
		builder.addHeader("startDate", params.startDateStr);
		builder.addHeader("endDate", params.endDateStr);
		builder.addHeader("sort", params.sort);
		builder.addHeader("skip", params.skipMessages);
		builder.addHeader("max", params.maxMessages);
		return frankApiService.callSyncGateway(builder);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("pipeline")
	@PutMapping(value = "/configurations/{configuration}/adapters/{adapterName}/receivers/{receiverName}/stores/Error/messages/{messageId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> resendReceiverMessage(TransactionStoragePathVariables path) {

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.STATUS);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, path.configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, path.adapterName);
		builder.addHeader(BusMessageUtils.HEADER_RECEIVER_NAME_KEY, path.receiverName);

		// messageId is Base64 encoded, because it can contain '/' in ExchangeMailListener
		builder.addHeader("messageId", decodeBase64(path.messageId));
		return frankApiService.callAsyncGateway(builder);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("pipeline")
	@PostMapping(value = "/configurations/{configuration}/adapters/{adapterName}/receivers/{receiverName}/stores/Error", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> resendReceiverMessages(TransactionStoragePathVariables path,
													@RequestPart("messageIds") String messageIdsPart) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.STATUS);

		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, path.configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, path.adapterName);
		builder.addHeader(BusMessageUtils.HEADER_RECEIVER_NAME_KEY, path.receiverName);

		return getResponseForMessageIds(messageIdsPart, builder);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("pipeline")
	@PostMapping(value = "/configurations/{configuration}/adapters/{adapterName}/receivers/{receiverName}/stores/{processState}/move/{targetState}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> changeMessagesProcessState(TransactionStoragePathVariables path,
														@RequestPart("messageIds") String messageIdsPart) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.MANAGE);

		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, path.configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, path.adapterName);
		builder.addHeader(BusMessageUtils.HEADER_RECEIVER_NAME_KEY, path.receiverName);

		builder.addHeader("processState", path.processState);
		builder.addHeader("targetState", path.targetState);

		return getResponseForMessageIds(messageIdsPart, builder);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("pipeline")
	@DeleteMapping(value = "/configurations/{configuration}/adapters/{adapterName}/receivers/{receiverName}/stores/Error/messages/{messageId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> deleteReceiverMessage(TransactionStoragePathVariables path) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.DELETE);

		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, path.configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, path.adapterName);
		builder.addHeader(BusMessageUtils.HEADER_RECEIVER_NAME_KEY, path.receiverName);

		// messageId is Base64 encoded, because it can contain '/' in ExchangeMailListener
		String messageId = decodeBase64(path.messageId);

		builder.addHeader("messageId", messageId);
		return frankApiService.callAsyncGateway(builder);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("pipeline")
	@DeleteMapping(value = "/configurations/{configuration}/adapters/{adapterName}/receivers/{receiverName}/stores/Error", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> deleteReceiverMessages(TransactionStoragePathVariables path,
													@RequestPart("messageIds") String messageIdsPart) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.DELETE);

		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, path.configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, path.adapterName);
		builder.addHeader(BusMessageUtils.HEADER_RECEIVER_NAME_KEY, path.receiverName);

		return getResponseForMessageIds(messageIdsPart, builder);
	}

	public record TransactionStoragePathVariables(
			String configuration,
			String adapterName,
			String storageSourceName,
			String receiverName,
			String processState,
			String targetState,
			String messageId,
			StorageSource storageSource
	) {}

	@Getter
	@Setter
	public static class BrowseMessagesParams {
		private Integer skip = 0;
		private Integer max = 0;
		private String type;
		private String host;
		private String id;
		private String messageId;
		private String correlationId;
		private String comment;
		private String message;
		private String label;
		private String startDateStr;
		private String endDateStr;
		private String sort;
		private String skipMessages;
		private String maxMessages;
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
