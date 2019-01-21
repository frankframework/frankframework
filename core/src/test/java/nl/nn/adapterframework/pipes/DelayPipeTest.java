package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import org.junit.Test;

import static org.junit.Assert.*;

public class DelayPipeTest extends PipeTestBase<DelayPipe> {

    private IPipeLineSession session = new PipeLineSessionBase();

    @Override
    public DelayPipe createPipe() {
        return new DelayPipe();
    }

    @Test
    public void getterSetterDelayTime() {
        long dummyTime = 1337;
        pipe.setDelayTime(dummyTime);
        assertEquals(pipe.getDelayTime(), dummyTime);
    }

    @Test
    public void testUnInterruptedSession() throws PipeRunException {
        Object input = "dummyInput";
        pipe.setDelayTime(1000);
        assertEquals(input, pipe.doPipe(input, session).getResult().toString());
    }
}