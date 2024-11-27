package org.frankframework.ladybug;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import nl.nn.testtool.Checkpoint;

public class MessageEncoderTest {

	@Test
	void testToObject() {
		Checkpoint cpt = new Checkpoint();

		MessageEncoder encoder = new MessageEncoder();
		Object object = encoder.toObject(cpt);
		assertNotNull(object);
	}
}
