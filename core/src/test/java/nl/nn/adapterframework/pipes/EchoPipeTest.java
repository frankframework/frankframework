package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import org.junit.Test;

import static org.junit.Assert.*;

public class EchoPipeTest extends PipeTestBase<EchoPipe> {

    private IPipeLineSession session = new PipeLineSessionBase();

    @Override
    public EchoPipe createPipe() {
        return new EchoPipe();
    }

    @Test
    public void testDoPipe() {
        Object dummyInput = "dummyInput";
        pipe.doPipe(dummyInput, session);

        assertEquals(pipe.doPipe(dummyInput, session).getResult().toString(), dummyInput);
    }


}