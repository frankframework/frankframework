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
import java.io.Reader;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.input.BoundedReader;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.MessageEncoderImpl;
import nl.nn.testtool.TestTool;

public class MessageEncoder extends MessageEncoderImpl {
	private Logger log = LogUtil.getLogger(this);

	private @Setter @Getter TestTool testTool;

	@Override
	public ToStringResult toString(Object message, String charset) {
		if (message instanceof Message) {
			// Hide/remove the Message class/object
			Message m = ((Message)message);
			ToStringResult toStringResult;
			if (m.requiresStream()) {
				if (m.isRepeatable()) {
					StringWriter writer = new StringWriter();
					if (m.isBinary()) {
						try (InputStream inputStream = m.asInputStream()) {
							String charsetToUse = StringUtils.isNotEmpty(m.getCharset()) ? m.getCharset() : StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
							IOUtils.copy(new BoundedInputStream(inputStream, testTool.getMaxMessageLength()), new WriterOutputStream(writer, charsetToUse), testTool.getMaxMessageLength());
						} catch (IOException e) {
							log.warn("Could not capture message", e);
						}
					} else {
						try (Reader reader = m.asReader()){
							IOUtils.copy(new BoundedReader(reader, testTool.getMaxMessageLength()), writer);
						} catch (IOException e) {
							log.warn("Could not capture message", e);
						}
					}
					toStringResult = new ToStringResult(writer.toString(), charset, m.asObject().getClass().getTypeName());
							
				} else {
					toStringResult = new ToStringResult(WAITING_FOR_STREAM_MESSAGE, null, m.asObject().getClass().getTypeName());
				}
			} else {
				toStringResult = super.toString(m.asObject(), charset);
			}
			return toStringResult;
		} 
		return super.toString(message, charset);
	}

	@Override
	public Object toObject(Checkpoint checkpoint) {
		return Message.asMessage(super.toObject(checkpoint));
	}

	@Override
	@SneakyThrows
	@SuppressWarnings("unchecked")
	public <T> T toObject(Checkpoint originalCheckpoint, T messageToStub) {
		if (messageToStub instanceof Message) {
			// In case a stream is stubbed the replaced stream needs to be closed as next pipe will read and close the
			// stub which would leave the replaced stream unclosed
			Object object = ((Message)messageToStub).asObject();
			if (object instanceof AutoCloseable) {
				((AutoCloseable)object).close();
			}
		}
		return (T)toObject(originalCheckpoint);
	}

}
