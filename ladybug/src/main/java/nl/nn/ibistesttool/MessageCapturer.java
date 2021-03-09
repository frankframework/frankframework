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

import java.io.OutputStream;
import java.io.Writer;
import java.util.function.Consumer;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.testtool.TestTool;

public class MessageCapturer implements nl.nn.testtool.MessageCapturer {
	private @Setter @Getter TestTool testTool;

	@Override
	public StreamingType getStreamingType(Object message) {
		if (message instanceof Message) {
			Message m = (Message)message;
			if (m.requiresStream()) {
				return m.isBinary() ? StreamingType.BYTE_STREAM : StreamingType.CHARACTER_STREAM;
			}
		} else {
			if (message instanceof MessageOutputStream) {
				return ((MessageOutputStream)message).isBinary() ? StreamingType.BYTE_STREAM : StreamingType.CHARACTER_STREAM;
			}
		}
		return StreamingType.NONE;
	}

	@Override
	public <T> T toWriter(T message, Writer writer) {
		if (message instanceof Message) {
			((Message)message).captureCharacterStream(writer, testTool.getMaxMessageLength());
		} else {
			if (message instanceof MessageOutputStream) {
				((MessageOutputStream)message).captureCharacterStream(writer, testTool.getMaxMessageLength());
			}
		}
		return message;
	}

	@Override
	public <T> T toOutputStream(T message, OutputStream outputStream, Consumer<String> charsetNotifier) {
		if (message instanceof Message) {
			charsetNotifier.accept(((Message)message).getCharset());
			((Message)message).captureBinaryStream(outputStream, testTool.getMaxMessageLength());
		} else {
			if (message instanceof MessageOutputStream) {
				((MessageOutputStream)message).captureBinaryStream(outputStream, testTool.getMaxMessageLength());
			}
		}
		return message;
	}

}
