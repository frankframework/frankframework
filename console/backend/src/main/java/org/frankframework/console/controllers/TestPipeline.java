/*
   Copyright 2024-2025 WeAreFrank!

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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.annotation.security.RolesAllowed;

import org.apache.commons.lang3.Strings;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.frankframework.console.ApiException;
import org.frankframework.console.Description;
import org.frankframework.console.Relation;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.console.util.RequestUtils;
import org.frankframework.console.util.ResponseUtils;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.XmlEncodingUtils;

@RestController
public class TestPipeline {

	private final FrankApiService frankApiService;

	public TestPipeline(FrankApiService frankApiService) {
		this.frankApiService = frankApiService;
	}

	@RolesAllowed("IbisTester")
	@Relation("testing")
	@Description("send a message to an Adapters pipeline, bypassing the receiver")
	@PostMapping(value = "/test-pipeline", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<TestPipeLineResponse> testPipeLine(@ModelAttribute TestPipeLineModel model) throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.TEST_PIPELINE, BusAction.UPLOAD);

		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, model.configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, model.adapter);

		Optional.ofNullable(model.sessionKeys)
				.ifPresent(sessionKeys -> builder.addHeader("sessionKeys", sessionKeys));

		String fileEncoding = Optional.ofNullable(model.encoding).orElse(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);

		String inputMessage;

		if (model.file != null) {
			String fileNameOrPath = model.file.getOriginalFilename();
			String fileName = Paths.get(fileNameOrPath).getFileName().toString();

			if (Strings.CI.endsWith(fileName, ".zip")) {
				try {
					InputStream file = model.file.getInputStream();
					String zipResults = processZipFile(file, builder);
					return testPipelineResponse(zipResults);
				} catch (Exception e) {
					throw new ApiException("An exception occurred while processing zip file", e);
				}
			} else {
				try {
					InputStream file = model.file.getInputStream();
					inputMessage = XmlEncodingUtils.readXml(file, fileEncoding);
				} catch (UnsupportedEncodingException e) {
					throw new ApiException("unsupported file encoding [" + fileEncoding + "]");
				} catch (IOException e) {
					throw new ApiException("error reading file", e);
				}
			}
		} else {
			inputMessage = RequestUtils.resolveStringWithEncoding("message", model.message, fileEncoding, false);
		}

		builder.setPayload(inputMessage);
		Message<?> response = frankApiService.sendSyncMessage(builder);
		String state = BusMessageUtils.getHeader(response, MessageBase.STATE_KEY);
		return testPipelineResponse(ResponseUtils.parseAsString(response), state, inputMessage);
	}

	private ResponseEntity<TestPipeLineResponse> testPipelineResponse(String payload) {
		return testPipelineResponse(payload, "SUCCESS", null);
	}

	private ResponseEntity<TestPipeLineResponse> testPipelineResponse(String payload, String state, String message) {
		return ResponseEntity.status(200)
				.body(new TestPipeLineResponse(payload, state, message));
	}

	// cannot call callAsyncGateway, backend calls are not synchronous
	private String processZipFile(InputStream file, RequestMessageBuilder builder) throws IOException {
		StringBuilder result = new StringBuilder();

		try (ZipInputStream archive = new ZipInputStream(file)) {
			ZipEntry zipEntry;
			while ((zipEntry = archive.getNextEntry()) != null) {
				String name = zipEntry.getName();
				String currentMessage = XmlEncodingUtils.readXml(StreamUtil.streamToBytes(CloseUtils.dontClose(archive)), null);

				builder.setPayload(currentMessage);
				Message<?> response = frankApiService.sendSyncMessage(builder);
				result.append(name);
				result.append(": ");
				result.append(BusMessageUtils.getHeader(response, MessageBase.STATE_KEY));
				result.append("\n");
			}
		}

		return result.toString();
	}

	public record TestPipeLineModel(
			String configuration,
			String adapter,
			String sessionKeys,
			String encoding,
			MultipartFile message,
			MultipartFile file) {
	}

	public record TestPipeLineResponse(
			String result,
			String state,
			String message) {
	}
}
