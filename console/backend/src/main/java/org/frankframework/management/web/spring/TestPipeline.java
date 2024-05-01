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
package org.frankframework.management.web.spring;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.management.web.Description;
import org.frankframework.management.web.Relation;
import org.frankframework.util.RequestUtils;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.XmlEncodingUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.security.RolesAllowed;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
public class TestPipeline extends FrankApiBase {

	@RolesAllowed("IbisTester")
	@Relation("testing")
	@Description("send a message to an Adapters pipeline, bypassing the receiver")
	@PostMapping(value = "/test-pipeline", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> testPipeLine(PipelineMultipartBody multipartBody) throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.TEST_PIPELINE, BusAction.UPLOAD);
		String configuration = RequestUtils.resolveRequiredProperty("configuration", multipartBody.getConfiguration(), null);
		builder.addHeader("configuration", configuration);
		String adapterName = RequestUtils.resolveRequiredProperty("adapter", multipartBody.getAdapter(), null);
		builder.addHeader("adapter", adapterName);

		// resolve session keys
		String sessionKeys = RequestUtils.resolveRequiredProperty("sessionKeys", multipartBody.getSessionKeys(), "");
		if(StringUtils.isNotEmpty(sessionKeys)) { //format: [{"index":1,"key":"test","value":"123"}]
			builder.addHeader("sessionKeys", sessionKeys);
		}

		String fileEncoding = RequestUtils.resolveRequiredProperty("encoding", multipartBody.getEncoding(), StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);

		String message;
		MultipartFile filePart = multipartBody.getFile();
		if(filePart != null) {
			String fileNameOrPath = multipartBody.getFile().getOriginalFilename();
			String fileName = Paths.get(fileNameOrPath).getFileName().toString();

			if (StringUtils.endsWithIgnoreCase(fileName, ".zip")) {
				try {
					InputStream file = filePart.getInputStream();
					String zipResults = processZipFile(file, builder);
					return testPipelineResponse(zipResults);
				} catch (Exception e) {
					throw new ApiException("An exception occurred while processing zip file", e);
				}
			}
			else {
				try {
					InputStream file = filePart.getInputStream();
					message = XmlEncodingUtils.readXml(file, fileEncoding);
				} catch (UnsupportedEncodingException e) {
					throw new ApiException("unsupported file encoding ["+fileEncoding+"]");
				} catch (IOException e) {
					throw new ApiException("error reading file", e);
				}
			}
		} else {
			message = RequestUtils.resolveStringWithEncoding("message", multipartBody.getMessage(), fileEncoding);
		}

		if(StringUtils.isEmpty(message)) {
			throw new ApiException("Neither a file nor a message was supplied", 400);
		}

		builder.setPayload(message);
		Message<?> response = sendSyncMessage(builder);
		String state = BusMessageUtils.getHeader(response, MessageBase.STATE_KEY);
		return testPipelineResponse(getPayload(response), state, message);
	}

	@Getter
	@Setter
	public static class PipelineMultipartBody {
		private String configuration;
		private String adapter;
		private String sessionKeys;
		private String encoding;
		private MultipartFile message;
		private MultipartFile file;
	}

	private String getPayload(Message<?> response) {
		Object payload = response.getPayload();
		if(payload instanceof String string) {
			return string;
		} else if(payload instanceof byte[] bytes) {
			return new String(bytes);
		} else if(payload instanceof InputStream stream) {
			try {
				// Convert line endings to \n to show them in the browser as real line feeds
				return StreamUtil.streamToString(stream,  "\n", false);
			} catch (IOException e) {
				throw new ApiException("unable to read response payload", e);
			}
		}
		throw new ApiException("unexpected response payload type ["+payload.getClass().getCanonicalName()+"]");
	}

	private ResponseEntity<?> testPipelineResponse(String payload) {
		return testPipelineResponse(payload, "SUCCESS", null);
	}
	private ResponseEntity<?> testPipelineResponse(String payload, String state, String message) {
		Map<String, Object> result = new HashMap<>();
		result.put("state", state);
		result.put("result", payload);
		if(message != null) {
			result.put("message", message);
		}
		return ResponseEntity.status(200).body(result);
	}

	// cannot call callAsyncGateway, backend calls are not synchronous
	private String processZipFile(InputStream file, RequestMessageBuilder builder) throws IOException {
		StringBuilder result = new StringBuilder();

		try(ZipInputStream archive = new ZipInputStream(file)) {
			ZipEntry zipEntry;
			while ((zipEntry = archive.getNextEntry()) != null) {
				String name = zipEntry.getName();
				String currentMessage = XmlEncodingUtils.readXml(StreamUtil.streamToBytes(StreamUtil.dontClose(archive)), null);

				builder.setPayload(currentMessage);
				Message<?> response = sendSyncMessage(builder);
				result.append(name);
				result.append(": ");
				result.append(BusMessageUtils.getHeader(response, MessageBase.STATE_KEY));
				result.append("\n");
			}
		}

		return result.toString();
	}

}
