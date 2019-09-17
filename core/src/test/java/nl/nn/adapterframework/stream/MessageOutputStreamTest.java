/*
   Copyright 2019 Integration Partners

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
package nl.nn.adapterframework.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.transform.Result;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.hamcrest.core.StringContains;
import org.junit.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import nl.nn.adapterframework.util.XmlUtils;

public class MessageOutputStreamTest {

	protected String testString="<root><sub>oh</sub><sub>ah</sub></root>";
	
	
	@Test
	public void testStreamAsStream() throws Exception {
		
		ByteArrayOutputStream target = new ByteArrayOutputStream();

		MessageOutputStream stream = new MessageOutputStream(target,null);

		try (OutputStream outputstream = stream.asStream()) {
			outputstream.write(testString.getBytes());
		}

		String actual = new String (target.toByteArray());
		assertEquals(testString, actual);
	}
	
	@Test
	public void testWriterAsWriter() throws Exception {
		
		StringWriter target = new StringWriter();

		MessageOutputStream stream = new MessageOutputStream(target,null);

		try (Writer writer = stream.asWriter()) {
			writer.write(testString);
		}

		String actual = new String (target.toString());
		assertEquals(testString, actual);
	}

	@Test
	public void testWriterAsStream() throws Exception {
		
		StringWriter target = new StringWriter();

		MessageOutputStream stream = new MessageOutputStream(target,null);

		try (OutputStream outputstream = stream.asStream()) {
			outputstream.write(testString.getBytes());
		}

		String actual = new String (target.toString());
		assertEquals(testString, actual);
	}

	@Test
	public void testStreamAsWriter() throws Exception {
		
		ByteArrayOutputStream target = new ByteArrayOutputStream();
		
		MessageOutputStream stream = new MessageOutputStream(target,null);
		
		try (Writer writer = stream.asWriter()) {
			writer.write(testString);
		}
		
		String actual = new String (target.toByteArray());
		assertEquals(testString, actual);
	}

	@Test
	public void testContentHandlerAsWriter() throws Exception {
		
        SAXTransformerFactory tf = (SAXTransformerFactory)TransformerFactory.newInstance();
        TransformerHandler transformerHandler = tf.newTransformerHandler();
        StringWriter sw = new StringWriter();
        Result result = new StreamResult(sw);
        transformerHandler.setResult(result);

		MessageOutputStream stream = new MessageOutputStream(transformerHandler,null);
		
		try (Writer writer = stream.asWriter()) {
			writer.write(testString);
		}
		
		String actual = new String (sw.toString());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+testString, actual);
	}

	@Test
	public void testWriterAsContentHandler() throws Exception {
		
		Writer target = new StringWriter();

		MessageOutputStream stream = new MessageOutputStream(target,null);

		ContentHandler handler = stream.asContentHandler();

		InputSource inputSource = new InputSource(new StringReader(testString)); 
		XMLReader reader =XmlUtils.getXMLReader(true, false);
		reader.setContentHandler(handler);
		reader.parse(inputSource);
		
		String actual = new String (target.toString());
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+testString, actual);
	}

	@Test
	public void testWriterAsStreamError() throws Exception {
		
		StringWriter target = new StringWriter() {

			@Override
			public void write(char[] arg0, int arg1, int arg2) {
				throw new RuntimeException("fakeFailure");
			}
			
		};

		MessageOutputStream stream = new MessageOutputStream(target,null);

		try {
			try (OutputStream outputstream = stream.asStream()) {
				outputstream.write(testString.getBytes());
			}
			fail("exception should be thrown");
		} catch (Exception e) {
			assertThat(e.getMessage(),StringContains.containsString("fakeFailure"));
		}
	}

	@Test
	public void testStreamAsWriterError() throws Exception {
		
		ByteArrayOutputStream target = new ByteArrayOutputStream() {

			@Override
			public synchronized void write(byte[] arg0, int arg1, int arg2) {
				throw new RuntimeException("fakeFailure");
			}
			
		};
		
		MessageOutputStream stream = new MessageOutputStream(target,null);
		
		try {
			try (Writer writer = stream.asWriter()) {
				writer.write(testString);
			}
			fail("exception should be thrown");
		} catch (Exception e) {
			assertThat(e.getMessage(),StringContains.containsString("fakeFailure"));
		}
	}

	@Test
	public void testContentHandlerAsWriterError() throws Exception {
		
        SAXTransformerFactory tf = (SAXTransformerFactory)TransformerFactory.newInstance();
        TransformerHandler transformerHandler = tf.newTransformerHandler();
		StringWriter sw = new StringWriter() {

			@Override
			public void write(char[] arg0, int arg1, int arg2) {
				throw new RuntimeException("fakeFailure");
			}
			
		};
        Result result = new StreamResult(sw);
        transformerHandler.setResult(result);

		MessageOutputStream stream = new MessageOutputStream(transformerHandler,null);
		
		try {
			try (Writer writer = stream.asWriter()) {
				writer.write(testString);
			}
			fail("exception should be thrown");
		} catch (Exception e) {
			assertThat(e.getMessage(),StringContains.containsString("fakeFailure"));
		}
	}

	@Test
	public void testWriterAsContentHandlerError() throws Exception {
		
		StringWriter target = new StringWriter() {

			@Override
			public void write(char[] arg0, int arg1, int arg2) {
				throw new RuntimeException("fakeFailure");
			}
			
		};

		MessageOutputStream stream = new MessageOutputStream(target,null);

		ContentHandler handler = stream.asContentHandler();

		try {
			InputSource inputSource = new InputSource(new StringReader(testString)); 
			XMLReader reader =XmlUtils.getXMLReader(true, false);
			reader.setContentHandler(handler);
			reader.parse(inputSource);
			fail("exception should be thrown");
		} catch (Exception e) {
			assertThat(e.getMessage(),StringContains.containsString("fakeFailure"));
		}
		
	}

	
	
	
}
