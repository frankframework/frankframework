package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.pipes.SkipPipe;
import nl.nn.adapterframework.pipes.Stream2StringPipe;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.rules.ExpectedException;
import static org.junit.Assert.assertEquals;

/**
 * SkipPipe Tester.
 *
 * @author <Sina Sen>
 * @version 1.0
 * @since <pre>Mar 12, 2020</pre>
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
        PipeRunResult res = pipe.doPipe("0123456", session);
        assertEquals(res.getResult().toString(), "34");
    }

    @Test
    public void testDoPipeWithByteArray() throws Exception {
        byte[] myvar = "Any String you want".getBytes(); pipe.setSkip(2);
        PipeRunResult res = pipe.doPipe(myvar, session);
        assertEquals(res.getResult().toString(), "[B@eb21112");

    }
    @Test
    public void testWrongInput() throws Exception {
        pipe.setSkip(2);
        PipeRunResult res = pipe.doPipe(2, session);
        assertEquals(res.getResult().toString(), "SkipPipe doesn't work for this type of object");
    }





}
