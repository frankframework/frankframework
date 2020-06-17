package nl.nn.adapterframework.pipes;

<<<<<<< HEAD
=======
import nl.nn.adapterframework.core.PipeForward;
>>>>>>> 26a7f1065d4e5e85843a1261c06f2690ebde620e
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.PasswordHash;
import org.junit.Test;
<<<<<<< HEAD
import org.junit.Before;
import org.junit.After;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
=======
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
>>>>>>> 26a7f1065d4e5e85843a1261c06f2690ebde620e

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

<<<<<<< HEAD

=======
>>>>>>> 26a7f1065d4e5e85843a1261c06f2690ebde620e
    /**
     * Method: doPipe(Object input, IPipeLineSession session)
     */
    @Test
    public void testHashPipe() throws Exception {
<<<<<<< HEAD
        session.put("key", "3:2342:2342" );
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "password", session);
        assertEquals("success", res.getPipeForward().getName());
=======
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "password", session);
        assertTrue(PasswordHash.validatePassword("password", res.getResult().asString()));
        int hashLength = res.getResult().asString().length();
        assertEquals("success", res.getPipeForward().getName());
        assertTrue(hashLength == 135);
>>>>>>> 26a7f1065d4e5e85843a1261c06f2690ebde620e
    }

    @Test
    public void testValidatePipe() throws Exception {
<<<<<<< HEAD

        String sc = PasswordHash.createHash("password");
        session.put("key", sc );
        pipe.setHashSessionKey("key");
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "password", session);
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

=======
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "password", session);
        assertTrue(PasswordHash.validatePassword("password", res.getResult().asString()));
        assertEquals("success", res.getPipeForward().getName());
    }
    @Test
    public void testValidatePipeFailAsNotTheSame() throws Exception {
        String hashed = PasswordHash.createHash("password");
        session.put("key", hashed+"2132"); // this will make test fail as validation of the hash and the paswword will not be the same
        pipe.setHashSessionKey("key");
        pipe.configure();
        pipe.registerForward(new PipeForward("failure", "random/path"));
        PipeRunResult res = doPipe(pipe, "password", session);
        assertEquals("failure", res.getPipeForward().getName());
    }

    @Test
    public void testValidatePassAsTheSame() throws Exception {
        String hashed = PasswordHash.createHash("password");
        session.put("key", hashed); // this will make test fail as validation of the hash and the paswword will not be the same
        pipe.setHashSessionKey("key");
        pipe.configure();
        pipe.registerForward(new PipeForward("failure", "random/path"));
        PipeRunResult res = doPipe(pipe, "password", session);
        assertEquals("success", res.getPipeForward().getName());
    }

    @Test
    public void testTwoHashesNotTheSame() throws Exception {
        pipe.configure();

        PipeRunResult res1 = doPipe(pipe, "a", session);
        PipeRunResult res2 = doPipe(pipe, "a", session);
        assertNotEquals(res1.getResult().asString(), res2.getResult().asString());
    }
>>>>>>> 26a7f1065d4e5e85843a1261c06f2690ebde620e



}
