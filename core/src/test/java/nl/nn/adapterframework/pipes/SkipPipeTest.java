package nl.nn.adapterframework.pipes;


import nl.nn.adapterframework.core.PipeRunResult;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * SkipPipe Tester.
 *
 * @author <Sina Sen>
 */
public class SkipPipeTest extends PipeTestBase<SkipPipe>{



    @Override
    public SkipPipe createPipe() {
        return new SkipPipe();
    }

    /**
     * Method: setSkip(int skip)
     */
    @Test
    public void testDoPipeSkip3Read2WithString() throws Exception {
        pipe.setSkip(3); pipe.setLength(2);
        PipeRunResult res = doPipe(pipe, "0123456", session);
        assertEquals(res.getResult().toString(), "String: 34");
    }

    @Test
    public void testDoPipeWithByteArray() throws Exception {
        byte[] myvar = "Any String you want".getBytes(); pipe.setSkip(2);
        PipeRunResult res = doPipe(pipe, myvar, session);
        assertNotEquals(res.getResult().toString(),  "");

    }
    @Test
    public void testWrongInput() throws Exception {
        pipe.setSkip(2);
        PipeRunResult res = doPipe(pipe, 2, session);
        assertEquals(res.getResult().toString(), "String: SkipPipe doesn't work for this type of object");
    }





}
