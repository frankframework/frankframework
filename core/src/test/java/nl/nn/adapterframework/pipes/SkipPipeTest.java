package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineExit;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.pipes.SkipPipe;
import nl.nn.adapterframework.pipes.Stream2StringPipe;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.rules.ExpectedException;

/**
 * SkipPipe Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>Mar 12, 2020</pre>
 */
public class SkipPipeTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();
    protected IPipeLineSession session = new PipeLineSessionBase();
    protected AbstractPipe pipe = new SkipPipe();
    protected PipeLine pipeline;
    protected Adapter adapter;

    @Before
    public void setup() throws ConfigurationException {
        pipe.registerForward(new PipeForward("success",null));
        pipe.setName(pipe.getClass().getSimpleName()+" under test");
        pipeline = new PipeLine();
        pipeline.addPipe(pipe);
        PipeLineExit exit = new PipeLineExit();
        exit.setPath("exit");
        exit.setState("success");
        pipeline.registerPipeLineExit(exit);
        adapter = new Adapter();
        adapter.registerPipeLine(pipeline);
    }


    /**
     * Method: setSkip(int skip)
     */
    @Test
    public void testSetSkip() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setLength(int length)
     */
    @Test
    public void testSetLength() throws Exception {
//TODO: Test goes here... 
    }


} 
