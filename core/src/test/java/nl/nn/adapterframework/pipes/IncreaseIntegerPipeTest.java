package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;

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
     * Method: doPipe(Object input, IPipeLineSession session)
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

}
