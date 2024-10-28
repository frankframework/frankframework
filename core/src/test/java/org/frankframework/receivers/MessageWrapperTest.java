package org.frankframework.receivers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.mail.internet.InternetHeaders;
import jakarta.mail.internet.MimeBodyPart;

import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

import org.frankframework.http.PartMessage;
import org.frankframework.stream.Message;
import org.frankframework.stream.PathMessage;
import org.frankframework.testutil.SerializationTester;

@Log4j2
public class MessageWrapperTest {

	private static final String characterWire76 = "aced00057372002f6e6c2e6e6e2e616461707465726672616d65776f726b2e7265636569766572732e4d657373616765577261707065728d7e867056c0b4ff0200034c0007636f6e7465787474000f4c6a6176612f7574696c2f4d61703b4c000269647400124c6a6176612f6c616e672f537472696e673b4c00076d6573736167657400274c6e6c2f6e6e2f616461707465726672616d65776f726b2f73747265616d2f4d6573736167653b7870737200176a6176612e7574696c2e4c696e6b6564486173684d617034c04e5c106cc0fb0200015a000b6163636573734f72646572787200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f4000000000000c770800000010000000017400196d65737361676557726170706572436f6e746578744974656d74000966616b6556616c7565780074000666616b654964737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300034c00076368617273657471007e00024c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000e777261707065645265717565737471007e000c78707074001c746573746461746120766f6f72206d657373616765777261707065727078";
	private static final String binaryWire76 =    "aced00057372002f6e6c2e6e6e2e616461707465726672616d65776f726b2e7265636569766572732e4d657373616765577261707065728d7e867056c0b4ff0200034c0007636f6e7465787474000f4c6a6176612f7574696c2f4d61703b4c000269647400124c6a6176612f6c616e672f537472696e673b4c00076d6573736167657400274c6e6c2f6e6e2f616461707465726672616d65776f726b2f73747265616d2f4d6573736167653b7870737200176a6176612e7574696c2e4c696e6b6564486173684d617034c04e5c106cc0fb0200015a000b6163636573734f72646572787200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f4000000000000c770800000010000000017400196d65737361676557726170706572436f6e746578744974656d74000966616b6556616c7565780074000666616b654964737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300034c00076368617273657471007e00024c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000e777261707065645265717565737471007e000c787070757200025b42acf317f8060854e002000078700000001c746573746461746120766f6f72206d657373616765777261707065727078";
	private static final String characterWire77 = "aced00057372002f6e6c2e6e6e2e616461707465726672616d65776f726b2e7265636569766572732e4d657373616765577261707065728d7e867056c0b4ff0200034c0007636f6e7465787474000f4c6a6176612f7574696c2f4d61703b4c000269647400124c6a6176612f6c616e672f537472696e673b4c00076d6573736167657400274c6e6c2f6e6e2f616461707465726672616d65776f726b2f73747265616d2f4d6573736167653b7870737200176a6176612e7574696c2e4c696e6b6564486173684d617034c04e5c106cc0fb0200015a000b6163636573734f72646572787200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f4000000000000c770800000010000000017400196d65737361676557726170706572436f6e746578744974656d74000966616b6556616c7565780074000666616b654964737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300044c00076368617273657471007e00024c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400114c6a6176612f6c616e672f436c6173733b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707074001c746573746461746120766f6f72206d65737361676577726170706572767200106a6176612e6c616e672e537472696e67a0f0a4387a3bb34202000078707078";
	private static final String binaryWire77 =    "aced00057372002f6e6c2e6e6e2e616461707465726672616d65776f726b2e7265636569766572732e4d657373616765577261707065728d7e867056c0b4ff0200034c0007636f6e7465787474000f4c6a6176612f7574696c2f4d61703b4c000269647400124c6a6176612f6c616e672f537472696e673b4c00076d6573736167657400274c6e6c2f6e6e2f616461707465726672616d65776f726b2f73747265616d2f4d6573736167653b7870737200176a6176612e7574696c2e4c696e6b6564486173684d617034c04e5c106cc0fb0200015a000b6163636573734f72646572787200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f4000000000000c770800000010000000017400196d65737361676557726170706572436f6e746578744974656d74000966616b6556616c7565780074000666616b654964737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300044c00076368617273657471007e00024c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400114c6a6176612f6c616e672f436c6173733b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b787070757200025b42acf317f8060854e002000078700000001c746573746461746120766f6f72206d657373616765777261707065727671007e00107078";
	private static final String characterWire78 = "aced00057372002f6e6c2e6e6e2e616461707465726672616d65776f726b2e7265636569766572732e4d657373616765577261707065728d7e867056c0b4ff0300034c0007636f6e7465787474000f4c6a6176612f7574696c2f4d61703b4c000269647400124c6a6176612f6c616e672f537472696e673b4c00076d6573736167657400274c6e6c2f6e6e2f616461707465726672616d65776f726b2f73747265616d2f4d6573736167653b7870737200176a6176612e7574696c2e4c696e6b6564486173684d617034c04e5c106cc0fb0200015a000b6163636573734f72646572787200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f4000000000000c770800000010000000017400196d65737361676557726170706572436f6e746578744974656d74000966616b6556616c7565780074000666616b654964737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787471007e00014c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c61737371007e00024c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707074001c746573746461746120766f6f72206d65737361676577726170706572740006537472696e677878";
	private static final String binaryWire78 =    "aced00057372002f6e6c2e6e6e2e616461707465726672616d65776f726b2e7265636569766572732e4d657373616765577261707065728d7e867056c0b4ff0300034c0007636f6e7465787474000f4c6a6176612f7574696c2f4d61703b4c000269647400124c6a6176612f6c616e672f537472696e673b4c00076d6573736167657400274c6e6c2f6e6e2f616461707465726672616d65776f726b2f73747265616d2f4d6573736167653b7870737200176a6176612e7574696c2e4c696e6b6564486173684d617034c04e5c106cc0fb0200015a000b6163636573734f72646572787200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f4000000000000c770800000010000000017400196d65737361676557726170706572436f6e746578744974656d74000966616b6556616c7565780074000666616b654964737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787471007e00014c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c61737371007e00024c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b787070757200025b42acf317f8060854e002000078700000001c746573746461746120766f6f72206d65737361676577726170706572740006627974655b5d7878";
	private static final String characterWire79 = "aced00057372002f6e6c2e6e6e2e616461707465726672616d65776f726b2e7265636569766572732e4d657373616765577261707065728d7e867056c0b4ff0300024c000d636f7272656c6174696f6e49647400124c6a6176612f6c616e672f537472696e673b4c00076d6573736167657400274c6e6c2f6e6e2f616461707465726672616d65776f726b2f73747265616d2f4d6573736167653b7870737200176a6176612e7574696c2e4c696e6b6564486173684d617034c04e5c106cc0fb0200015a000b6163636573734f72646572787200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f4000000000000c770800000010000000017400196d65737361676557726170706572436f6e746578744974656d74000966616b6556616c7565780074000666616b654964737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474000f4c6a6176612f7574696c2f4d61703b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c61737371007e00014c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707074001c746573746461746120766f6f72206d65737361676577726170706572740006537472696e677874001166616b65436f7272656c6174696f6e496478";
	private static final String binaryWire79 =    "aced00057372002f6e6c2e6e6e2e616461707465726672616d65776f726b2e7265636569766572732e4d657373616765577261707065728d7e867056c0b4ff0300024c000d636f7272656c6174696f6e49647400124c6a6176612f6c616e672f537472696e673b4c00076d6573736167657400274c6e6c2f6e6e2f616461707465726672616d65776f726b2f73747265616d2f4d6573736167653b7870737200176a6176612e7574696c2e4c696e6b6564486173684d617034c04e5c106cc0fb0200015a000b6163636573734f72646572787200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f4000000000000c770800000010000000017400196d65737361676557726170706572436f6e746578744974656d74000966616b6556616c7565780074000666616b654964737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474000f4c6a6176612f7574696c2f4d61703b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c61737371007e00014c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b787070757200025b42acf317f8060854e002000078700000001c746573746461746120766f6f72206d65737361676577726170706572740006627974655b5d7874001166616b65436f7272656c6174696f6e496478";
	private static final String characterWire80 = "aced00057372002b6f72672e6672616e6b6672616d65776f726b2e7265636569766572732e4d657373616765577261707065728d7e867056c0b4ff0300014c00076d6573736167657400234c6f72672f6672616e6b6672616d65776f726b2f73747265616d2f4d6573736167653b7870737200176a6176612e7574696c2e4c696e6b6564486173684d617034c04e5c106cc0fb0200015a000b6163636573734f72646572787200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f4000000000000c770800000010000000037400036d696474000666616b65496474000363696474001166616b65436f7272656c6174696f6e49647400196d65737361676557726170706572436f6e746578744974656d74000966616b6556616c7565780071007e0007737200216f72672e6672616e6b6672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474002a4c6f72672f6672616e6b6672616d65776f726b2f73747265616d2f4d657373616765436f6e746578743b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400124c6a6176612f6c616e672f537472696e673b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707074001c746573746461746120766f6f72206d65737361676577726170706572740006537472696e677871007e000978";
	private static final String binaryWire80 =    "aced00057372002b6f72672e6672616e6b6672616d65776f726b2e7265636569766572732e4d657373616765577261707065728d7e867056c0b4ff0300014c00076d6573736167657400234c6f72672f6672616e6b6672616d65776f726b2f73747265616d2f4d6573736167653b7870737200176a6176612e7574696c2e4c696e6b6564486173684d617034c04e5c106cc0fb0200015a000b6163636573734f72646572787200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f4000000000000c770800000010000000037400036d696474000666616b65496474000363696474001166616b65436f7272656c6174696f6e49647400196d65737361676557726170706572436f6e746578744974656d74000966616b6556616c7565780071007e0007737200216f72672e6672616e6b6672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474002a4c6f72672f6672616e6b6672616d65776f726b2f73747265616d2f4d657373616765436f6e746578743b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400124c6a6176612f6c616e672f537472696e673b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b787070757200025b42acf317f8060854e002000078700000001c746573746461746120766f6f72206d65737361676577726170706572740006627974655b5d7871007e000978";

