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
import java.io.UnsupportedEncodingException;

import jakarta.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.frankframework.console.AllowAllIbisUserRoles;
import org.frankframework.console.ApiException;
import org.frankframework.console.Description;
import org.frankframework.console.Relation;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.console.util.RequestUtils;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.XmlEncodingUtils;

@RestController
public class TestServiceListener {

	private final FrankApiService frankApiService;

	public TestServiceListener(FrankApiService frankApiService) {
		this.frankApiService = frankApiService;
	}

	@AllowAllIbisUserRoles
	@Relation("testing")
	@Description("view a list of all available service-listeners")
	@GetMapping(value = "/test-servicelistener", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getServiceListeners() throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.SERVICE_LISTENER, BusAction.GET);
		return frankApiService.callSyncGateway(builder);
	}

	@RolesAllowed("IbisTester")
	@Relation("testing")
	@Description("send a message to a service listeners, triggering an adapter to process the message")
	@PostMapping(value = "/test-servicelistener", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> postServiceListener(TestServiceListenerModel model) throws ApiException {
		String message;

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.SERVICE_LISTENER, BusAction.UPLOAD);
		builder.addHeader("service", RequestUtils.resolveRequiredProperty("service", model.service(), null));
		String fileEncoding = RequestUtils.resolveRequiredProperty("encoding", model.encoding(), StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		MultipartFile filePart = model.file();

		if (filePart != null) {
			try {
				InputStream file = filePart.getInputStream();
				message = XmlEncodingUtils.readXml(file, fileEncoding);
			} catch (UnsupportedEncodingException e) {
				throw new ApiException("unsupported file encoding [" + fileEncoding + "]");
			} catch (IOException e) {
				throw new ApiException("error reading file", e);
			}
		} else {
			message = RequestUtils.resolveStringWithEncoding("message", model.message(), fileEncoding, true);
		}

		if (StringUtils.isEmpty(message)) {
			throw new ApiException("Neither a file nor a message was supplied", 400);
		}

		builder.setPayload(message);
		return frankApiService.callSyncGateway(builder);
	}

	public record TestServiceListenerModel(
			String service,
			String encoding,
			MultipartFile file,
			MultipartFile message) {
	}
}
