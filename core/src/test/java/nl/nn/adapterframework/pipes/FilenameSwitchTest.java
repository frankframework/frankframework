package nl.nn.adapterframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hamcrest.Matchers;

import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import org.junit.jupiter.api.Test;

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
		assertEquals(pipe.getNotFoundForwardName(), "input_not_found");
		pipe.configure();
	}

	@Test
	public void testSetToLowercase() throws Exception {
		pipe.setToLowercase(true);
		assertEquals(pipe.isToLowercase(), true);
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
		PipeRunResult res = doPipe(pipe, "CreateHelloWorld/success", session);
		assertEquals("success", res.getPipeForward().getName());
	}

	@Test
	public void testValidForwardNameToLowerCase() throws Exception {
		pipe.setToLowercase(true);
		String input = "https:\\www.delft.nl/corona-besmettingsgeval-gevonden-in-delft/a\\SUCCESS";
		PipeRunResult res = doPipe(pipe, input, session);
		assertEquals("success", res.getPipeForward().getName());
	}

	@Test
	public void testValidForwardNameToLowerCaseFalse() throws Exception {
		pipe.setToLowercase(false);
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
