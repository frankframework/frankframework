package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.After;
import org.junit.rules.ExpectedException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Stream2StringPipe Tester.
 *
 * @author <Sina Sen>
 * @version 1.0
 * @since <pre>Mar 12, 2020</pre>
 */
public class Stream2StringPipeTest {


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
        PipeRunResult res = pipe.doPipe(is, session);
        assertEquals(res.getResult().toString(), "testString");
    }
    /**
     * Method: doPipe(Object input, IPipeLineSession session)
     */
    @Test
    public void testDoPipeFail() throws Exception {
        exception.expect(ClassCastException.class);
        String myString = "testString";
        PipeRunResult res = pipe.doPipe(myString, session);
        assertEquals(res.getResult().toString(), "testString");
    }


} 
