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
package org.frankframework.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.output.XmlStreamWriter;
import org.frankframework.documentbuilder.json.JsonWriter;
import org.frankframework.util.AppConstants;
import org.frankframework.util.TemporaryDirectoryUtils;
import org.frankframework.xml.XmlWriter;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

@Log4j2
public class MessageBuilder {
	private static final long MAX_IN_MEMORY_SIZE = AppConstants.getInstance().getLong(Message.MESSAGE_MAX_IN_MEMORY_PROPERTY, Message.MESSAGE_MAX_IN_MEMORY_DEFAULT);

	private final OutputStream outputStream;
	private Path location;
	private @Setter MimeType mimeType;

	/**
	 * Stores the message in the {@code temp-messages} folder.
	 * Attempts to store the result in memory and automatically overflows to disk.
	 */
	public MessageBuilder() throws IOException {
		int maxBufferSize = Math.toIntExact(MAX_IN_MEMORY_SIZE);
		Path tempdir = TemporaryDirectoryUtils.getTempDirectory(SerializableFileReference.TEMP_MESSAGE_DIRECTORY);
		outputStream = new OverflowToDiskOutputStream(maxBufferSize, tempdir);
	}

	/**
	 * Directly stores to disk.
	 * Mainly for legacy implementations which allows the user to choose where to save the file.
	 */
	public MessageBuilder(Path file) throws IOException {
		if(Files.isDirectory(file)) throw new IOException("location ["+file+"] may not be a folder");

		if(Files.exists(file)) {
			log.info("location [{}] already exists, overwriting file-contents", file);
		}

		location = file;
		outputStream = Files.newOutputStream(location);
	}

	public Writer asWriter() {
		return new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
	}

	public XmlWriter asXmlWriter() {
		mimeType = MediaType.APPLICATION_XML;
		try {
			Writer xmlWriter = XmlStreamWriter.builder().setOutputStream(asOutputStream()).get();
			return new XmlWriter(xmlWriter, true);
		} catch (IOException e) {
			// This really should only happen if somebody from Apache Commons dun-goofed...
			// Converting an OutputStream to an OutputStream cannot trigger an IOException...
			throw new IllegalStateException("unable to create XmlWriter", e);
		}
	}

	public JsonWriter asJsonWriter() {
		mimeType = MediaType.APPLICATION_JSON;
		return new JsonWriter(asWriter(), true);
	}

	public OutputStream asOutputStream() {
		return outputStream;
	}

	/**
	 * SFR will be removed upon close.
	 * @return {@link SerializableFileReference} as {@link PathMessage}. Repeatable.
	 */
	public Message build() {
		final Message result;
		if(outputStream instanceof OverflowToDiskOutputStream odo) {
			result = odo.toMessage();
		} else {
			result = new PathMessage(location);
		}

		if(mimeType != null) {
			result.getContext().withMimeType(mimeType);
		}

		return result;
	}
}
