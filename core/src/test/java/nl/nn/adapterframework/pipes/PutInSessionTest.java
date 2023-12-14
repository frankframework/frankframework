package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.testutil.ParameterBuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

		assertEquals(expected, session.getString("hola"));
	}

	@Test
	public void testPutWithValue() throws Exception {
		pipe.setSessionKey("hola");
		pipe.setValue("val");
		pipe.configure();
		doPipe(pipe, "notimportant", session);
		assertEquals("val", session.getString("hola"));
	}

	@Test
	public void testSessionKeyWithOneParam() throws Exception {
		pipe.setValue("value");
		pipe.setSessionKey("sessionKey");
		pipe.addParameter(new Parameter("param", "test"));

		pipe.configure();
		pipe.doPipe(null, session);

		assertEquals("value", session.getString("sessionKey"));
		assertEquals("test", session.getString("param"));
	}

	@Test
	public void testParamFromConfiguredSessionKey() throws Exception {
		pipe.setValue("value");
		pipe.setSessionKey("sessionKey");
		pipe.addParameter(ParameterBuilder.create().withName("param").withSessionKey("sessionKey"));

		pipe.configure();
		pipe.doPipe(null, session);

		assertEquals("value", session.getString("sessionKey"));
		assertEquals("value", session.getString("param"));
	}

	@Test
	public void testWithOneParam() throws Exception {
		pipe.addParameter(new Parameter("param", "test"));

		pipe.configure();
		pipe.doPipe(null, session);

		assertEquals("test", session.getString("param"));
	}

}
