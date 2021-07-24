package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 * PutInSession Tester.
 *
 * @author <Sina Sen>
 */
public class PutInSessionTest extends PipeTestBase<PutInSession> {

	@Override
	public PutInSession createPipe() {
		return new PutInSession();
	}


	@Test
	public void testConfigureWithoutValue() throws Exception {
		pipe.setSessionKey("hola");
		pipe.configure();

		String message = "val";
		String expected = message;
		doPipe(pipe, message, session);

		assertEquals(expected, session.get("hola")); // must be type String and have this value 
	}

	@Test
	public void testPutWithValue() throws Exception {
		pipe.setSessionKey("hola");
		pipe.setValue("val");
		pipe.configure();
		doPipe(pipe, "notimportant", session);
		assertEquals("val", session.get("hola")); // must be type String and have this value 
	}

	@Test
	public void testNoSessionKey() throws Exception {
		exception.expectMessage("attribute sessionKey must be specified");
		exception.expect(ConfigurationException.class);
		pipe.setValue("val");
		pipe.configure();
		pipe.doPipe(null, session);
		fail("this is expected to fail");
	}

}
