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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import org.frankframework.console.AllowAllIbisUserRoles;
import org.frankframework.console.Description;
import org.frankframework.console.Relation;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.console.util.ResponseUtils;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;

@RestController
public class FileViewer {

	private final FrankApiService frankApiService;

	public FileViewer(FrankApiService frankApiService) {
		this.frankApiService = frankApiService;
	}

	@GetMapping(value = "/file-viewer", produces = { "text/html", "text/plain", "application/xml", "application/zip", "application/octet-stream" })
	@AllowAllIbisUserRoles
	@Relation("logging")
	@Description("view or download a (log)file")
	public ResponseEntity<StreamingResponseBody> getFileContent(@RequestParam("file") String file, @RequestParam(value = "accept", required = false) String acceptParam, @RequestHeader("Accept") String acceptHeader) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.FILE_VIEWER, BusAction.GET);

		if (StringUtils.isEmpty(acceptHeader)) acceptHeader = "*/*";
		String acceptType = !StringUtils.isEmpty(acceptParam) ? acceptParam : acceptHeader.split(",")[0];
		String wantedType = MediaType.valueOf(acceptType).getSubtype();
		builder.addHeader("fileName", file);
		builder.addHeader("resultType", wantedType);

		var response = frankApiService.sendSyncMessage(builder);
		Message<InputStream> inputStreamMessage = getInputStreamMessage(response);
		return ResponseUtils.convertToSpringStreamingResponse(inputStreamMessage);
	}

	private static Message<InputStream> getInputStreamMessage(Message<?> response) {
		Message<InputStream> inputStreamMessage;
		if (response.getPayload() instanceof String payload) {
			InputStream inputStream = new ByteArrayInputStream(payload.getBytes(StandardCharsets.UTF_8));
			inputStreamMessage = new GenericMessage<>(inputStream, response.getHeaders());
		} else {
			inputStreamMessage = (Message<InputStream>) response;
		}
		return inputStreamMessage;
	}

}
