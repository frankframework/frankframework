/*
   Copyright 2021-2026 WeAreFrank!

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
import java.io.PrintWriter;
import java.io.StringWriter;

import org.wearefrank.ladybug.Checkpoint;
import org.wearefrank.ladybug.MessageEncoderImpl;

import org.frankframework.stream.Message;
import org.frankframework.util.EnumUtils;

public class MessageEncoder extends MessageEncoderImpl {

	private final int maxMessageLength;

	public MessageEncoder(int maxMessageLength) {
		this.maxMessageLength = maxMessageLength;
	}

	@Override
	public ToStringResult toString(Object message, String charset) {
		if (message instanceof Message m) {
			if (m.isNull()) {
				return new ToStringResult(null, null, null);
			}
			try {
				final String type = m.isBinary() ? "UTF-8" : null;
				return new ToStringResult(m.peek(maxMessageLength), type, m.getRequestClass());
			} catch (IOException e) {
				StringWriter stringWriter = new StringWriter();
				e.printStackTrace(new PrintWriter(stringWriter));
				return new ToStringResult(stringWriter.toString(), THROWABLE_ENCODER);
			}
		} else if (message instanceof Boolean b) {
			return new ToStringResult(b.toString(), null, Boolean.class.getTypeName());
		} else if (message instanceof Enum<?> e) {
			return new ToStringResult(e.name(), null, e.getClass().getTypeName());
		}

		return super.toString(message, charset);
	}

	@Override
	public Object toObject(Checkpoint checkpoint) {
		return toObject(checkpoint, null);
	}

	/**
	 * @param originalCheckpoint the checkpoint from the original report that will be used as a stub for the
	 *                           counterpart checkpoint in the report in progress. The original checkpoint holds the
	 *                           string representation and the encoding method used when the original message was
	 *                           encoded and possible other relevant information to determine the original object type.
	 *                           It can be null when the original checkpoint cannot be found (in that case the decision
	 *                           to stub is not based on the original checkpoint but based on a stubbing strategy that
	 *                           stubs certain types of checkpoints). When null the default implementation
	 *                           {@link MessageEncoderImpl} will return the default stub message
	 *                           <p>
	 *                           {@link TestTool#DEFAULT_STUB_MESSAGE}
	 *
	 * @param messageToStub      The message in the report in progress that needs to be stubbed.
	 *                           Only used to determine the class' type.
	 * @param <T>                Unused
	 *
	 * @return                   In the case of a {@code Writer} of {@code OutputStream}, the stubbed message must be written to it, and it must be returned.
	 *                           Else the {@code originalCheckpoint.getMessage()} must be converted to {@code THIS} type and returned.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T toObject(Checkpoint originalCheckpoint, T messageToStub) {
		T stub = super.toObject(originalCheckpoint, messageToStub);

		if (messageToStub instanceof Enum<?> enumType) {
			// Probably shouldn't happen but...
			return (T) EnumUtils.parse(enumType.getDeclaringClass(), "" + stub);
		} else if (messageToStub instanceof Message) {
			// String encoding = originalCheckpoint.getEncoding();
			// If the type is Message, and it's encoded, assume the original was binary.
			// For now, it doesn't matter I suppose?

			// Checked
			return (T) Message.asMessage(stub);
		}

		// Unchecked unsafe...
		return stub;
	}

}
