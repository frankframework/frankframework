package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

import org.frankframework.management.gateway.SerializableInputStream;

@Log4j2
public class SerializableInputStreamTest {

	@Test
	public void testIfAnInputStreamCanBeSerialized() throws Exception {
		String inputString = "input string";
		assertEquals(inputString, testSerialization(inputString), "strings should be equal to prove the testSerialization method works");

		InputStream data = new ByteArrayInputStream(inputString.getBytes());
		try (SerializableInputStream sis = new SerializableInputStream(data)) {
			InputStream serialized = testSerialization(sis);

			ByteArrayOutputStream boas = new ByteArrayOutputStream();
			IOUtils.copy(serialized, boas);
			serialized.close();

			assertArrayEquals(inputString.getBytes(), boas.toByteArray());
		}
	}

	@Test
	public void testLargeStream() throws Exception {
		URL testFile = SerializableInputStream.class.getResource("/25k-file.txt");
		Path fileThatLargerThen20Kb = Path.of(testFile.toURI());
		assertTrue(Files.size(fileThatLargerThen20Kb) > 20_480, "the test file must be larger then the default buffer size");

		byte[] serializedData;
		try(InputStream is = Files.newInputStream(fileThatLargerThen20Kb); SerializableInputStream sis = new SerializableInputStream(is)) {
			serializedData = serialize(sis);
		}

		Object deserialized = deserialize(serializedData);
		assertInstanceOf(InputStream.class, deserialized);

		try (InputStream expected = Files.newInputStream(fileThatLargerThen20Kb); InputStream result = (InputStream) deserialized) {
			assertArrayEquals(expected.readAllBytes(), result.readAllBytes());
		}
	}

	public byte[] serialize(Object in) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(baos)) {
			out.writeObject(in);
			return baos.toByteArray();
		} catch (IOException e) {
			System.out.println("problem serializing session attribute class [" + in.getClass().getName() + "]: (" + e.getClass().getName() + "): " + e.getMessage() + ". Value [" + ToStringBuilder.reflectionToString(in, ToStringStyle.MULTI_LINE_STYLE) + "]");
			throw e;
		}
	}

	public Object deserialize(byte[] wire) throws Exception {
		try (ByteArrayInputStream bais = new ByteArrayInputStream(wire); ObjectInputStream in = new ObjectInputStream(bais)) {
			return in.readObject();
		} catch (Exception e) {
			System.out.println("problem deserializing session attribute class (" + e.getClass().getName() + "): " + e.getMessage());
			throw e;
		}
	}

	@SuppressWarnings("unchecked")
	public <T> T testSerialization(T in) throws Exception {
		byte[] wire=serialize(in);
		if (wire==null) {
			throw new NullPointerException("Could not Serialize ["+ToStringBuilder.reflectionToString(in,ToStringStyle.MULTI_LINE_STYLE)+"]");
		}
		log.debug("Serialization wire for object type [{}]: [{}]", ()->in.getClass().getSimpleName(), ()->Hex.encodeHexString(wire));
		Object out=deserialize(wire);
		if (out==null) {
			throw new NullPointerException("Could not Deserialize ["+ToStringBuilder.reflectionToString(in,ToStringStyle.MULTI_LINE_STYLE)+"]");
		}
		return (T) out;
	}
}
