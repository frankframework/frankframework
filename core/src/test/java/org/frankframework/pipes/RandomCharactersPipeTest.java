package org.frankframework.pipes;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.frankframework.pipes.RandomCharactersPipe.Type.ALPHANUMERIC;
import static org.frankframework.pipes.RandomCharactersPipe.Type.ALPHANUMERIC_UPPERCASE;
import static org.frankframework.pipes.RandomCharactersPipe.Type.NUMERIC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RandomCharactersPipeTest extends PipeTestBase<RandomCharactersPipe> {

	private final Object input = new Object();

	@Override
	public RandomCharactersPipe createPipe() {
		return new RandomCharactersPipe();
	}

	@Test
	void testTypeIsNumeric() throws ConfigurationException, PipeRunException, IOException {
		pipe.setType(NUMERIC);
		pipe.configure();
		PipeRunResult pipeRunResult = doPipe(pipe, input, session);
		String result = pipeRunResult.getResult().asString();
		assertNotNull(result);
		assertEquals(6, result.length());
		assertTrue(result.matches("[0-9]+"));
	}

	@Test
	void testTypeIsNumericAndLengthIsTen() throws ConfigurationException, PipeRunException, IOException {
		pipe.setType(NUMERIC);
		pipe.setLength(10);
		pipe.configure();
		PipeRunResult pipeRunResult = doPipe(pipe, input, session);
		String result = pipeRunResult.getResult().asString();
		assertNotNull(result);
		assertEquals(10, result.length());
		assertTrue(result.matches("[0-9]+"));
	}

	@Test
	void testTypeIsAlphanumeric() throws ConfigurationException, PipeRunException, IOException {
		pipe.configure();
		PipeRunResult pipeRunResult = doPipe(pipe, input, session);
		String result = pipeRunResult.getResult().asString();
		assertNotNull(result);
		assertEquals(6, result.length());
		assertTrue(result.matches("[a-zA-Z 0-9]+"));
	}

	@Test
	void testTypeIsAlphanumericAndLengthIsTen() throws ConfigurationException, PipeRunException, IOException {
		pipe.setType(ALPHANUMERIC);
		pipe.setLength(10);
		pipe.configure();
		PipeRunResult pipeRunResult = doPipe(pipe, input, session);
		String result = pipeRunResult.getResult().asString();
		assertNotNull(result);
		assertEquals(10, result.length());
		assertTrue(result.matches("[a-zA-Z0-9]+"));
	}

	@Test
	void testTypeIsAlphanumericUppercase() throws ConfigurationException, PipeRunException, IOException {
		pipe.setType(ALPHANUMERIC_UPPERCASE);
		pipe.configure();
		PipeRunResult pipeRunResult = doPipe(pipe, input, session);
		String result = pipeRunResult.getResult().asString();
		assertNotNull(result);
		assertEquals(6, result.length());
		assertTrue(result.matches("[A-Z 0-9]+"));
	}

	@Test
	void testTypeIsAlphanumericUppercaseAndLengthIsTen() throws ConfigurationException, PipeRunException, IOException {
		pipe.setType(ALPHANUMERIC_UPPERCASE);
		pipe.setLength(10);
		pipe.configure();
		PipeRunResult pipeRunResult = doPipe(pipe, input, session);
		String result = pipeRunResult.getResult().asString();
		assertNotNull(result);
		assertEquals(10, result.length());
		assertTrue(result.matches("[A-Z0-9]+"));
	}

	@Test
	void testTypeIsAlphanumericAndLengthIsThousand() throws ConfigurationException, PipeRunException, IOException {
		pipe.setType(ALPHANUMERIC);
		pipe.setLength(1000);
		pipe.configure();
		String message = assertThrows(PipeRunException.class, () -> doPipe(pipe, input, session)).getMessage();
		assertEquals("Pipe [RandomCharactersPipe under test] Length is greater than 999", message);
	}
}
