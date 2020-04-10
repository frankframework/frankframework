package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;

/**
 * IncreaseIntegerPipe Tester.
 *
 * @author <Sina Sen>
 * @version 1.0
 * @since <pre>Mar 12, 2020</pre>
 */
public class IncreaseIntegerPipeTest extends PipeTestBase<IncreaseIntegerPipe> {

    @Mock
    private IPipeLineSession session = new PipeLineSessionBase();

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
        pipe.doPipe("doesnt matter", session);
        assertEquals(session.get("a"), "6");
    }

    @Test
    public void testCannotIncreaseBy2AsNoSessionKey() throws Exception {
        exception.expect(ConfigurationException.class);
        exception.expectMessage("sessionKey must be filled");
        session.put("a", "4");
        pipe.setIncrement(2);
        pipe.configure();
        pipe.doPipe("doesnt matter", session);
        assertEquals(session.get("a"), "6");
    }

}
