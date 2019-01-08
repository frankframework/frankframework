package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import org.junit.Test;
import org.mockito.Mock;

public class BytesOutputPipeTest extends PipeTestBase<BytesOutputPipe> {


    @Mock
    private IPipeLineSession session;

    @Override
    public BytesOutputPipe createPipe() {
        return new BytesOutputPipe();
    }

    @Test(expected = PipeRunException.class)
    public void emptyInput() throws PipeRunException {
        pipe.doPipe("", session);
    }

}
