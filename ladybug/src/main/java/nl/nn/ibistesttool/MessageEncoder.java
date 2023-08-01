/*
   Copyright 2021, 2022 WeAreFrank!

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.input.BoundedReader;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import nl.nn.adapterframework.stream.Message;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.MessageEncoderImpl;
import nl.nn.testtool.TestTool;

public class MessageEncoder extends MessageEncoderImpl {

	private @Setter @Getter TestTool testTool;

	@Override
	public ToStringResult toString(Object message, String charset) {
		if (message instanceof Message) {
			Message m = ((Message)message);
			if (charset==null) {
				charset = m.getCharset();
			}
			if (m.requiresStream()) {
				if (m.isRepeatable()) {
					if (m.isBinary()) {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						try (InputStream inputStream = m.asInputStream()) {
							IOUtils.copy(new BoundedInputStream(inputStream, testTool.getMaxMessageLength()), baos, testTool.getMaxMessageLength());
							ToStringResult result = super.toString(baos.toByteArray(), charset);
							result.setMessageClassName(m.getObjectId());
							return result;
						} catch (IOException e) {
							return super.toString(e, null);
						}
					}
					StringWriter writer = new StringWriter();
					try (Reader reader = m.asReader()){
						IOUtils.copy(new BoundedReader(reader, testTool.getMaxMessageLength()), writer);
					} catch (IOException e) {
						return super.toString(e, null);
					}
					return new ToStringResult(writer.toString(), null, m.getObjectId());
				}
				return new ToStringResult(WAITING_FOR_STREAM_MESSAGE, null, m.getObjectId());
			}
			ToStringResult r = super.toString(m.asObject(), charset);
			r.setMessageClassName(m.getObjectId());
			return r;
		}
		if (message instanceof WriterPlaceHolder) {
			return new ToStringResult(WAITING_FOR_STREAM_MESSAGE, null, "request to provide outputstream");
		}
		return super.toString(message, charset);
	}

	@Override
	public Object toObject(Checkpoint checkpoint) {
		return Message.asMessage(super.toObject(checkpoint));
	}

	@Override
	@SneakyThrows
	public <T> T toObject(Checkpoint originalCheckpoint, T messageToStub) {
		if (messageToStub instanceof Message) {
			// In case a stream is stubbed the replaced stream needs to be closed as next pipe will read and close the
			// stub which would leave the replaced stream unclosed
			((Message)messageToStub).close();
		}
		return (T) Message.asMessage(super.toObject(originalCheckpoint, messageToStub));
	}

}
