/*
   Copyright 2019, 2020 WeAreFrank!

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
package org.frankframework.stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.transform.Result;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.frankframework.core.IForwardTarget;
import org.frankframework.stream.json.JsonUtils;
import org.frankframework.util.XmlUtils;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;

@TestMethodOrder(MethodName.class)
public class MessageOutputStreamTest {

	private final boolean TEST_CDATA = true;
	private final String CDATA_START = TEST_CDATA ? "<![CDATA[" : "";
	private final String CDATA_END = TEST_CDATA ? "]]>" : "";

	protected String testString="<root><sub>abc&amp;&lt;&gt;</sub><sub>"+CDATA_START+"<a>a&amp;b</a>"+CDATA_END+"</sub></root>";
	protected String testJson="{\"key1\":\"string\",\"key2\":12,\"key3\":[1,2,3]}";

	@Test
	public void test11StreamAsStream() throws Exception {

		CloseObservableOutputStream target = new CloseObservableOutputStream();

		try (MessageOutputStream stream = new MessageOutputStream(null, target, (IForwardTarget)null)) {

			try (OutputStream outputstream = stream.asStream()) {
				outputstream.write(testString.getBytes());
			}
		}
		String actual = target.toString();
		assertEquals(testString, actual);
		assertTrue(target.isCloseCalled());
	}

	@Test
	public void test12StreamAsWriter() throws Exception {

		CloseObservableOutputStream target = new CloseObservableOutputStream();

		try (MessageOutputStream stream = new MessageOutputStream(null, target, (IForwardTarget)null)) {
			try (Writer writer = stream.asWriter()) {
				writer.write(testString);
			}
		}
		String actual = target.toString();
		assertEquals(testString, actual);
		assertTrue(target.isCloseCalled());
	}

	@Test
	public void test13StreamAsContentHandler() throws Exception {

		CloseObservableOutputStream target = new CloseObservableOutputStream();

		try (MessageOutputStream stream = new MessageOutputStream(null, target, (IForwardTarget)null)) {

			ContentHandler handler = stream.asContentHandler();

			InputSource inputSource = new InputSource(new StringReader(testString));
			XmlUtils.parseXml(inputSource, handler);
		}
		String actual = target.toString();
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+testString, actual);
		assertTrue(target.isCloseCalled());
	}

	@Test
	public void test14StreamAsJson() throws Exception {

		CloseObservableOutputStream target = new CloseObservableOutputStream();

		try (MessageOutputStream stream = new MessageOutputStream(null, target, (IForwardTarget)null)) {
			JsonEventHandler handler = stream.asJsonEventHandler();
			JsonUtils.parseJson(testJson, handler);
		}
		String actual = target.toString();
		assertEquals(testJson, actual);
		assertTrue(target.isCloseCalled());
	}

	@Test
	public void test21WriterAsStream() throws Exception {

		CloseObservableWriter target = new CloseObservableWriter();

		try (MessageOutputStream stream = new MessageOutputStream(null, target, (IForwardTarget)null)) {
			try (OutputStream outputstream = stream.asStream()) {
				outputstream.write(testString.getBytes());
			}
		}
		String actual = target.toString();
		assertEquals(testString, actual);
		assertTrue(target.isCloseCalled());
	}

	@Test
	public void test22WriterAsWriter() throws Exception {

		CloseObservableWriter target = new CloseObservableWriter();

		try (MessageOutputStream stream = new MessageOutputStream(null, target, (IForwardTarget)null)) {
			try (Writer writer = stream.asWriter()) {
				writer.write(testString);
			}
		}
		String actual = target.toString();
		assertEquals(testString, actual);
		assertTrue(target.isCloseCalled());
	}

	@Test
	public void test23WriterAsContentHandler() throws Exception {

		CloseObservableWriter target = new CloseObservableWriter();

		try (MessageOutputStream stream = new MessageOutputStream(null, target, (IForwardTarget)null)) {

			ContentHandler handler = stream.asContentHandler();

			InputSource inputSource = new InputSource(new StringReader(testString));
			XmlUtils.parseXml(inputSource, handler);
		}
		String actual = target.toString();
		assertEquals(testString, actual);
		assertTrue(target.isCloseCalled());
	}

	@Test
	public void test24WriterAsJson() throws Exception {

		CloseObservableWriter target = new CloseObservableWriter();

		try (MessageOutputStream stream = new MessageOutputStream(null, target, (IForwardTarget)null)) {
			JsonEventHandler handler = stream.asJsonEventHandler();
			JsonUtils.parseJson(testJson, handler);
		}
		String actual = target.toString();
		assertEquals(testJson, actual);
		assertTrue(target.isCloseCalled());
	}

	@Test
	public void testX21WriterAsStreamError() throws Exception {

		CloseObservableWriter target = new CloseObservableWriter() {

			@Override
			public void write(char[] arg0, int arg1, int arg2) {
				throw new RuntimeException("fakeFailure");
			}
		};

		try (MessageOutputStream stream = new MessageOutputStream(null, target, (IForwardTarget)null)) {

			try {
				try (OutputStream outputstream = stream.asStream()) {
					outputstream.write(testString.getBytes());
				}
				fail("exception should be thrown");
			} catch (Exception e) {
				assertThat(e.getMessage(),StringContains.containsString("fakeFailure"));
			}
		}
		assertTrue(target.isCloseCalled());
	}



	@Test
	public void test31ContentHandlerAsStream() throws Exception {

		CloseObservableXmlWriter target = new CloseObservableXmlWriter();

		try (MessageOutputStream stream = new MessageOutputStream(target)) {
			try (OutputStream outputstream = stream.asStream()) {
				outputstream.write(testString.getBytes());
			}
		}
		String actual = target.toString();
		assertEquals(testString, actual);
		assertTrue(target.isCloseCalled());
	}

	@Test
	public void test32ContentHandlerAsWriter() throws Exception {

		CloseObservableXmlWriter target = new CloseObservableXmlWriter();

		try (MessageOutputStream stream = new MessageOutputStream(target)) {

			try (Writer writer = stream.asWriter()) {
				writer.write(testString);
			}
		}
		String actual = target.toString();
		assertEquals(testString, actual);
		assertTrue(target.isCloseCalled());
	}

	@Test
	public void test41JsonAsStream() throws Exception {

		CloseObservableJsonWriter target = new CloseObservableJsonWriter();

		try (MessageOutputStream stream = new MessageOutputStream(target)) {
			try (OutputStream outputstream = stream.asStream()) {
				outputstream.write(testJson.getBytes());
			}
		}
		String actual = target.toString();
		assertEquals(testJson, actual);
		assertTrue(target.isCloseCalled());
	}

	@Test
	public void test42JsonAsWriter() throws Exception {

		CloseObservableJsonWriter target = new CloseObservableJsonWriter();

		try (MessageOutputStream stream = new MessageOutputStream(target)) {

			try (Writer writer = stream.asWriter()) {
				writer.write(testJson);
			}
		}
		String actual = target.toString();
		assertEquals(testJson, actual);
		assertTrue(target.isCloseCalled());
	}

	@Test
	public void testX12StreamAsWriterError() throws Exception {

		CloseObservableOutputStream target = new CloseObservableOutputStream() {

			@Override
			public synchronized void write(byte[] arg0, int arg1, int arg2) {
				throw new RuntimeException("fakeFailure");
			}
		};

		try (MessageOutputStream stream = new MessageOutputStream(null, target, (IForwardTarget)null)) {

			try {
				try (Writer writer = stream.asWriter()) {
					writer.write(testString);
				}
				fail("exception should be thrown");
			} catch (Exception e) {
				assertThat(e.getMessage(),StringContains.containsString("fakeFailure"));
			}
		}
		assertTrue(target.isCloseCalled());
	}

	@Test
	@Disabled("No contract to call endDocument() in case of an Exception")
	public void testX32ContentHandlerAsWriterError() throws Exception {

		CloseObservableWriter cow = new CloseObservableWriter() {

			@Override
			public void write(char[] arg0, int arg1, int arg2) {
				throw new RuntimeException("fakeFailure");
			}

		};
		Result result = new StreamResult(cow);
		SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();
		TransformerHandler transformerHandler = tf.newTransformerHandler();
		transformerHandler.setResult(result);

		try (MessageOutputStream stream = new MessageOutputStream(transformerHandler)) {

			try {
				try (Writer writer = stream.asWriter()) {
					writer.write(testString);
				}
				fail("exception should be thrown");
			} catch (Exception e) {
				assertThat(e.getMessage(),StringContains.containsString("fakeFailure"));
			}
		}
		assertTrue(cow.isCloseCalled());
	}

	@Test
	@Disabled("No contract to call endDocument() in case of an Exception")
	public void testX31ContentHandlerAsStreamError() throws Exception {

		CloseObservableOutputStream cos = new CloseObservableOutputStream() {

			@Override
			public void write(byte[] arg0, int arg1, int arg2) {
				throw new RuntimeException("fakeFailure");
			}

		};
		Result result = new StreamResult(cos);
		SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();
		TransformerHandler transformerHandler = tf.newTransformerHandler();
		transformerHandler.setResult(result);

		try (MessageOutputStream stream = new MessageOutputStream(transformerHandler)) {
			try {
				try (Writer writer = stream.asWriter()) {
					writer.write(testString);
				}
				fail("exception should be thrown");
			} catch (Exception e) {
				assertThat(e.getMessage(),StringContains.containsString("fakeFailure"));
			}
		}
		assertTrue(cos.isCloseCalled());
	}

	@Test
	public void testX13StreamAsContentHandlerError() throws Exception {

		CloseObservableOutputStream target = new CloseObservableOutputStream() {

			@Override
			public void write(byte[] arg0, int arg1, int arg2) {
				throw new RuntimeException("fakeFailure 1");
			}

			@Override
			public void write(byte[] b) throws IOException {
				throw new RuntimeException("fakeFailure 2");
			}

		};

		try (MessageOutputStream stream = new MessageOutputStream(null, target, (IForwardTarget)null)) {

			ContentHandler handler = stream.asContentHandler();

			try {
				InputSource inputSource = new InputSource(new StringReader(testString));
				XmlUtils.parseXml(inputSource, handler);
				fail("exception should be thrown");
			} catch (Exception e) {
				assertThat(e.getMessage(),StringContains.containsString("fakeFailure"));
			}
		}
		assertTrue(target.isCloseCalled());
	}

	@Test
	public void testX14StreamAsJsonError() throws Exception {

		CloseObservableOutputStream target = new CloseObservableOutputStream() {

			@Override
			public void write(byte[] arg0, int arg1, int arg2) {
				throw new RuntimeException("fakeFailure 1");
			}

			@Override
			public void write(byte[] b) throws IOException {
				throw new RuntimeException("fakeFailure 2");
			}
		};

		try (MessageOutputStream stream = new MessageOutputStream(null, target, (IForwardTarget)null)) {

			JsonEventHandler handler = stream.asJsonEventHandler();

			try {
				JsonUtils.parseJson(testJson, handler);
				fail("exception should be thrown");
			} catch (Exception e) {
				assertThat(e.getMessage(),StringContains.containsString("fakeFailure"));
			}
		}
		assertTrue(target.isCloseCalled());
	}

	@Test
	public void testX23WriterAsContentHandlerError() throws Exception {

		CloseObservableWriter target = new CloseObservableWriter() {

			@Override
			public void write(char[] arg0, int arg1, int arg2) {
				throw new RuntimeException("fakeFailure 1");
			}

			@Override
			public StringWriter append(char arg0) {
				throw new RuntimeException("fakeFailure 2");
			}

			@Override
			public StringWriter append(CharSequence arg0, int arg1, int arg2) {
				throw new RuntimeException("fakeFailure 3");
			}

			@Override
			public StringWriter append(CharSequence arg0) {
				throw new RuntimeException("fakeFailure 4");
			}

			@Override
			public void write(String arg0, int arg1, int arg2) {
				throw new RuntimeException("fakeFailure 5");
			}

			@Override
			public void write(String arg0) {
				throw new RuntimeException("fakeFailure 6");
			}
		};

		try (MessageOutputStream stream = new MessageOutputStream(null, target, (IForwardTarget)null)) {

			ContentHandler handler = stream.asContentHandler();

			try {
				InputSource inputSource = new InputSource(new StringReader(testString));
				XmlUtils.parseXml(inputSource, handler);
				fail("exception should be thrown");
			} catch (Exception e) {
				assertThat(e.getMessage(),StringContains.containsString("fakeFailure"));
			}
		}
		assertTrue(target.isCloseCalled());
	}

	@Test
	public void testX24WriterAsJsonError() throws Exception {

		CloseObservableWriter target = new CloseObservableWriter() {

			@Override
			public void write(char[] arg0, int arg1, int arg2) {
				throw new RuntimeException("fakeFailure 1");
			}

			@Override
			public StringWriter append(char arg0) {
				throw new RuntimeException("fakeFailure 2");
			}

			@Override
			public StringWriter append(CharSequence arg0, int arg1, int arg2) {
				throw new RuntimeException("fakeFailure 3");
			}

			@Override
			public StringWriter append(CharSequence arg0) {
				throw new RuntimeException("fakeFailure 4");
			}

			@Override
			public void write(String arg0, int arg1, int arg2) {
				throw new RuntimeException("fakeFailure 5");
			}

			@Override
			public void write(String arg0) {
				throw new RuntimeException("fakeFailure 6");
			}
		};

		try (MessageOutputStream stream = new MessageOutputStream(null, target, (IForwardTarget)null)) {

			JsonEventHandler handler = stream.asJsonEventHandler();

			try {
				JsonUtils.parseJson(testJson, handler);
				fail("exception should be thrown");
			} catch (Exception e) {
				assertThat(e.getMessage(),StringContains.containsString("fakeFailure"));
			}
		}
		assertTrue(target.isCloseCalled());
	}
}
