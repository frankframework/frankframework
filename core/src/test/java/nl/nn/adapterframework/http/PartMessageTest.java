package nl.nn.adapterframework.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeBodyPart;

import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.util.MimeType;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;
import nl.nn.adapterframework.stream.MessageTest;
import nl.nn.adapterframework.testutil.SerializationTester;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.LogUtil;

public class PartMessageTest {
	protected Logger log = LogUtil.getLogger(this);

	protected String testString = MessageTest.testString;
	protected String testStringFile = MessageTest.testStringFile;
	protected int testStringLength = 116;

	private String[][] wires = {
			{ "7.8 2022-04-20", "aced0005737200276e6c2e6e6e2e616461707465726672616d65776f726b2e687474702e506172744d65737361676541c94d37efd077bc020000787200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474000f4c6a6176612f7574696c2f4d61703b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400114c6a6176612f6c616e672f436c6173733b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e767200346e6c2e6e6e2e616461707465726672616d65776f726b2e687474702e506172744d657373616765546573742454657374506172740000000000000000000000787078" },
		};

	private SerializationTester<Message> serializationTester=new SerializationTester<Message>();

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
		assertNull(partMessage.getContext().get(MessageContext.METADATA_MIMETYPE));
	}

	private class TestPart extends MimeBodyPart {
		private InputStream stream = null;//non-repeatable

		public TestPart(String contentType) {
			super();
			headers.addHeader("Content-Type", contentType);
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
	public void testSerialize() throws Exception {
		TemporaryFolder folder = new TemporaryFolder();
		folder.create();
		File source = folder.newFile();
		MessageTest.writeContentsToFile(source, testString);

		TestPart testPart = new TestPart(source.toURL());
		Message in = new PartMessage(testPart, "UTF-8");

		//assertEquals(testStringLength, in.size());
		byte[] wire = serializationTester.serialize(in);
		log.debug("wire "+Hex.encodeHexString(wire));
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
			log.debug("testDeserializationCompatibility() "+label);
			byte[] wire = Hex.decodeHex(wires[i][1]);
			Message out = serializationTester.deserialize(wire);

			assertEquals(PartMessage.class, out.getClass());
			assertTrue(label, out.isBinary());
			assertEquals(label, "UTF-8", out.getCharset());
			assertEquals(label, testString,out.asString());
			assertEquals(testStringLength, out.size());
		}
	}


}
