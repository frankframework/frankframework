package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.PasswordHash;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * PasswordHashPipe Tester.
 * @author <Sina>
 */
public class PasswordHashPipeTest extends PipeTestBase<PasswordHashPipe> {

    @Mock
    private PipeLineSessionBase session = new PipeLineSessionBase();

    @Override
    public PasswordHashPipe createPipe() {
        return new PasswordHashPipe();
    }




    /**
     * Method: doPipe(Object input, IPipeLineSession session)
     */
    @Test
    public void testHashPipe() throws Exception {
        session.put("key", "3:2342:2342" );
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "password", session);
        assertEquals("success", res.getPipeForward().getName());
    }

    @Test
    public void testValidatePipe() throws Exception {
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "password", session);
        assertTrue(PasswordHash.validatePassword("password", res.getResult().asString()));
        assertEquals("success", res.getPipeForward().getName());
    }
    @Test
    public void testValidatePipeFailAsNotTheSame() throws Exception {

        session.put("key", "2:22:22");
        pipe.setHashSessionKey("key");
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "password", session);
        assertEquals(null, res.getPipeForward());
    }

    @Test
    public void testTwoHashesNotTheSame() throws Exception {
        pipe.configure();

        PipeRunResult res1 = doPipe(pipe, "a", session);
        PipeRunResult res2 = doPipe(pipe, "a", session);
        assertNotEquals(res1.getResult().asString(), res2.getResult().asString());
    }



}
