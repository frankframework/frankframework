/*
   Copyright 2021-2025 WeAreFrank!

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

import java.io.OutputStream;
import java.io.Writer;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;

import lombok.Setter;
import nl.nn.testtool.MessageCapturerImpl;

import org.frankframework.stream.Message;
import org.frankframework.util.CloseUtils;

public class MessageCapturer extends MessageCapturerImpl {

	private @Setter @Autowired int maxMessageLength;

	@Override
	public StreamingType getStreamingType(Object message) {
		if (message instanceof Message m) {
			if (m.requiresStream()) {
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
				// TODO: Since messages are now preserved, should instantly write message to capture-writer
				// should call StreamCaptureUtils.captureReader directly...
				message1.captureCharacterStream(writer, maxMessageLength);
			} catch (Throwable t) {
				exceptionNotifier.accept(t);
				CloseUtils.closeSilently(writer);
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
				// TODO: Since messages are now preserved, should instantly write message to capture-outputstream
				m.captureBinaryStream(outputStream, maxMessageLength);
			} catch (Throwable t) {
				exceptionNotifier.accept(t);
				CloseUtils.closeSilently(outputStream);
			}
			return message;
		}
		return super.toOutputStream(message, outputStream, charsetNotifier, exceptionNotifier);
	}
}
