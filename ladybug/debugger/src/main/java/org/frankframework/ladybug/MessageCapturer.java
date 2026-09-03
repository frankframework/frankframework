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

import java.io.Writer;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.wearefrank.ladybug.MessageCapturerImpl;

import lombok.Setter;

public class MessageCapturer extends MessageCapturerImpl {

	private @Setter @Autowired int maxMessageLength;

	@Override
	public StreamingType getStreamingType(Object message) {
		if (message instanceof WriterPlaceHolder) {
			return StreamingType.CHARACTER_STREAM;
		}

		return super.getStreamingType(message);
	}

	@Override
	public <T> T toWriter(T message, Writer writer, Consumer<Throwable> exceptionNotifier) {
		if (message instanceof WriterPlaceHolder writerPlaceHolder) {
			writerPlaceHolder.setWriter(writer);
			writerPlaceHolder.setSizeLimit(maxMessageLength);
			return message;
		}

		return super.toWriter(message, writer, exceptionNotifier);
	}
}
