package nl.nn.adapterframework.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.rules.TemporaryFolder;

import nl.nn.adapterframework.testutil.SerializationTester;
import nl.nn.adapterframework.util.LogUtil;

public class UrlMessageTest {
	protected Logger log = LogUtil.getLogger(this);

	protected String testString = MessageTest.testString;
	protected String testStringFile = MessageTest.testStringFile;

	private String[][] wires = {
			{ "7.8 2022-04-20", "aced0005737200286e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e55726c4d657373616765834faa88987c7791020000787200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474000f4c6a6176612f7574696c2f4d61703b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400114c6a6176612f6c616e672f436c6173733b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e7672000c6a6176612e6e65742e55524c962537361afce47203000749000868617368436f6465490004706f72744c0009617574686f726974797400124c6a6176612f6c616e672f537472696e673b4c000466696c6571007e000b4c0004686f737471007e000b4c000870726f746f636f6c71007e000b4c000372656671007e000b787078" },
		};

	private SerializationTester<Message> serializationTester=new SerializationTester<>();

	@Test
	public void testSerialize() throws Exception {
		TemporaryFolder folder = new TemporaryFolder();
		folder.create();
		File source = folder.newFile();
		MessageTest.writeContentsToFile(source, testString);

		Message in = new UrlMessage(source.toURL(), new MessageContext().withCharset("UTF-8"));

		byte[] wire = serializationTester.serialize(in);
		log.debug("wire "+Hex.encodeHexString(wire));
		MessageTest.writeContentsToFile(source, "fakeContentAsReplacementOfThePrevious");
		Message out = serializationTester.deserialize(wire);

		assertTrue(out.isBinary());
		assertEquals(testString, out.asString());
	}

	@Test
	public void testDeserializationCompatibility() throws Exception {

		for (int i=0; i< wires.length; i++) {
			String label = wires[i][0];
			log.debug("testDeserializationCompatibility() "+label);
			byte[] wire = Hex.decodeHex(wires[i][1]);
			Message out = serializationTester.deserialize(wire);

			assertEquals(UrlMessage.class, out.getClass());
			assertTrue(out.isBinary(), label);
			assertEquals("UTF-8", out.getCharset(), label);
			assertEquals(testString, out.asString(), label);
		}
	}

}
