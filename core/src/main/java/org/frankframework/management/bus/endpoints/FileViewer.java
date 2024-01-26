package org.frankframework.management.bus.endpoints;


import org.apache.commons.io.FilenameUtils;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BinaryResponseMessage;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.JsonResponseMessage;
import org.frankframework.management.bus.ResponseMessageBase;
import org.frankframework.management.bus.StringResponseMessage;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.util.AppConstants;
import org.frankframework.util.FileUtils;
import org.frankframework.util.XmlEncodingUtils;
import org.springframework.messaging.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.HashMap;
import java.util.Map;

import static org.frankframework.webcontrol.FileViewerServlet.makeConfiguredReplacements;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.FILE_VIEWER)
public class FileViewer extends BusEndpointBase {

	public static final String permissionRules = AppConstants.getInstance().getProperty("FileViewerServlet.permission.rules");

	private static ResponseMessageBase<?> showReaderContents(String filepath, String type) throws IOException {
		if (type.equalsIgnoreCase("html")) {
			StringBuilder htmlResponse = new StringBuilder(); // Temp solution
			FileReader reader = new FileReader(filepath);
			LineNumberReader lineNumber = new LineNumberReader(reader);

			String line;
			while ((line = lineNumber.readLine()) != null) {
				htmlResponse.append(makeConfiguredReplacements(XmlEncodingUtils.encodeChars(line))).append("<br/>");
			}

			StringResponseMessage responseMessage = new StringResponseMessage(htmlResponse.toString());
			responseMessage.setHeader("Content-Type", "text/html");
			return responseMessage;
		}

		String filename = FilenameUtils.getName(filepath);
		File file = new File(filepath);
		FileInputStream fileStream = new FileInputStream(file);
		BinaryResponseMessage responseMessage = new BinaryResponseMessage(fileStream);
//		StringResponseMessage responseMessage = new StringResponseMessage(fileStream);

		if (type.equalsIgnoreCase("text")) {
			responseMessage.setHeader("Content-Type", "text/plain");
			responseMessage.setHeader("Content-Disposition","attachment; filename=\""+filename+"\""); //download
		} else if (type.equalsIgnoreCase("xml")) {
			responseMessage.setHeader("Content-Type", "application/xml");
			responseMessage.setHeader("Content-Disposition","inline; filename=\""+filename+"\""); //show in browser
		}

		return responseMessage;
	}

	@ActionSelector(BusAction.GET)
	public Message<?> getFileContent(Message<?> message) {
		String type = BusMessageUtils.getHeader(message, "resultType");
		String fileName = BusMessageUtils.getHeader(message, "fileName");

		if (fileName == null || type == null) {
			throw new BusException("fileName or type not specified");
		} else {
			if (!FileUtils.readAllowed(permissionRules, fileName, BusMessageUtils::hasRole)) {
				throw new BusException("not allowed");
			}
		}

		Map<String, String> debugResponse = new HashMap<>();
		debugResponse.put("fileName", fileName);
		debugResponse.put("type", type);
		ResponseMessageBase<?> response = new JsonResponseMessage(debugResponse);

		try {
			if (type.equalsIgnoreCase("zip") || type.equalsIgnoreCase("bin")) {
//				showInputStreamContents(fileName, type, response);
			} else {
				response = showReaderContents(fileName, type);
			}
		} catch (IOException e) {
			throw new BusException("FileViewer caught IOException", e);
		}

		return response;
	}

}
