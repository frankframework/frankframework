package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;

/**
 * PutInSession Tester.
 *
 * @author <Sina Sen>
 * @version 1.0
 * @since <pre>Mar 19, 2020</pre>
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
    @Test
    public void testConfigureWithoutSessionKey() throws Exception {
        pipe.setSessionKey("hola");
        pipe.configure(); pipe.doPipe("val", session);
        assertEquals("val", session.get("hola"));

    }

    /**
     * Method: doPipe(Object input, IPipeLineSession session)
     */
    @Test
    public void testPutWithSessionKey() throws Exception {
        pipe.setSessionKey("hola"); pipe.setValue("val");
        pipe.configure(); pipe.doPipe("notimportant", session);
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
