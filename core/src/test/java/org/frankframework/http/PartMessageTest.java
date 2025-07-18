package org.frankframework.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.input.NullInputStream;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.MimeType;

import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.stream.MessageTest;
import org.frankframework.testutil.SerializationTester;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.LogUtil;

public class PartMessageTest {
	protected Logger log = LogUtil.getLogger(this);

	protected String testString = MessageTest.testString;
	protected String testStringFile = MessageTest.testStringFile;
	protected int testStringLength = 116;

	private final String[][] wires = {
		{ "7.8 2022-04-20", "aced0005737200276e6c2e6e6e2e616461707465726672616d65776f726b2e687474702e506172744d65737361676541c94d37efd077bc020000787200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474000f4c6a6176612f7574696c2f4d61703b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400114c6a6176612f6c616e672f436c6173733b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e767200346e6c2e6e6e2e616461707465726672616d65776f726b2e687474702e506172744d657373616765546573742454657374506172740000000000000000000000787078"},
		{ "7.9 2023-12-21", "aced0005737200276e6c2e6e6e2e616461707465726672616d65776f726b2e687474702e506172744d65737361676541c94d37efd077bc020000787200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474002e4c6e6c2f6e6e2f616461707465726672616d65776f726b2f73747265616d2f4d657373616765436f6e746578743b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400124c6a6176612f6c616e672f537472696e673b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38737200376e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e53657269616c697a61626c6546696c655265666572656e636500000000000000010300035a000662696e6172794a000473697a654c00076368617273657471007e000478707787000000000000000100000000000000740100003c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e78740008546573745061727478" },
		{ "8.0 2023-12-21", "aced0005737200236f72672e6672616e6b6672616d65776f726b2e687474702e506172744d65737361676541c94d37efd077bc020000787200216f72672e6672616e6b6672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474002a4c6f72672f6672616e6b6672616d65776f726b2f73747265616d2f4d657373616765436f6e746578743b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400124c6a6176612f6c616e672f537472696e673b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38737200336f72672e6672616e6b6672616d65776f726b2e73747265616d2e53657269616c697a61626c6546696c655265666572656e636500000000000000010300035a000662696e6172794a000473697a654c00076368617273657471007e000478707787000000000000000100000000000000740100003c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e78740008546573745061727478" },
	};

	private final SerializationTester<Message> serializationTester = new SerializationTester<>();

	@Test
	public void testCharset() throws Exception {
		TestPart testPart = new TestPart("application/pdf; charset=UTF-8");

		PartMessage partMessage = new PartMessage(testPart);

		assertEquals("UTF-8", partMessage.getCharset());
		assertEquals(MimeType.valueOf("application/pdf; charset=UTF-8"), partMessage.getContext().get(MessageContext.METADATA_MIMETYPE));
	}

	@Test
	public void testIllegalCharset() throws Exception {

		TestPart testPart = new TestPart("application/pdf; charset=application/pdf");

		PartMessage partMessage = new PartMessage(testPart);

		assertNull(partMessage.getCharset());
		assertEquals(MimeType.valueOf("application/pdf"), (MimeType)partMessage.getContext().get(MessageContext.METADATA_MIMETYPE));
	}

	private static class TestPart extends MimeBodyPart {
		private final InputStream stream;//non-repeatable

		public TestPart(String contentType) {
			super();
			headers.addHeader("Content-Type", contentType);
			stream = new NullInputStream();
		}

		public TestPart(URL url) throws IOException {
			this.stream = url.openStream();
			headers.addHeader("Content-Type", "application/octet-stream");
		}

		@Override
		public InputStream getInputStream() throws IOException, MessagingException {
			return stream;
		}
	}

	@Test
	public void testParameterValue() throws Exception {
		String sessionKey = "file";

		Parameter p = new Parameter();
		p.setName("contents");
		p.setSessionKey(sessionKey);
		p.configure();

		PipeLineSession session = new PipeLineSession();
		TestPart testPart = new TestPart(TestFileUtils.getTestFileURL("/file.xml"));
		session.put(sessionKey, new PartMessage(testPart));

		ParameterValueList pvl = new ParameterValueList();
		Message message = new Message("fakeMessage");

		Object value = p.getValue(pvl, message, session, false);
		assertTrue(value instanceof Message);
		assertEquals("<file>in root of classpath</file>", ((Message)value).asString());
	}

	@Test
	public void testSerialize(@TempDir Path folder) throws Exception {
		// NB: This test logs a wire that can be added to future compatibility checkpoints
		File source = folder.resolve("testSerialize").toFile();
		MessageTest.writeContentsToFile(source, testString);

		TestPart testPart = new TestPart(source.toURI().toURL());
		Message in = new PartMessage(testPart, "UTF-8");

		//assertEquals(testStringLength, in.size());
		byte[] wire = serializationTester.serialize(in);
		log.debug("wire {}", () -> Hex.encodeHexString(wire));
		MessageTest.writeContentsToFile(source, "fakeContentAsReplacementOfThePrevious");
		Message out = serializationTester.deserialize(wire);

		assertTrue(out.isBinary());
		assertEquals(testString, out.asString());
		assertEquals(testStringLength, out.size());
	}

	@Test
	public void testDeserializationCompatibility() throws Exception {

		for (int i=0; i< wires.length; i++) {
			String label = wires[i][0];
			log.debug("testDeserializationCompatibility() {}", label);
			byte[] wire = Hex.decodeHex(wires[i][1]);
			Message out = serializationTester.deserialize(wire);

			assertEquals(PartMessage.class, out.getClass());
			assertTrue(out.isBinary(), label);
			assertEquals("UTF-8", out.getCharset(), label);
			assertEquals(testString,out.asString(), label);
			assertEquals(testStringLength, out.size());
		}
	}
}
