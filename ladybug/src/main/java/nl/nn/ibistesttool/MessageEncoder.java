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

import lombok.SneakyThrows;
import nl.nn.adapterframework.stream.Message;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.MessageEncoderImpl;

public class MessageEncoder extends MessageEncoderImpl {

	@Override
	public ToStringResult toString(Object message, String charset) {
		if (message instanceof Message) {
			// Hide/remove the Message class/object
			Message m = ((Message)message);
			ToStringResult toStringResult;
			if (m.requiresStream()) {
				toStringResult = new ToStringResult(WAITING_FOR_STREAM_MESSAGE, null,
						m.asObject().getClass().getTypeName());
			} else {
				toStringResult = super.toString(m.asObject(), charset);
			}
			return toStringResult;
		} else {
			return super.toString(message, charset);
		}
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