	private final SerializationTester<MessageWrapper<?>> serializationTester = new SerializationTester<>();

	@Test
	public void testSerializeDeserializeCharacters() throws Exception {
		// NB: This test logs a character-wire that can be added to future compatibility checkpoints
		String data = "testdata voor messagewrapper";
		String id = "fakeId";
		String correlationId = "fakeCorrelationId";
		String contextKey = "messageWrapperContextItem";
		String contextValue = "fakeValue";

		MessageWrapper<?> in = new MessageWrapper<>(new Message(data), id, correlationId);
		in.getContext().put(contextKey, contextValue);
		in.getContext().put("NON-SERIALIZABLE-VALUE", new Object());

		byte[] wire = serializationTester.serialize(in);
		log.debug("Current characterWire: [{}]", ()-> Hex.encodeHexString(wire));

		assertNotNull(wire);
		MessageWrapper<?> out = serializationTester.deserialize(wire);

		assertFalse(out.getMessage().isBinary());
		assertEquals(data, out.getMessage().asString());
		assertEquals(id, out.getId());
		assertEquals(correlationId, out.getCorrelationId());
		assertEquals(contextValue, out.getContext().get(contextKey));
		assertFalse(out.getContext().containsKey("NON-SERIALIZABLE-VALUE"));
	}

