/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.larva;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Properties;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.io.FilenameUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.larva.output.LarvaWriter;
import org.frankframework.stream.FileMessage;
import org.frankframework.stream.Message;
import org.frankframework.util.StreamUtil;

@Log4j2
public class LarvaUtil {

	private LarvaUtil() {
		// Private constructor to prevent creating instances
	}

	public static String getAbsolutePath(String parent, String child) {
		return getAbsolutePath(parent, child, false);
	}

	/**
	 * Returns the absolute pathname for the child pathname. The parent pathname
	 * is used as a prefix when the child pathname is an not absolute.
	 *
	 * @param parent  the parent pathname to use
	 * @param child   the child pathname to convert to a absolute pathname
	 */
	public static String getAbsolutePath(String parent, String child, boolean addFileSeparator) {
		File result;
		File file = new File(child);
		if (file.isAbsolute()) {
			result = file;
		} else {
			result = new File(parent, child);
		}
		String absPath = FilenameUtils.normalize(result.getAbsolutePath());
		if (addFileSeparator) {
			return absPath + File.separator;
		} else {
			return absPath;
		}
	}

	public static String formatDuration(long durationInMs) {
		Duration duration = Duration.ofMillis(durationInMs);
		if (duration.toMinutesPart() == 0 && duration.toSecondsPart() == 0) {
			// Only milliseconds (e.g. 123ms)
			return duration.toMillisPart() + "ms";
		} else if (duration.toMinutesPart() == 0) {
			// Seconds and milliseconds (e.g. 1s 123ms)
			return duration.toSecondsPart() + "s " + duration.toMillisPart() + "ms";
		} else {
			// Minutes, seconds and milliseconds (e.g. 1m 1s 123ms)
			return duration.toMinutesPart() + "m " + duration.toSecondsPart() + "s " + duration.toMillisPart() + "ms";
		}
	}

	public static @Nullable String getParentOfWebappRoot() {
		URL rootResource = LarvaUtil.class.getResource("/");
		if (rootResource == null) {
			return null;
		}
		String realPath = rootResource.getPath();
		return new File(realPath).getParent();
	}

	protected static @Nonnull Properties readProperties(LarvaWriter out, File propertyFile) {
		Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(propertyFile.toPath()); Reader reader = StreamUtil.getCharsetDetectingInputStreamReader(in)) {
			properties.load(reader);
		} catch (IOException e) {
			out.errorMessage("Cannot read property file: " + propertyFile.getAbsolutePath(), e);
		}
		return properties;
	}

	public static Message readFile(@Nonnull String fileName) throws IOException {
		String encoding;
		if (fileName.endsWith(".xml") || fileName.endsWith(".wsdl")) {
			encoding = parseEncodingFromXml(fileName);
		} else if (fileName.endsWith(".utf8") || fileName.endsWith(".json")) {
			encoding = "UTF-8";
		} else if (fileName.endsWith(".ISO-8859-1")) {
			encoding = "ISO-8859-1";
		} else {
			encoding = null;
		}
		return new FileMessage(new File(fileName), encoding);
	}

	private static @Nullable String parseEncodingFromXml(@Nonnull String fileName) throws IOException {
		// Determine the encoding the XML way but don't use an XML parser to
		// read the file and transform it to a string to prevent changes in
		// formatting and prevent adding a xml declaration where this is
		// not present in the file. For example, when using a
		// WebServiceSender to send a message to a WebServiceListener the
		// xml message must not contain a xml declaration.
		try (InputStream in = new FileInputStream(fileName)) {
			XMLInputFactory factory = XMLInputFactory.newInstance();
			factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
			factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);

			XMLStreamReader parser = factory.createXMLStreamReader(in);
			String encoding = parser.getEncoding();
			parser.close();
			return encoding;
		} catch (XMLStreamException e) {
			throw new IOException("Could not determine encoding for file [" + fileName + "]", e);
		}
	}
}
