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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.function.Consumer;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.input.BoundedReader;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.testtool.TestTool;

public class MessageCapturer implements nl.nn.testtool.MessageCapturer {
	private Logger log = LogUtil.getLogger(this);
	
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
			Message m = (Message)message;
			if (m.isRepeatable()) {
				// if the message is repeatable, then avoid losing its repeatability by wrapping it in a non repeatable Reader
				try (Reader reader = m.asReader()){
					IOUtils.copy(new BoundedReader(reader, testTool.getMaxMessageLength()), writer);
				} catch (IOException e) {
					log.warn("Could not capture message", e);
				}
			} else {
				m.captureCharacterStream(writer, testTool.getMaxMessageLength());
			}
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
			Message m = (Message)message;
			charsetNotifier.accept(m.getCharset());
			if (m.isRepeatable()) {
				// if the message is repeatable, then avoid losing its repeatability by wrapping it in a non repeatable InputStream
				try (InputStream inputStream = m.asInputStream()) {
					IOUtils.copy(new BoundedInputStream(inputStream, testTool.getMaxMessageLength()), outputStream, testTool.getMaxMessageLength());
				} catch (IOException e) {
					log.warn("Could not capture message", e);
				}
			} else {
				((Message)message).captureBinaryStream(outputStream, testTool.getMaxMessageLength());
			}
		} else {
			if (message instanceof MessageOutputStream) {
				((MessageOutputStream)message).captureBinaryStream(outputStream, testTool.getMaxMessageLength());
			}
		}
		return message;
	}

}
