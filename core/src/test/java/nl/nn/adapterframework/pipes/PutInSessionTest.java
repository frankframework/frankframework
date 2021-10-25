package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.parameters.Parameter;

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

		assertEquals(expected, session.getMessage("hola").asString());
	}

	@Test
	public void testPutWithValue() throws Exception {
		pipe.setSessionKey("hola");
		pipe.setValue("val");
		pipe.configure();
		doPipe(pipe, "notimportant", session);
		assertEquals("val", session.getMessage("hola").asString());
	}

	@Test
	public void testSessionKeyWithOneParam() throws Exception {
		pipe.setValue("value");
		pipe.setSessionKey("sessionKey");
		Parameter p = new Parameter();
		p.setName("param");
		p.setValue("test");
		pipe.addParameter(p);

		pipe.configure();
		pipe.doPipe(null, session);

		assertEquals("value", session.getMessage("sessionKey").asString());
		assertEquals("test", session.getMessage("param").asString());
	}
	
	@Test
	public void testParamFromConfiguredSessionKey() throws Exception {
		pipe.setValue("value");
		pipe.setSessionKey("sessionKey");
		Parameter p = new Parameter();
		p.setName("param");
		p.setSessionKey("sessionKey");
		pipe.addParameter(p);

		pipe.configure();
		pipe.doPipe(null, session);

		assertEquals("value", session.getMessage("sessionKey").asString());
		assertEquals("value", session.getMessage("param").asString());
	}
	
	@Test
	public void testWithOneParam() throws Exception {
		Parameter p = new Parameter();
		p.setName("param");
		p.setValue("test");
		pipe.addParameter(p);

		pipe.configure();
		pipe.doPipe(null, session);

		assertEquals("test", session.getMessage("param").asString());
	}

}
