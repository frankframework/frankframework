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


import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BinaryResponseMessage;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.ResponseMessageBase;
import org.frankframework.management.bus.StringResponseMessage;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.util.AppConstants;
import org.frankframework.util.FileUtils;
import org.frankframework.util.XmlEncodingUtils;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.FILE_VIEWER)
public class FileViewer extends BusEndpointBase {

	public static final String fvConfigKey = "FileViewerServlet.signal";
	public static final String permissionRules = AppConstants.getInstance().getProperty("FileViewerServlet.permission.rules");

	public static String makeConfiguredReplacements(String input) {
		for (final String signal : AppConstants.getInstance().getListProperty(fvConfigKey)) {
			String pre = AppConstants.getInstance().getProperty(fvConfigKey + "." + signal + ".pre");
			String post = AppConstants.getInstance().getProperty(fvConfigKey + "." + signal + ".post");
			input = StringUtils.replace(input, signal, pre + signal + post);
		}
		return StringUtils.replace(input, "\t", "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
	}

	@ActionSelector(BusAction.GET)
	public Message<?> getFileContent(Message<?> message) {
		String type = BusMessageUtils.getHeader(message, "resultType");
		String fileName = BusMessageUtils.getHeader(message, "fileName");
		ResponseMessageBase<?> response;

		if (fileName == null || type == null) {
			throw new BusException("fileName or type not specified");
		} else if (!FileUtils.readAllowed(permissionRules, fileName, BusMessageUtils::hasRole)){
			throw new BusException("not allowed");
		}

		try {
			if (type.equalsIgnoreCase("html") || type.equalsIgnoreCase("xml")) {
				response = getInlineContents(fileName, type);
			} else {
				response = getInputStreamContents(fileName, type);
			}
		} catch (IOException e) {
			throw new BusException("FileViewer caught IOException", e);
		}
		return response;
	}

	private static BinaryResponseMessage getInputStreamContents(String filepath, String type) throws IOException {
		InputStream inputStream = new FileInputStream(filepath);
		String filename = FilenameUtils.getName(filepath);
		BinaryResponseMessage response;
		if (type.equalsIgnoreCase("zip")) {
			response = new BinaryResponseMessage(inputStream, MediaType.valueOf("application/zip"));
		} else if (type.equalsIgnoreCase("text")) {
			response = new BinaryResponseMessage(inputStream, MediaType.TEXT_PLAIN);
		} else {
			response = new BinaryResponseMessage(inputStream, MediaType.APPLICATION_OCTET_STREAM);
		}
		response.setFilename(filename);
		return response;
	}

	private static ResponseMessageBase<?> getInlineContents(String filepath, String type) throws IOException {
		if (type.equalsIgnoreCase("html")) {
			StringBuilder htmlResponse = new StringBuilder(); // Temp solution
			FileReader reader = new FileReader(filepath);
			LineNumberReader lineNumber = new LineNumberReader(reader);

			String line;
			while ((line = lineNumber.readLine()) != null) {
				htmlResponse.append(makeConfiguredReplacements(XmlEncodingUtils.encodeChars(line))).append("<br/>");
			}
			return new StringResponseMessage(htmlResponse.toString(), MediaType.TEXT_HTML);
		}

		String filename = FilenameUtils.getName(filepath);
		File file = new File(filepath);
		InputStream fileStream = new FileInputStream(file);
		BinaryResponseMessage response = new BinaryResponseMessage(fileStream, MediaType.APPLICATION_XML);
		response.setFilename("inline", filename);
		return response;
	}

}
