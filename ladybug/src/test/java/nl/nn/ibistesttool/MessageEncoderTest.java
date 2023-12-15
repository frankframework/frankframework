package nl.nn.ibistesttool;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import nl.nn.testtool.Checkpoint;

import org.junit.jupiter.api.Test;

public class MessageEncoderTest {

	@Test
	void testToObject() {
		Checkpoint cpt = new Checkpoint();

		MessageEncoder encoder = new MessageEncoder();
		Object object = encoder.toObject(cpt);
		assertNotNull(object);
	}
}
