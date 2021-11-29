/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.ibistesttool;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.testtool.TestTool;

public class MessageCapturer implements nl.nn.testtool.MessageCapturer {
	protected Logger log = LogUtil.getLogger(this);
	
	private @Setter @Getter TestTool testTool;

	@Override
	public StreamingType getStreamingType(Object message) {
		if (message instanceof Message) {
			Message m = (Message)message;
			if (m.requiresStream() && !m.isRepeatable()) {
				return m.isBinary() ? StreamingType.BYTE_STREAM : StreamingType.CHARACTER_STREAM;
			}
		} else {
			if (message instanceof WriterPlaceHolder) {
				return StreamingType.CHARACTER_STREAM;
			}
		}
		return StreamingType.NONE;
	}

	// do not set override annotation, to avoid Ladybug compatibility problems
	public <T> T toWriter(T message, Writer writer) {
		return toWriter(message, writer, null);
	}

	// do not set override annotation, to avoid Ladybug compatibility problems
	public <T> T toWriter(T message, Writer writer, Consumer<Throwable> exceptionNotifier) {
		if (message instanceof Message) {
			try {
				((Message)message).captureCharacterStream(writer, testTool.getMaxMessageLength());
			} catch (Exception e) {
				if (exceptionNotifier!=null) {
					exceptionNotifier.accept(e);
				}
				log.warn("Could not capture characterstream", e);
				try {
					writer.close();
				} catch (IOException e1) {
					log.error("Could not close writer", e1);
				}
			}
		} 
		if (message instanceof WriterPlaceHolder) {
			WriterPlaceHolder writerPlaceHolder = (WriterPlaceHolder)message;
			writerPlaceHolder.setWriter(writer);
			writerPlaceHolder.setSizeLimit(testTool.getMaxMessageLength());
		}
		return message;
	}

	// do not set override annotation, to avoid Ladybug compatibility problems
	public <T> T toOutputStream(T message, OutputStream outputStream) {
		return toOutputStream(message, outputStream, null, null);
	}

	// do not set override annotation, to avoid Ladybug compatibility problems
	public <T> T toOutputStream(T message, OutputStream outputStream, Consumer<String> charsetNotifier) {
		return toOutputStream(message, outputStream, charsetNotifier, null);
	}

	// do not set override annotation, to avoid Ladybug compatibility problems
	public <T> T toOutputStream(T message, OutputStream outputStream, Consumer<String> charsetNotifier, Consumer<Throwable> exceptionNotifier) {
		if (message instanceof Message) {
			Message m = (Message)message;
			if (charsetNotifier!=null) {
				charsetNotifier.accept(m.getCharset());
			}
			try {
				((Message)message).captureBinaryStream(outputStream, testTool.getMaxMessageLength());
			} catch (Exception e) {
				if (exceptionNotifier!=null) {
					exceptionNotifier.accept(e);
				}
				log.warn("Could not capture binary stream", e);
				try {
					outputStream.close();
				} catch (IOException e1) {
					log.error("Could not close output stream", e1);
				}
			}
		}
		return message;
	}


}
