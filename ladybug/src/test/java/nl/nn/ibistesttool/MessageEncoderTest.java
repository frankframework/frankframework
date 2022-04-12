package nl.nn.ibistesttool;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import nl.nn.testtool.Checkpoint;

public class MessageEncoderTest {

	@Test
	public void testToObject() {
		Checkpoint cpt = new Checkpoint();

		MessageEncoder encoder = new MessageEncoder();
		Object object = encoder.toObject(cpt);
		assertNotNull(object);
	}
}
