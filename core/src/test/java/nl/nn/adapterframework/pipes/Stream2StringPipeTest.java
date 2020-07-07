package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Stream2StringPipe Tester.
 *
 * @author <Sina Sen>
 */
public class Stream2StringPipeTest extends PipeTestBase<Stream2StringPipe> {


    @Rule
    public ExpectedException exception = ExpectedException.none();
    protected IPipeLineSession session = new PipeLineSessionBase();
    protected AbstractPipe pipe = new Stream2StringPipe();

    /**
     * Method: doPipe(Object input, IPipeLineSession session)
     */
    @Test
    public void testDoPipeSuccess() throws Exception {
        String myString = "testString";
        InputStream is = new ByteArrayInputStream(myString.getBytes());
        Message m = new Message(is);
        PipeRunResult res = pipe.doPipe( m, session);
        assertEquals("testString", res.getResult().asString());
    }
  
    /**
     * Method: doPipe(Object input, IPipeLineSession session)
     */
    @Test
    public void testDoPipeFail() throws Exception {
        String myString = "testString";
        Message m = new Message(myString);
        PipeRunResult res = pipe.doPipe(m, session);
        assertEquals("testString", res.getResult().asString());
    }


    @Override
    public Stream2StringPipe createPipe() {
        return new Stream2StringPipe();
    }
}
