/*
   Copyright 2021-2024 WeAreFrank!

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
package org.frankframework.ladybug;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Setter;
import nl.nn.testtool.MessageCapturerImpl;

import org.frankframework.ladybug.capture.OutputStreamCaptureWrapper;
import org.frankframework.ladybug.capture.WriterCaptureWrapper;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;

public class MessageCapturer extends MessageCapturerImpl {
	private final Logger log = LogUtil.getLogger(this);

	private @Setter @Autowired int maxMessageLength;

	@Override
	public StreamingType getStreamingType(Object message) {
		if (message instanceof Message m) {
			if (m.requiresStream() && !m.isRepeatable()) {
				return m.isBinary() ? StreamingType.BYTE_STREAM : StreamingType.CHARACTER_STREAM;
			}
		} else {
			if (message instanceof WriterPlaceHolder) {
				return StreamingType.CHARACTER_STREAM;
			}
		}
		return super.getStreamingType(message);
	}

	@Override
	public <T> T toWriter(T message, Writer writer, Consumer<Throwable> exceptionNotifier) {
		if (message instanceof Message message1) {
			try {
				message1.captureCharacterStream(new WriterCaptureWrapper(writer), maxMessageLength);
			} catch (Throwable t) {
				exceptionNotifier.accept(t);
				try {
					writer.close();
				} catch (IOException e) {
					log.error("Could not close writer", e);
				}
			}
			return message;
		}
		if (message instanceof WriterPlaceHolder writerPlaceHolder) {
			writerPlaceHolder.setWriter(writer);
			writerPlaceHolder.setSizeLimit(maxMessageLength);
			return message;
		}
		return super.toWriter(message, writer, exceptionNotifier);
	}

	@Override
	public <T> T toOutputStream(T message, OutputStream outputStream, Consumer<String> charsetNotifier, Consumer<Throwable> exceptionNotifier) {
		if (message instanceof Message m) {
			charsetNotifier.accept(m.getCharset());
			try {
				m.captureBinaryStream(new OutputStreamCaptureWrapper(outputStream), maxMessageLength);
			} catch (Throwable t) {
				exceptionNotifier.accept(t);
				try {
					outputStream.close();
				} catch (IOException e) {
					log.error("Could not close output stream", e);
				}
			}
			return message;
		}
		return super.toOutputStream(message, outputStream, charsetNotifier, exceptionNotifier);
	}
}
