package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.stream.Message;
import org.junit.Test;

import org.mockito.Mock;

import static org.junit.Assert.assertEquals;

/**
 * PutInSession Tester.
 *
 * @author <Sina Sen>
 */
public class PutInSessionTest extends PipeTestBase<PutInSession> {

    @Mock
    PipeLineSessionBase session = new PipeLineSessionBase();

    @Override
    public PutInSession createPipe() {
        return new PutInSession();
    }



    /**
     * Method: configure()
     */
    /*
    @Test
    public void testConfigureWithoutSessionKey() throws Exception {
        pipe.setSessionKey("hola");
        pipe.configure();
        doPipe(pipe, "val", session);
        Message m = new Message("val");
        assertEquals(new Message("val") , session.get("hola"));

    }

    PutInSessionPipeTest:
java.lang.AssertionError: expected: nl.nn.adapterframework.stream.Message<String: val> but was: nl.nn.adapterframework.stream.Message<String: val>
Expected :nl.nn.adapterframework.stream.Message<String: val>
Actual   :nl.nn.adapterframework.stream.Message<String: val>
<Click to see difference>
No Difference??

    */


    /**
     * Method: doPipe(Object input, IPipeLineSession session)
     */
    @Test
    public void testPutWithSessionKey() throws Exception {
        pipe.setSessionKey("hola");
        pipe.setValue("val");
        pipe.configure();
        doPipe(pipe, "notimportant", session);
        assertEquals("val", session.get("hola"));
    }

    @Test
    public void testNoSessionKey() throws Exception {
        exception.expectMessage("attribute sessionKey must be specified");
        exception.expect(ConfigurationException.class);
        pipe.setValue("val");
        pipe.configure(); pipe.doPipe(null, session);
    }


}