	@Test
	public void testSerializeDeserializeWithNullValues() throws Exception {
		String data = "testdata voor messagewrapper";
		String contextKey = "messageWrapperContextItem";
		String contextValue = "fakeValue";

		MessageWrapper<?> in = new MessageWrapper<>(new Message(data), null, null);
		in.getContext().put(contextKey, contextValue);

		byte[] wire = serializationTester.serialize(in);

		assertNotNull(wire);
		MessageWrapper<?> out = serializationTester.deserialize(wire);

		assertFalse(out.getMessage().isBinary());
		assertEquals(data, out.getMessage().asString());
		assertNull(out.getId());
		assertNull(out.getCorrelationId());
		assertEquals(contextValue, out.getContext().get(contextKey));
	}


	@Test
	public void testSerializeDeserializeBinary() throws Exception {
		// NB: This test logs a binary-wire that can be added to future compatibility checkpoints
		byte[] data = "testdata voor messagewrapper".getBytes();
		String id = "fakeId";
		String correlationId = "fakeCorrelationId";
		String contextKey = "messageWrapperContextItem";
		String contextValue = "fakeValue";

		MessageWrapper<?> in = new MessageWrapper<>(new Message(data), id, correlationId);
		in.getContext().put(contextKey, contextValue);

		byte[] wire = serializationTester.serialize(in);
		log.debug("Current binaryWire: [{}]", ()-> Hex.encodeHexString(wire));

		assertNotNull(wire);
		MessageWrapper<?> out = serializationTester.deserialize(wire);

		assertTrue(out.getMessage().isBinary());
		assertEquals(new String(data), out.getMessage().asString());
		assertEquals(id, out.getId());
		assertEquals(correlationId, out.getCorrelationId());
		assertEquals(contextValue, out.getContext().get(contextKey));
	}

