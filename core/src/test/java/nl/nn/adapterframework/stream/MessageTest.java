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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static org.hamcrest.core.StringStartsWith.startsWith;

import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.XmlWriter;

public class MessageTest {

	private boolean TEST_CDATA=true;
	private String CDATA_START=TEST_CDATA?"<![CDATA[":"";
	private String CDATA_END=TEST_CDATA?"]]>":"";

	protected String testString="<root><sub>abc&amp;&lt;&gt;</sub><sub>"+CDATA_START+"<a>a&amp;b</a>"+CDATA_END+"</sub><data attr=\"één €\">één €</data></root>";
	
	
	protected void testAsStream(Message adapter) throws IOException {
		InputStream result = adapter.asInputStream();
		String actual = StreamUtil.streamToString(result, null, "UTF-8");
		assertEquals(testString, actual);
	}
	
	protected void testAsReader(Message adapter) throws IOException {
		Reader result = adapter.asReader();
		String actual = StreamUtil.readerToString(result, null);
		assertEquals(testString, actual);
	}

	protected void testAsInputSource(Message adapter) throws IOException, SAXException {
		InputSource result = adapter.asInputSource();
		XmlWriter sink =  new XmlWriter();
		XmlUtils.parseXml(sink, result);
		
		String actual = sink.toString();
		assertEquals(testString, actual);
	}
	
	protected void testAsString(Message adapter) throws IOException {
		String actual = adapter.asString();
		assertEquals(testString, actual);
	}

	protected void testAsByteArray(Message adapter) throws IOException {
		byte[] actual = adapter.asByteArray();
		byte[] expected = testString.getBytes("UTF-8");
		assertEquals("lengths differ", expected.length, actual.length);
		for(int i=0; i<expected.length; i++) {
			assertEquals("byte arrays differ at position ["+i+"]",expected[i],actual[i]);
		}
	}
	
	protected void testToString(Message adapter, Class clazz) {
		String actual = adapter.toString();
		assertThat(actual, startsWith(clazz.getSimpleName()));
		assertEquals(adapter.asObject().getClass().getName(), clazz.getName());
	}
	
	@Test
	public void testStreamAsStream() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source);
		testAsStream(adapter);
	}
	
	@Test
	public void testStreamAsReader() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source);
		testAsReader(adapter);
	}
	
	@Test
	public void testStreamAsInputSource() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source);
		testAsInputSource(adapter);
	}
	
	@Test
	public void testStreamAsByteArray() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testStreamAsString() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source);
		testAsString(adapter);
	}

	@Test
	public void testStreamToString() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes("utf-8"));
		Message adapter = new Message(source);
		testToString(adapter, ByteArrayInputStream.class);
	}

	
	@Test
	public void testReaderAsStream() throws Exception {
		StringReader source = new StringReader(testString);
		Message adapter = new Message(source);
		testAsStream(adapter);
	}

	@Test
	public void testReaderAsReader() throws Exception {
		StringReader source = new StringReader(testString);
		Message adapter = new Message(source);
		testAsReader(adapter);
	}

	@Test
	public void testReaderAsInputSource() throws Exception {
		StringReader source = new StringReader(testString);
		Message adapter = new Message(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testReaderAsByteArray() throws Exception {
		StringReader source = new StringReader(testString);
		Message adapter = new Message(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testReaderAsString() throws Exception {
		StringReader source = new StringReader(testString);
		Message adapter = new Message(source);
		testAsString(adapter);
	}

	@Test
	public void testReaderToString() throws Exception {
		StringReader source = new StringReader(testString);
		Message adapter = new Message(source);
		testToString(adapter, StringReader.class);
	}

	
	@Test
	public void testStringAsStream() throws Exception {
		String source = testString;
		Message adapter = new Message(source);
		testAsStream(adapter);
	}

	@Test
	public void testStringAsReader() throws Exception {
		String source = testString;
		Message adapter = new Message(source);
		testAsReader(adapter);
	}

	@Test
	public void testStringAsInputSource() throws Exception {
		String source = testString;
		Message adapter = new Message(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testStringAsByteArray() throws Exception {
		String source = testString;
		Message adapter = new Message(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testStringAsString() throws Exception {
		String source = testString;
		Message adapter = new Message(source);
		testAsString(adapter);
	}

	@Test
	public void testStringToString() throws Exception {
		String source = testString;
		Message adapter = new Message(source);
		testToString(adapter, String.class);
	}

	
	@Test
	public void testByteArrayAsStream() throws Exception {
		byte[] source = testString.getBytes("utf-8");
		Message adapter = new Message(source);
		testAsStream(adapter);
	}

	@Test
	public void testByteArrayAsReader() throws Exception {
		byte[] source = testString.getBytes("utf-8");
		Message adapter = new Message(source);
		testAsReader(adapter);
	}

	@Test
	public void testByteArrayAsInputSource() throws Exception {
		byte[] source = testString.getBytes("utf-8");
		Message adapter = new Message(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testByteArrayAsByteArray() throws Exception {
		byte[] source = testString.getBytes("utf-8");
		Message adapter = new Message(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testByteArrayAsString() throws Exception {
		byte[] source = testString.getBytes("utf-8");
		Message adapter = new Message(source);
		testAsString(adapter);
	}

	@Test
	public void testByteArrayToString() throws Exception {
		byte[] source = testString.getBytes("utf-8");
		Message adapter = new Message(source);
		testToString(adapter,byte[].class);
	}
}
