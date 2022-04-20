package nl.nn.adapterframework.testutil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class SerializationTester<T> {

	public byte[] serialize(T in) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ObjectOutputStream out = new ObjectOutputStream(baos);
			out.writeObject(in);
			out.close();
			baos.close();
			return baos.toByteArray();
		} catch (IOException e) {
			System.out.println("problem serializing session attribute class [" + in.getClass().getName() + "]: (" + e.getClass().getName() + "): "+e.getMessage()+". Value ["+ToStringBuilder.reflectionToString(in,ToStringStyle.MULTI_LINE_STYLE)+"]");
			throw e;
		}
	}
	
	public T deserialize(byte[] wire) throws Exception {
		ByteArrayInputStream bais = new ByteArrayInputStream(wire);
		try {
			ObjectInputStream in = new ObjectInputStream(bais);
			Object obj=in.readObject();
			in.close();
			bais.close();
			return (T)obj;
		} catch (Exception e) {
			System.out.println("problem deserializing session attribute class (" + e.getClass().getName() + "): "+e.getMessage());
			throw e;
		}
	}

	public T testSerialization(T in) throws Exception {
		byte[] wire=serialize(in);
		if (wire==null) {
			throw new NullPointerException("Could not Serialize ["+ToStringBuilder.reflectionToString(in,ToStringStyle.MULTI_LINE_STYLE)+"]");
		}
		T out=deserialize(wire);
		if (out==null) {
			throw new NullPointerException("Could not Deserialize ["+ToStringBuilder.reflectionToString(in,ToStringStyle.MULTI_LINE_STYLE)+"]");
		}
		return out;
	}

	
	
}
