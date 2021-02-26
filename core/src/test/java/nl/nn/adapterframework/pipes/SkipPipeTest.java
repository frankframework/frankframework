package nl.nn.adapterframework.pipes;


import nl.nn.adapterframework.core.PipeRunResult;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


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
        assertEquals("34", res.getResult().asString());
    }

    @Test
    public void  testSkip3() throws Exception {
        pipe.setSkip(3);
        PipeRunResult res = doPipe(pipe, "0123456", session);
        assertEquals( "3456", res.getResult().asString());
    }

    @Test
    public void testRead2WithString() throws Exception {
        pipe.setLength(2);
        PipeRunResult res = doPipe(pipe, "0123456", session);
        assertEquals( "01", res.getResult().asString());
    }

    @Test
    public void testDoPipeWithByteArray() throws Exception {
        byte[] myvar = "Any String you want".getBytes(); pipe.setSkip(2);
        PipeRunResult res = doPipe(pipe, myvar, session);
        assertNotEquals( "", res.getResult().asString());
    }
    @Test
    public void testWrongInput() throws Exception {
        pipe.setSkip(2);
        PipeRunResult res = doPipe(pipe, 2, session);
        assertEquals( "SkipPipe doesn't work for this type of object", res.getResult().asString());
    }





}
