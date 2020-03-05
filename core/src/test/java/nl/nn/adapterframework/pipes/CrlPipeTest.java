package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import org.mockito.Mock;

public class CrlPipeTest extends PipeTestBase<CrlPipe> {

    @Mock
    private IPipeLineSession session = new PipeLineSessionBase();

    @Override
    public CrlPipe createPipe() {
        return new CrlPipe();
    }



}
