/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.testutil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FilterInputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import nl.nn.adapterframework.stream.FileMessage;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.StreamUtil;

public class MessageTestUtils {
	public enum MessageType {
		CHARACTER_UTF8("/Util/MessageUtils/utf8-with-bom.txt"),
		CHARACTER_ISO88591("/Util/MessageUtils/iso-8859-1.txt"),
		BINARY("/Documents/doc001.pdf");

		private URL url;
		private MessageType(String resource) {
			URL url = TestFileUtils.getTestFileURL(resource);
			assertNotNull(url, "unable to find test file");
			this.url = url;
		}

		public Message getMessage() throws IOException {
			try {
				return new FileMessage(new File(url.toURI()));
			} catch (URISyntaxException e) {
				throw new IOException(e);
			}
		}
	}

	public static Message getMessage(MessageType type) throws IOException {
		return type.getMessage();
	}

	public static Message getNonRepeatableMessage(MessageType type) throws IOException {
		Message message = type.getMessage();
		if(type.equals(MessageType.BINARY)) {
			return new Message(new FilterInputStream(message.asInputStream()) {}, message.getContext());
		}
		return new Message(new FilterReader(message.asReader(StreamUtil.AUTO_DETECT_CHARSET)) {}, message.getContext());
	}
}
