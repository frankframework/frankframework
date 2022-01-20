package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.testutil.ParameterBuilder;

/**
 * IncreaseIntegerPipe Tester.
 *
 * @author <Sina Sen>
 */
public class IncreaseIntegerPipeTest extends PipeTestBase<IncreaseIntegerPipe> {


    @Override
    public IncreaseIntegerPipe createPipe() {
        return new IncreaseIntegerPipe();
    }

    /**
     * Method: doPipe(Object input, PipeLineSession session)
     */
    @Test
    public void testIncreaseBy2() throws Exception {

        session.put("a", "4");
        pipe.setSessionKey("a");
        pipe.setIncrement(2);
        pipe.configure();
        doPipe(pipe, "doesnt matter", session);
        assertEquals("6", session.get("a"));
    }

    @Test
    public void testCannotIncreaseBy2AsNoSessionKey() throws Exception {
        exception.expect(ConfigurationException.class);
        exception.expectMessage("sessionKey must be filled");
        session.put("a", "4");
        pipe.setIncrement(2);
        pipe.configure();
        doPipe(pipe, "doesnt matter", session);
        fail("this is expected to fail");
    }

    @Test
    public void testIncrementParameter() throws Exception {
    	String numberSession = "number";
		session.put(numberSession, "4");
		pipe.addParameter(new ParameterBuilder("increment", "5"));
		pipe.setSessionKey(numberSession);
		pipe.configure();
		doPipe(pipe, "message", session);
		assertEquals("9", session.get(numberSession));
    }

    @Test
    public void testNullIncrementParameter() throws Exception {
    	String numberSession = "number";
		session.put(numberSession, "4");
		pipe.addParameter(new ParameterBuilder("increment", null));
		pipe.setSessionKey(numberSession);
		pipe.configure();
		doPipe(pipe, null, session);
		assertEquals("5", session.get(numberSession));
    }

    @Test
    public void testEmptyIncrementParameter() throws Exception {
		Exception exception = assertThrows(NumberFormatException.class, () -> {
			String numberSession = "number";
			session.put(numberSession, "4");
			pipe.addParameter(new ParameterBuilder("increment", ""));
			pipe.setSessionKey(numberSession);
			pipe.configure();
			doPipe(pipe, "", session);
		});
		String expectedMessage = "For input string";
		String actualMessage = exception.getMessage();

		assertTrue(actualMessage.contains(expectedMessage));
    }

}
