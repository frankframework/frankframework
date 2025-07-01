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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.input.BoundedReader;
import org.springframework.beans.factory.annotation.Autowired;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.MessageEncoderImpl;

import org.frankframework.stream.Message;

public class MessageEncoder extends MessageEncoderImpl {

	private @Autowired int maxMessageLength;

	@Override
	public ToStringResult toString(Object message, String charset) {
		if (message instanceof Message m) {
			if (charset==null) {
				charset = m.getCharset();
			}
			if (m.requiresStream()) {
				if (m.isBinary()) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					try (InputStream inputStream = m.asInputStream()) {
						IOUtils.copy(new BoundedInputStream(inputStream, maxMessageLength), baos, maxMessageLength);
						ToStringResult result = super.toString(baos.toByteArray(), charset);
						result.setMessageClassName(m.getObjectId());
						return result;
					} catch (IOException e) {
						return super.toString(e, null);
					}
				}
				StringWriter writer = new StringWriter();
				try (Reader reader = m.asReader()){
					IOUtils.copy(new BoundedReader(reader, maxMessageLength), writer);
				} catch (IOException e) {
					return super.toString(e, null);
				}
				return new ToStringResult(writer.toString(), null, m.getObjectId());
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
	public <T> T toObject(Checkpoint originalCheckpoint, T messageToStub) {
		if (messageToStub instanceof Message message) {
			// In case a stream is stubbed the replaced stream needs to be closed as next pipe will read and close the
			// stub which would leave the replaced stream unclosed
			message.close();
		}
		return (T) Message.asMessage(super.toObject(originalCheckpoint, messageToStub));
	}

}
