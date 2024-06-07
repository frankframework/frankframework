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
package org.frankframework.management.web;

import java.io.InputStream;

import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.util.ResponseUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
public class FileViewer extends FrankApiBase {

	@GetMapping(value = "/file-viewer", produces = {"text/html", "text/plain", "application/xml", "application/zip", "application/octet-stream"})
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("logging")
	@Description("view or download a (log)file")
	public ResponseEntity<StreamingResponseBody> getFileContent(@RequestParam("file") String file, @RequestParam(value = "accept", required = false) String acceptParam, @RequestHeader("Accept") String acceptHeader) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.FILE_VIEWER, BusAction.GET);

		if (StringUtils.isEmpty(acceptHeader)) acceptHeader = "*/*";
		String acceptType = !StringUtils.isEmpty(acceptParam) ? acceptParam : acceptHeader.split(",")[0];
		try {
			String wantedType = MediaType.valueOf(acceptType).getSubtype();
			builder.addHeader("fileName", file);
			builder.addHeader("resultType", wantedType);
		} catch (Exception e) {
			throw new ApiException("Invalid accept parameter or header: " + acceptHeader, e);
		}

		Message<InputStream> inputStreamMessage = (Message<InputStream>) sendSyncMessage(builder);
		return ResponseUtils.convertToSpringStreamingResponse(inputStreamMessage);
	}

}
