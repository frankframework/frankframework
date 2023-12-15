package nl.nn.adapterframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.pipes.EtagHandlerPipe.EtagAction;

public class EtagHandlerPipeTest extends PipeTestBase<EtagHandlerPipe> {

	@Override
	public EtagHandlerPipe createPipe() {
		return new EtagHandlerPipe();
	}

	@Test
	public void getterSetterUriPattern() {
		String dummyString = "dummyString";
		pipe.setUriPattern(dummyString);
		assertEquals(dummyString.toLowerCase(), pipe.getUriPattern().toLowerCase());

		pipe.setUriPattern(null);
		assertNull(pipe.getUriPattern());
	}

	@Test
	public void getterSetterRestPath() {
		String dummyString = "dummyString";
		pipe.setRestPath(dummyString);
		assertEquals(dummyString, pipe.getRestPath());

		pipe.setRestPath(null);
		assertNull(pipe.getRestPath());
	}

	@Test
	public void testNoActionGiven() {
		pipe.setAction(null);
		assertThrows(ConfigurationException.class, () -> pipe.configure());
	}

	@Test
	public void testNoUriPatternGiven() {
		pipe.setAction(EtagAction.GENERATE);
		assertThrows(ConfigurationException.class, () -> pipe.configure());
	}

	@Test
	public void testInputNull() {
		assertThrows(PipeRunException.class, () -> doPipe(pipe, null, session));
	}

	@Test
	public void testWrongInputFormat() {
		assertThrows(PipeRunException.class, () -> doPipe(pipe, 5000, session));
	}

	@Test
	public void testFailedToLocateCache()  {
		assertThrows(PipeRunException.class, () -> doPipe(pipe, "dummyString", session));
	}

	@Test
	public void testFailedToLocateEtag() throws ConfigurationException {
		pipe.setAction(EtagAction.GENERATE);
		pipe.setUriPattern("dummyPattern");
		pipe.configure();
		assertThrows(PipeRunException.class, () -> doPipe(pipe, "dummyString", session));
	}

}
