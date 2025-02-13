/*
   Copyright 2023 WeAreFrank!

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
package org.frankframework.compression;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.Logger;

import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.util.LogUtil;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.StreamUtil;

/**
 * Message will be closed after it's been read.
 */
public class MessageZipEntry extends ZipEntry {
	private final Logger log = LogUtil.getLogger(MessageZipEntry.class);
	private final Message message;

	public MessageZipEntry(Message message) {
		this(message, String.valueOf(message.getContext().get(MessageContext.METADATA_NAME)));
	}

	public MessageZipEntry(Message message, String filename) {
		super(filename);

		if (message!=null) {
			this.message = message;

			setSize(message.size());
		} else {
			this.message = Message.nullMessage();
			log.warn("contents of zip entry [{}] is null", filename);
		}
	}

	@Override
	public void setSize(long size) {
		if(size > 0) {
			super.setSize(size);
		}
	}

	/** Doesn't do anything if the size was already known. Computes if unknown. */
	public void computeSize() {
		setSize(MessageUtils.computeSize(message));
	}

	public void computeCrc() {
		Long crc32 = MessageUtils.generateCRC32(message);
		if(crc32 != null) {
			setCrc(crc32);
		}
	}

	public void computeFileHeaders() {
		setMethod(ZipEntry.STORED);
		computeCrc();
		computeSize();
	}

	/**
	 * Note: this consumes the {@link Message}.
	 */
	public void writeTo(OutputStream outputStream) throws IOException {
		try (message; InputStream is = message.asInputStream()) {
			StreamUtil.streamToStream(is, outputStream);
		}
	}

	public void writeEntry(ZipOutputStream outputStream) throws IOException {
		outputStream.putNextEntry(this);
		writeTo(outputStream);
		outputStream.closeEntry();
	}

	@Override
	public String toString() {
		return "ZipEntry ["+getName()+"] for [" + message.getRequestClass() + "]";
	}
}
