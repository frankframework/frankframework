package org.frankframework.testutil;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.frankframework.util.RenamingObjectInputStream;

public class SerializationTester<T> {
	private final Logger log = LogManager.getLogger(SerializationTester.class);

	public byte[] serialize(T in) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(baos)) {
			out.writeObject(in);
			return baos.toByteArray();
		} catch (IOException e) {
			System.out.println("problem serializing session attribute class [" + in.getClass().getName() + "]: (" + e.getClass().getName() + "): " + e.getMessage() + ". Value [" + ToStringBuilder.reflectionToString(in, ToStringStyle.MULTI_LINE_STYLE) + "]");
			throw e;
		}
	}

	public T deserialize(byte[] wire) throws Exception {
		assumeTrue(TestAssertions.isRunningWithAddOpens(), "skipped test because --add-opens has not been set");

		try (ByteArrayInputStream bais = new ByteArrayInputStream(wire); ObjectInputStream in = new RenamingObjectInputStream(bais)) {
			Object obj = in.readObject();
			return (T) obj;
		} catch (Exception e) {
			System.out.println("problem deserializing session attribute class (" + e.getClass().getName() + "): " + e.getMessage());
			throw e;
		}
	}

	public T testSerialization(T in) throws Exception {
		byte[] wire=serialize(in);
		if (wire==null) {
			throw new NullPointerException("Could not Serialize ["+ToStringBuilder.reflectionToString(in,ToStringStyle.MULTI_LINE_STYLE)+"]");
		}
		log.debug("Serialization wire for object type [{}]: [{}]", ()->in.getClass().getSimpleName(), ()->Hex.encodeHexString(wire));
		T out=deserialize(wire);
		if (out==null) {
			throw new NullPointerException("Could not Deserialize ["+ToStringBuilder.reflectionToString(in,ToStringStyle.MULTI_LINE_STYLE)+"]");
		}
		return out;
	}
}
