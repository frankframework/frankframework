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

import org.apache.commons.lang3.StringUtils;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.web.Description;
import org.frankframework.management.web.Relation;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;

@RestController
public class FileViewer extends FrankApiBase {

	@GetMapping(value = "/file-viewer", produces = {"text/html", "text/plain", "application/xml", "application/zip", "application/octet-stream"})
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("logging")
	@Description("view or download a (log)file")
	public ResponseEntity<?> getFileContent(@RequestParam("file") String file, @RequestParam(value = "accept", required = false) String acceptParam, @RequestHeader("Accept") String acceptHeader) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.FILE_VIEWER, BusAction.GET);
		if (StringUtils.isEmpty(acceptHeader)) acceptHeader = "*/*";
		String acceptType = !StringUtils.isEmpty(acceptParam) ? acceptParam : acceptHeader.split(",")[0];
		String wantedType = MediaType.valueOf(acceptType).getSubtype();
		builder.addHeader("fileName", file);
		builder.addHeader("resultType", wantedType);

//		if ("html".equalsIgnoreCase(wantedType)) {
//			return processHtmlMessage(builder);
//		}
		return callSyncGateway(builder);
	}

	/*private ResponseEntity<StreamingResponseBody> processHtmlMessage(RequestMessageBuilder builder) {
		Message<?> fileContentsMessage = sendSyncMessage(builder);
		StreamingResponseBody stream = outputStream -> {
			BufferedReader fileContentsReader = new BufferedReader(new InputStreamReader((InputStream) fileContentsMessage.getPayload()));
			String line;
			while ((line = fileContentsReader.readLine()) != null) {
				String formattedLine = StringUtils.replace(line, "\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
				outputStream.write((formattedLine + "<br>").getBytes());
			}
			outputStream.close();
		};
//		For testing purposes use this
		*//*StreamingResponseBody stream = out -> {
			String msg = "/srb" + " @ " + new Date();
			out.write(msg.getBytes());
		};
		return ResponseEntity.ok(stream);*//*
		return ResponseUtils.convertToSpringResponse(fileContentsMessage, stream);
	}*/

}
