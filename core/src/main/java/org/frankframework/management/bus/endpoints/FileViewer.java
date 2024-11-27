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
package org.frankframework.management.bus.endpoints;

import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.io.FilenameUtils;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.message.BinaryMessage;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.util.AppConstants;
import org.frankframework.util.FileUtils;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.FILE_VIEWER)
public class FileViewer extends BusEndpointBase {

	public static final String permissionRules = AppConstants.getInstance().getProperty("FileViewer.permission.rules");

	@ActionSelector(BusAction.GET)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<?> getFileContent(Message<?> message) {
		String resultType = BusMessageUtils.getHeader(message, "resultType");
		String fileName = BusMessageUtils.getHeader(message, "fileName");
		MessageBase<?> response;

		if (fileName == null || resultType == null) {
			throw new BusException("fileName or type not specified");
		} else if (!FileUtils.readAllowed(permissionRules, fileName, BusMessageUtils::hasRole)) {
			throw new BusException("not allowed", 403);
		}

		try {
			response = getFileContentsByType(fileName, resultType);
		} catch (IOException e) {
			throw new BusException("FileViewer caught IOException", e);
		}
		return response;
	}

	private static BinaryMessage getFileContentsByType(String filepath, String type) throws IOException {
		InputStream inputStream = new FileInputStream(filepath);
		String filename = FilenameUtils.getName(filepath);

		BinaryMessage response;
		switch (type.toLowerCase()) {
			case "html":
				response = new BinaryMessage(inputStream, MediaType.TEXT_HTML);
				response.setFilename("inline", filename);
				break;
			case "xml":
				response = new BinaryMessage(inputStream, MediaType.APPLICATION_XML);
				response.setFilename("inline", filename);
				break;
			case "plain":
				response = new BinaryMessage(inputStream, MediaType.TEXT_PLAIN);
				response.setFilename("inline", filename);
				break;
			case "zip":
				response = new BinaryMessage(inputStream, MediaType.valueOf("application/zip"));
				response.setFilename(filename);
				break;
			default:
				response = new BinaryMessage(inputStream, MediaType.APPLICATION_OCTET_STREAM);
				response.setFilename(filename);
		}
		return response;
	}
}
