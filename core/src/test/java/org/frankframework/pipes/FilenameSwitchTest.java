package org.frankframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;

/**
 * FilenameSwitch Tester.
 *
 * @author <Sina Sen>
 */
public class FilenameSwitchTest extends PipeTestBase<FilenameSwitch> {

	@Override
	public FilenameSwitch createPipe() {
		return new FilenameSwitch();
	}

	@Test
	public void testGetSetNotFoundForwardName() throws Exception {
		pipe.setNotFoundForwardName("input_not_found");
		assertEquals("input_not_found", pipe.getNotFoundForwardName());
		pipe.configure();
	}

	@Test
	public void testSetToLowercase() throws Exception {
		pipe.setToLowercase(true);
		assertTrue(pipe.isToLowercase());
		pipe.configure();
	}

	@Test
	public void testConfigureWithoutForwardNameAndWithoutAlternativeForward() throws Exception {
		pipe.setNotFoundForwardName(null);
		pipe.configure();

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, "", session));
		assertThat(e.getMessage(), Matchers.endsWith("cannot find forward or pipe named []"));
	}

	@Test
	public void testConfigureWithNullForwardName() throws Exception {
		pipe.configure();
		assertThrows(NullPointerException.class, ()->doPipe(pipe, null, session));
	}

	@Test
	public void testValidForwardName() throws Exception {
		pipe.configure();
		PipeRunResult res = doPipe(pipe, "CreateHelloWorld/success", session);
		assertEquals("success", res.getPipeForward().getName());
	}

	@Test
	public void testValidForwardNameToLowerCase() throws Exception {
		pipe.setToLowercase(true);
		pipe.configure();
		String input = "https:\\www.delft.nl/corona-besmettingsgeval-gevonden-in-delft/a\\SUCCESS";
		PipeRunResult res = doPipe(pipe, input, session);
		assertEquals("success", res.getPipeForward().getName());
	}

	@Test
	public void testValidForwardNameToLowerCaseFalse() throws Exception {
		pipe.setToLowercase(false);
		pipe.configure();
		String input = "https:\\www.delft.nl/corona-besmettingsgeval-gevonden-in-delft/a\\SUCCESS";

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, input, session));
		assertThat(e.getMessage(), Matchers.endsWith("cannot find forward or pipe named [SUCCESS]"));
	}

	@Test
	public void testWorkWithNothFoundForwardName() throws Exception {
		pipe.setNotFoundForwardName("success");
		pipe.configure();
		PipeRunResult res = doPipe(pipe,
				"https:\\www.delft.nl\\/corona-besmettingsgeval-gevonden-in-delft/asdSUCCasdESS", session);
		assertEquals("success", res.getPipeForward().getName());
	}

}