	@Test
	public void testSerializeDeserializePath() throws Exception {
		byte[] data = "testdata voor messagewrapper".getBytes();
		String id = "fakeId";
		String correlationId = "fakeCorrelationId";
		String contextKey = "messageWrapperContextItem";
		String contextValue = "fakeValue";


		Path file = Files.createTempFile("MessageWrapperTest", null);
		Files.write(file, data);

		MessageWrapper<?> in = new MessageWrapper<>(new PathMessage(file), id, correlationId);
		in.getContext().put(contextKey, contextValue);

		byte[] wire = serializationTester.serialize(in);

		Files.delete(file);

		assertNotNull(wire);
		MessageWrapper<?> out = serializationTester.deserialize(wire);

		assertTrue(out.getMessage().isBinary());
		assertEquals(new String(data), out.getMessage().asString());
		assertEquals(id, out.getId());
		assertEquals(correlationId, out.getCorrelationId());
		assertEquals(contextValue, out.getContext().get(contextKey));
	}

	@Test
	public void testSerializeDeserializeMimeBodyPart() throws Exception {
		byte[] data = "testdata voor messagewrapper".getBytes();
		String id = "fakeId";
		String correlationId = "fakeCorrelationId";
		String contextKey = "messageWrapperContextItem";
		String contextValue = "fakeValue";


		MimeBodyPart bodyPart = new MimeBodyPart(new InternetHeaders(), data);

		MessageWrapper<?> in = new MessageWrapper<>(new PartMessage(bodyPart), id, correlationId);
		in.getContext().put(contextKey, contextValue);

		byte[] wire = serializationTester.serialize(in);

		assertNotNull(wire);
		MessageWrapper<?> out = serializationTester.deserialize(wire);

		assertTrue(out.getMessage().isBinary());
		assertEquals(new String(data), out.getMessage().asString());
		assertEquals(id, out.getId());
		assertEquals(correlationId, out.getCorrelationId());
		assertEquals(contextValue, out.getContext().get(contextKey));
	}

