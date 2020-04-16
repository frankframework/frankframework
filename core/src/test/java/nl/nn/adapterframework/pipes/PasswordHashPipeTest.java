package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.PasswordHash;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.mockito.Mock;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static nl.nn.adapterframework.util.PasswordHash.PBKDF2_ALGORITHM;
import static org.junit.Assert.assertEquals;

/**
 * PasswordHashPipe Tester.
 *
 * @author <Sina>
 * @version 1.0
 * @since <pre>Apr 12, 2020</pre>
 */
public class PasswordHashPipeTest extends PipeTestBase<PasswordHashPipe> {

    @Mock
    private PipeLineSessionBase session = new PipeLineSessionBase();

    @Override
    public PasswordHashPipe createPipe() {
        return new PasswordHashPipe();
    }

    @Before
    public void before() throws Exception {
    }

    @After
    public void after() throws Exception {
    }

    /**
     * Method: doPipe(Object input, IPipeLineSession session)
     */
    @Test
    public void testHashPipe() throws Exception {
        session.put("key", "3:2342:2342" );
        pipe.configure();
        PipeRunResult res = pipe.doPipe("password", session);
        assertEquals("success", res.getPipeForward().getName());
    }

    @Test
    public void testValidatePipe() throws Exception {

        String sc = PasswordHash.createHash("password");
        session.put("key", sc ); pipe.setHashSessionKey("key");
        pipe.configure();
        PipeRunResult res = pipe.doPipe("password", session);
        assertEquals("success", res.getPipeForward().getName());
    }

    @Test
    public void testValidatePipeFailAsNotTheSame() throws Exception {

        session.put("key", "2:22:22"); pipe.setHashSessionKey("key");
        pipe.configure();
        PipeRunResult res = pipe.doPipe("password", session);
        assertEquals(null, res.getPipeForward());
    }




}
