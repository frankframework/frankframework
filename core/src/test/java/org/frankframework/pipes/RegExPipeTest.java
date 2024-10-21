package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunResult;
import org.frankframework.util.CloseUtils;

public class RegExPipeTest extends PipeTestBase<RegExPipe> {

	private PipeRunResult pipeRunResult;

	@Override
	@AfterEach
	public void tearDown() {
		CloseUtils.closeSilently(pipeRunResult);
	}

	@Override
	public RegExPipe createPipe() throws ConfigurationException {
		RegExPipe pipe = new RegExPipe();

		//Add default pipes
		pipe.addForward(new PipeForward(RegExPipe.THEN_FORWARD, null));
		pipe.addForward(new PipeForward(RegExPipe.ELSE_FORWARD, null));
		return pipe;
	}

	@Test
	void testInvalidRegex() {
		pipe.setRegex("[");

		assertThrows(ConfigurationException.class, () -> pipe.configure());
	}

	@Test
	void testEmptyInput() throws Exception {
		//Arrange
		pipe.setRegex("(.*?)");

		pipe.configure();
		pipe.start();

		//Act
		pipeRunResult = doPipe(pipe, "", session);

		//Assert
		assertEquals(RegExPipe.ELSE_FORWARD, pipeRunResult.getPipeForward().getName());
		assertTrue(pipeRunResult.getResult().isNull());
	}

	@Test
	void testNullInput() throws Exception {
		//Arrange
		pipe.setRegex("(.*?)");

		pipe.configure();
		pipe.start();

		//Act
		pipeRunResult = doPipe(pipe, null, session);

		//Assert
		assertEquals(RegExPipe.ELSE_FORWARD, pipeRunResult.getPipeForward().getName());
		assertTrue(pipeRunResult.getResult().isNull());
	}

	@Test
	void testMatchWildcard() throws Exception {
		//Arrange
		pipe.setRegex("^(.*?)(string!)$");

		pipe.configure();
		pipe.start();

		//Act
		pipeRunResult = doPipe(pipe, "This is a string!", session);

		//Assert
		assertEquals(RegExPipe.THEN_FORWARD, pipeRunResult.getPipeForward().getName());

		final String expectedResult = "<matches>\n" +
				"\t<match index=\"1\" value=\"This is a string!\">\n" +
				"\t\t<group index=\"1\">This is a </group>\n" +
				"\t\t<group index=\"2\">string!</group>\n" +
				"\t</match>\n" +
				"</matches>";
		assertEquals(expectedResult, pipeRunResult.getResult().asString());
	}

	@Test
	void testFindSubString() throws Exception {
		//Arrange
		pipe.setRegex("hoi");

		pipe.configure();
		pipe.start();

		//Act
		pipeRunResult = doPipe(pipe, "hoi a hoi", session);

		//Assert
		assertEquals(RegExPipe.THEN_FORWARD, pipeRunResult.getPipeForward().getName());

		final String expectedResult = "<matches>\n" +
				"\t<match index=\"1\" value=\"hoi\"/>\n" +
				"\t<match index=\"2\" value=\"hoi\"/>\n" +
				"</matches>";
		assertEquals(expectedResult, pipeRunResult.getResult().asString());
	}

	@Test
	void testMultiline() throws Exception {
		//Arrange
		pipe.setRegex("string");
		pipe.setFlags(RegExPipe.RegExFlag.MULTILINE);

		pipe.configure();
		pipe.start();

		//Act
		pipeRunResult = doPipe(pipe, "string\nstring\nstring", session);

		//Assert
		assertEquals(RegExPipe.THEN_FORWARD, pipeRunResult.getPipeForward().getName());

		final String expectedResult = "<matches>\n" +
				"\t<match index=\"1\" value=\"string\"/>\n" +
				"\t<match index=\"2\" value=\"string\"/>\n" +
				"\t<match index=\"3\" value=\"string\"/>\n" +
				"</matches>";
		assertEquals(expectedResult, pipeRunResult.getResult().asString());
	}

	@Test
	void testCaseInsensitive() throws Exception {
		//Arrange
		pipe.setRegex("STRING.");
		pipe.setFlags(RegExPipe.RegExFlag.CASE_INSENSITIVE);

		pipe.configure();
		pipe.start();

		//Act
		pipeRunResult = doPipe(pipe, "stringA\nstringB\nstringC", session);

		//Assert
		assertEquals(RegExPipe.THEN_FORWARD, pipeRunResult.getPipeForward().getName());

		final String expectedResult = "<matches>\n" +
				"\t<match index=\"1\" value=\"stringA\"/>\n" +
				"\t<match index=\"2\" value=\"stringB\"/>\n" +
				"\t<match index=\"3\" value=\"stringC\"/>\n" +
				"</matches>";
		assertEquals(expectedResult, pipeRunResult.getResult().asString());
	}

	@Test
	void testMultilineAndCaseInsensitive() throws Exception {
		//Arrange
		pipe.setRegex("^This is a (.*?)$");
		pipe.setFlags(RegExPipe.RegExFlag.MULTILINE, RegExPipe.RegExFlag.CASE_INSENSITIVE);

		pipe.configure();
		pipe.start();

		//Act
		pipeRunResult = doPipe(pipe, "THIS IS A STRING", session);

		//Assert
		assertEquals(RegExPipe.THEN_FORWARD, pipeRunResult.getPipeForward().getName());

		final String expectedResult = "<matches>\n" +
				"\t<match index=\"1\" value=\"THIS IS A STRING\">\n" +
				"\t\t<group index=\"1\">STRING</group>\n" +
				"\t</match>\n" +
				"</matches>";
		assertEquals(expectedResult, pipeRunResult.getResult().asString());
	}

	@Test
	void testNoMatch() throws Exception {
		//Arrange
		pipe.setRegex("string string string");

		pipe.configure();
		pipe.start();

		//Act
		pipeRunResult = doPipe(pipe, "string string", session);

		//Assert
		assertEquals(RegExPipe.ELSE_FORWARD, pipeRunResult.getPipeForward().getName());
		assertTrue(pipeRunResult.getResult().isNull());
	}

}
