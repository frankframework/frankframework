package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import org.junit.Test;

import static org.junit.Assert.*;

public class CounterSwitchPipeTest extends PipeTestBase<CounterSwitchPipe>{


    private IPipeLineSession session = new PipeLineSessionBase();

    @Override
    public CounterSwitchPipe createPipe() {
        CounterSwitchPipe pipe = new CounterSwitchPipe();
        pipe.registerForward(new PipeForward("1",null));
        pipe.registerForward(new PipeForward("2",null));
        pipe.registerForward(new PipeForward("",null));
        return pipe;
    }

    @Test
    public void getterSetterDivisor() {
        int dummyDivisor = 1337;
        pipe.setDivisor(1337);
        int otherDivisor = pipe.getDivisor();
        assertEquals(dummyDivisor, otherDivisor);
    }

    @Test(expected = ConfigurationException.class)
    public void testDivisorLessThanTwo() throws Exception {
        pipe.setDivisor(1);
        configureAndStartPipe();
        
    }

    @Test(expected = PipeRunException.class)
    public void testLegitimateDivisor() throws Exception {
        configureAndStartPipe();
        doPipe(pipe, "dummy", session);
    }

    @Test(expected = ConfigurationException.class)
    public void testNonExistingForward() throws Exception {
        pipe.setDivisor(3);
        configureAndStartPipe();
    }
}