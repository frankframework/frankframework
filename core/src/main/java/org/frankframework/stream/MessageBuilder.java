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
import java.nio.file.Files;
import java.nio.file.Path;

import org.frankframework.util.AppConstants;
import org.frankframework.util.FileUtils;
import org.frankframework.xml.XmlWriter;


public class MessageBuilder {
	private static final long MAX_IN_MEMORY_SIZE = AppConstants.getInstance().getLong(Message.MESSAGE_MAX_IN_MEMORY_PROPERTY, Message.MESSAGE_MAX_IN_MEMORY_DEFAULT);

	private final OutputStream outputStream;
	private Path location;

	/**
	 * Stores the message in the {@code temp-messages} folder.
	 * Attempts to store the result in memory and automatically overflows to disk.
	 */
	public MessageBuilder() throws IOException {
		int maxBufferSize = Math.toIntExact(MAX_IN_MEMORY_SIZE);
		Path tempdir = FileUtils.getTempDirectory(SerializableFileReference.TEMP_MESSAGE_DIRECTORY).toPath();
		outputStream = new OverflowToDiskOutputStream(maxBufferSize, tempdir);
	}

	/**
	 * Directly stores to disk.
	 * Mainly for legacy implementations
	 */
	public MessageBuilder(Path file) throws IOException {
		if(!Files.exists(file)) throw new IOException("file ["+file+"] does not exist");
		if(Files.isDirectory(file)) throw new IOException("location ["+file+"] may not be a folder");

		this.location = file;
		outputStream = Files.newOutputStream(location);
	}

	public XmlWriter asXmlWriter() {
		return new XmlWriter(asOutputStream(), true);
	}

	public OutputStream asOutputStream() {
		return outputStream;
	}

	/**
	 * SFR will be removed upon close.
	 * @return {@link SerializableFileReference} as {@link PathMessage}. Repeatable.
	 */
	public Message build() {
		if(outputStream instanceof OverflowToDiskOutputStream odo) {
			return odo.toMessage();
		}
		return new PathMessage(location);
	}
}