	@Test
	public void testDeserialization76CompatibilityCharacters() throws Exception {
		String data = "testdata voor messagewrapper";
		String id = "fakeId";
		String contextKey = "messageWrapperContextItem";
		String contextValue = "fakeValue";

		byte[] wire = Hex.decodeHex(characterWire76);
		MessageWrapper<?> out = serializationTester.deserialize(wire);

		assertFalse(out.getMessage().isBinary());
		assertEquals(data, out.getMessage().asString());
		assertEquals(id, out.getId());
		assertNull(out.getCorrelationId());
		assertEquals(contextValue, out.getContext().get(contextKey));
	}


	@Test
	public void testDeserialization76CompatibilityBinary() throws Exception {
		byte[] data = "testdata voor messagewrapper".getBytes();
		String id = "fakeId";
		String contextKey = "messageWrapperContextItem";
		String contextValue = "fakeValue";

		byte[] wire = Hex.decodeHex(binaryWire76);
		MessageWrapper<?> out = serializationTester.deserialize(wire);

		assertTrue(out.getMessage().isBinary());
		assertEquals(new String(data), out.getMessage().asString());
		assertEquals(id, out.getId());
		assertNull(out.getCorrelationId());
		assertEquals(contextValue, out.getContext().get(contextKey));
	}

	@Test
	public void testDeserialization77CompatibilityCharacters() throws Exception {
		String data = "testdata voor messagewrapper";
		String id = "fakeId";
		String contextKey = "messageWrapperContextItem";
		String contextValue = "fakeValue";

//		MessageWrapper in = new MessageWrapper();
//		in.setMessage(new Message(data));
//		in.setId(id);
//		in.getContext().put(contextKey, contextValue);
//		byte[] wire = serializationTester.serialize(in);
//		System.out.println("Character: "+Hex.encodeHexString(wire));

		byte[] wire = Hex.decodeHex(characterWire77);
		MessageWrapper<?> out = serializationTester.deserialize(wire);

		assertFalse(out.getMessage().isBinary());
		assertEquals(data, out.getMessage().asString());
		assertEquals(id, out.getId());
		assertNull(out.getCorrelationId());
		assertEquals(contextValue, out.getContext().get(contextKey));
	}


	@Test
	public void testDeserialization77CompatibilityBinary() throws Exception {
		byte[] data = "testdata voor messagewrapper".getBytes();
		String id = "fakeId";
		String contextKey = "messageWrapperContextItem";
		String contextValue = "fakeValue";

//		MessageWrapper in = new MessageWrapper();
//		in.setMessage(new Message(data));
//		in.setId(id);
//		in.getContext().put(contextKey, contextValue);
//		byte[] wire = serializationTester.serialize(in);
//		System.out.println("Bytes: "+Hex.encodeHexString(wire));

		byte[] wire = Hex.decodeHex(binaryWire77);
		MessageWrapper<?> out = serializationTester.deserialize(wire);

		assertTrue(out.getMessage().isBinary());
		assertEquals(new String(data), out.getMessage().asString());
		assertEquals(id, out.getId());
		assertNull(out.getCorrelationId());
		assertEquals(contextValue, out.getContext().get(contextKey));
	}

	@Test
	public void testDeserialization78CompatibilityCharacters() throws Exception {
		String data = "testdata voor messagewrapper";
		String id = "fakeId";
		String contextKey = "messageWrapperContextItem";
		String contextValue = "fakeValue";

//		MessageWrapper in = new MessageWrapper();
//		in.setMessage(new Message(data));
//		in.setId(id);
//		in.getContext().put(contextKey, contextValue);
//		byte[] wire = serializationTester.serialize(in);
//		System.out.println("Character: "+Hex.encodeHexString(wire));

		byte[] wire = Hex.decodeHex(characterWire78);
		MessageWrapper<?> out = serializationTester.deserialize(wire);

		assertFalse(out.getMessage().isBinary());
		assertEquals(data, out.getMessage().asString());
		assertEquals(id, out.getId());
		assertNull(out.getCorrelationId());
		assertEquals(contextValue, out.getContext().get(contextKey));
	}


