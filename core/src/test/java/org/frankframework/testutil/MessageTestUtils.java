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
package org.frankframework.testutil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FilterInputStream;
import java.io.FilterReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

import org.frankframework.stream.FileMessage;
import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;
import org.frankframework.util.StreamUtil;

@SuppressWarnings("resource")
public class MessageTestUtils {
	public enum MessageType {
		CHARACTER_UTF8("/Util/MessageUtils/utf8-with-bom.txt"),
		CHARACTER_ISO88591("/Util/MessageUtils/iso-8859-1.txt"),
		BINARY("/Documents/doc001.pdf");

		private final URL url;
		MessageType(String resource) {
			URL url = TestFileUtils.getTestFileURL(resource);
			assertNotNull(url, "unable to find test file");
			this.url = url;
		}

		public Message getMessage() throws IOException {
			return MessageTestUtils.getMessage(url);
		}
	}

	public static Message getMessage(String resource) throws IOException {
		URL url = TestFileUtils.getTestFileURL(resource);
		assertNotNull(url, "unable to find test file");
		return getMessage(url);
	}

	public static Message getMessage(URL url) throws IOException {
		try {
			return new FileMessage(new File(url.toURI()));
		} catch (URISyntaxException e) {
			throw new IOException(e);
		}
	}

	public static Message getMessage(MessageType type) throws IOException {
		return type.getMessage();
	}

	public static Message getNonRepeatableMessage(MessageType type) throws IOException {
		Message message = type.getMessage();
		if(type == MessageType.BINARY) {
			return new Message(new FilterInputStream(message.asInputStream()) {}, message.getContext());
		}
		return new Message(new FilterReader(message.asReader(StreamUtil.AUTO_DETECT_CHARSET)) {}, message.getContext());
	}

	public static Message getBinaryMessage(String resource, boolean repeatable) throws IOException {
		Message message = getMessage(resource);
		if(!repeatable) {
			return new Message(new FilterInputStream(message.asInputStream()) {}, message.getContext());
		}
		return message;
	}

	public static Message getCharacterMessage(String resource, boolean repeatable) throws IOException {
		Message message = getMessage(resource);
		if(!repeatable) {
			return new Message(new FilterReader(message.asReader(StreamUtil.AUTO_DETECT_CHARSET)) {}, message.getContext());
		}
		return message;
	}

	public static Stream<Arguments> readFileInDifferentWays(String resource) throws IOException, URISyntaxException {
		URL testFileURL = TestFileUtils.getTestFileURL(resource);
		return Stream.of(
				Arguments.of(MessageTestUtils.getBinaryMessage(resource, false)), //InputStream
				Arguments.of(MessageTestUtils.getCharacterMessage(resource, false)), //Reader
				Arguments.of(new UrlMessage(testFileURL)), //Supplier
				Arguments.of(new FileMessage(new File(testFileURL.toURI()))) //SerializableFileReference
		);
	}
}
