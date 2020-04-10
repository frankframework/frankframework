package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPostboxListener;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.jms.PullingJmsListener;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;

/**
 * PostboxRetrieverPipe Tester.
 *
 * @author <Sina Sen>
 * @version 1.0
 * @since <pre>Mar 20, 2020</pre>
 */
public class PostboxRetrieverPipeTest extends PipeTestBase<PostboxRetrieverPipe> {

    @Mock
    private PipeLineSessionBase session = new PipeLineSessionBase();

    static IPostboxListener listener = new PullingJmsListener();

    @Override
    public PostboxRetrieverPipe createPipe() {
        return new PostboxRetrieverPipe();
    }

    @After
    public void after() throws Exception {

    }

    /**
     * Method: configure()
     */
    @Test
    public void testConfigureWithoutListener() throws Exception {
        exception.expect(ConfigurationException.class);
        exception.expectMessage("no sender defined");
        pipe.configure();
    }



    /**
     * Method: getResultOnEmptyPostbox()
     */
    @Test
    public void testGetResultOnEmptyPostbox() throws Exception {
        //listener.openThread();
        listener.configure(); listener.open();
        pipe.setListener(listener);
        pipe.configure(); pipe.doPipe("trial", session);
    }

    /**
     * Method: setResultOnEmptyPostbox(String string)
     */
    @Test
    public void testSetResultOnEmptyPostbox() throws Exception {
//TODO: Test goes here... 
    }


}