	@Test
	public void testDeserialization78CompatibilityBinary() throws Exception {
		byte[] data = "testdata voor messagewrapper".getBytes();
		String id = "fakeId";
		String contextKey = "messageWrapperContextItem";
		String contextValue = "fakeValue";

//		MessageWrapper in = new MessageWrapper();
//		in.setMessage(new Message(data));
//		in.setId(id);
//		in.getContext().put(contextKey, contextValue);
//		byte[] wire = serializationTester.serialize(in);
//		System.out.println("Bytes: "+Hex.encodeHexString(wire));

		byte[] wire = Hex.decodeHex(binaryWire78);
		MessageWrapper<?> out = serializationTester.deserialize(wire);

		assertTrue(out.getMessage().isBinary());
		assertEquals(new String(data), out.getMessage().asString());
		assertEquals(id, out.getId());
		assertNull(out.getCorrelationId());
		assertEquals(contextValue, out.getContext().get(contextKey));
	}

	@Test
	public void testDeserialization79CompatibilityCharacters() throws Exception {
		String data = "testdata voor messagewrapper";
		String id = "fakeId";
		String correlationId = "fakeCorrelationId";
		String contextKey = "messageWrapperContextItem";
		String contextValue = "fakeValue";

		byte[] wire = Hex.decodeHex(characterWire79);
		MessageWrapper<?> out = serializationTester.deserialize(wire);

		assertFalse(out.getMessage().isBinary());
		assertEquals(data, out.getMessage().asString());
		assertEquals(id, out.getId());
		assertEquals(correlationId, out.getCorrelationId());
		assertEquals(contextValue, out.getContext().get(contextKey));
	}


	@Test
	public void testDeserialization79CompatibilityBinary() throws Exception {
		byte[] data = "testdata voor messagewrapper".getBytes();
		String id = "fakeId";
		String correlationId = "fakeCorrelationId";
		String contextKey = "messageWrapperContextItem";
		String contextValue = "fakeValue";


		byte[] wire = Hex.decodeHex(binaryWire79);
		MessageWrapper<?> out = serializationTester.deserialize(wire);

		assertTrue(out.getMessage().isBinary());
		assertEquals(new String(data), out.getMessage().asString());
		assertEquals(id, out.getId());
		assertEquals(correlationId, out.getCorrelationId());
		assertEquals(contextValue, out.getContext().get(contextKey));
	}

	@Test
	public void testDeserialization80CompatibilityCharacters() throws Exception {
		String data = "testdata voor messagewrapper";
		String id = "fakeId";
		String correlationId = "fakeCorrelationId";
		String contextKey = "messageWrapperContextItem";
		String contextValue = "fakeValue";

		byte[] wire = Hex.decodeHex(characterWire80);
		MessageWrapper<?> out = serializationTester.deserialize(wire);

		assertFalse(out.getMessage().isBinary());
		assertEquals(data, out.getMessage().asString());
		assertEquals(id, out.getId());
		assertEquals(correlationId, out.getCorrelationId());
		assertEquals(contextValue, out.getContext().get(contextKey));
	}


	@Test
	public void testDeserialization80CompatibilityBinary() throws Exception {
		byte[] data = "testdata voor messagewrapper".getBytes();
		String id = "fakeId";
		String correlationId = "fakeCorrelationId";
		String contextKey = "messageWrapperContextItem";
		String contextValue = "fakeValue";


		byte[] wire = Hex.decodeHex(binaryWire80);
		MessageWrapper<?> out = serializationTester.deserialize(wire);

		assertTrue(out.getMessage().isBinary());
		assertEquals(new String(data), out.getMessage().asString());
		assertEquals(id, out.getId());
		assertEquals(correlationId, out.getCorrelationId());
		assertEquals(contextValue, out.getContext().get(contextKey));
	}
}
