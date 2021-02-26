package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;

/**
 * Stream2StringPipe Tester.
 *
 * @author <Sina Sen>
 */
public class Stream2StringPipeTest extends PipeTestBase<Stream2StringPipe> {

    /**
     * Method: doPipe(Object input, IPipeLineSession session)
     */
    @Test
    public void testDoPipeSuccess() throws Exception {
        String myString = "testString";
        InputStream is = new ByteArrayInputStream(myString.getBytes());
        Message m = new Message(is);
        PipeRunResult res = doPipe(pipe, m, session);
        assertEquals("testString", res.getResult().asString());
    }
  
    /**
     * Method: doPipe(Object input, IPipeLineSession session)
     */
    @Test
    public void testDoPipeFail() throws Exception {
        String myString = "testString";
        Message m = new Message(myString);
        PipeRunResult res = doPipe(pipe, m, session);
        assertEquals("testString", res.getResult().asString());
    }


    @Override
    public Stream2StringPipe createPipe() {
        return new Stream2StringPipe();
    }
}
