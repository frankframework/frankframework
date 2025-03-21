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
import java.io.InputStream;
import java.util.HashMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.frankframework.console.AllowAllIbisUserRoles;
import org.frankframework.console.ApiException;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.console.util.RequestUtils;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.util.StreamUtil;

@RestController
public class LiquibaseScript {

	private final FrankApiService frankApiService;

	public LiquibaseScript(FrankApiService frankApiService) {
		this.frankApiService = frankApiService;
	}

	@AllowAllIbisUserRoles
	@GetMapping(value = "/jdbc/liquibase", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<?> downloadScript(@RequestParam(value = "configuration", required = false) String configuration) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.JDBC_MIGRATION, BusAction.DOWNLOAD);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
		return frankApiService.callSyncGateway(builder);
	}

	@AllowAllIbisUserRoles
	@PostMapping(value = "/jdbc/liquibase", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> generateSQL(GenerateSQLModel model) throws ApiException {
		String configuration = RequestUtils.resolveRequiredProperty("configuration", model.configuration(), null);
		MultipartFile filePart = RequestUtils.resolveRequiredProperty("file", model.file(), null);

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.JDBC_MIGRATION, BusAction.UPLOAD);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);

		String filenameOrPath = filePart.getOriginalFilename();
		String filename = FilenameUtils.getName(filenameOrPath);
		try {
			InputStream file = filePart.getInputStream();
			String payload = StreamUtil.streamToString(file);
			builder.setPayload(payload);
		} catch (IOException e) {
			throw new ApiException("unable to read payload", e);
		}

		if (StringUtils.endsWithIgnoreCase(filename, ".zip")) {
			throw new ApiException("uploading zip files is not supported!");
		}
		builder.addHeader("filename", filename);
		Message<?> response = frankApiService.sendSyncMessage(builder);
		String result = (String) response.getPayload();

		if (StringUtils.isEmpty(result)) {
			throw new ApiException("Make sure liquibase xml script exists for configuration [" + configuration + "]");
		}

		HashMap<String, Object> resultMap = new HashMap<>();
		resultMap.put("result", result);

		return ResponseEntity.status(HttpStatus.CREATED).body(resultMap);
	}

	public record GenerateSQLModel(
			String configuration,
			MultipartFile file) {
	}
}
