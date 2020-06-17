package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.stream.Message;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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



    /**
     * Method: configure()
     */

    @Test
    public void testConfigureWithoutSessionKey() throws Exception {
        pipe.setSessionKey("hola");
        pipe.configure();
        doPipe(pipe, "val", session);
        Message m = new Message("String: val");
        assertEquals(session.get("hola").toString(), m.asString());
    }

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
        pipe.configure();
        pipe.doPipe(null, session);
        fail("this is expected to fail");
    }


}
