package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import org.junit.Test;
import org.mockito.Mock;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

public class CrlPipeTest extends PipeTestBase<CrlPipe> {



    private byte[] var1 = "Any String you want".getBytes();
    private byte[] var2 = "Some String you want".getBytes();
    private ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(var1);
    private ByteArrayInputStream byteArrayInputStream2 = new ByteArrayInputStream(var2);

    @Mock
    private IPipeLineSession session = new PipeLineSessionBase();

    @Override
    public CrlPipe createPipe() {
        return new CrlPipe();
    }

    /**
     * Fails because generateCrl method is not implemented
     * @throws Exception
     */
    @Test
    public void testCrlPipeEmptyInputException() throws Exception {
        exception.expectMessage("Could not read CRL: (CRLException) Empty input");
        exception.expect(PipeRunException.class);
        session.put("first", byteArrayInputStream);
        pipe.setIssuerSessionKey("first");
        pipe.doPipe(byteArrayInputStream2, session);
    }



}
