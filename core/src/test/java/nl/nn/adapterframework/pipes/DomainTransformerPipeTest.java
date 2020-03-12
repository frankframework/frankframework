package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import org.mockito.Mock;

public class DomainTransformerPipeTest extends PipeTestBase<DomainTransformerPipe>{

    @Mock
    private IPipeLineSession session = new PipeLineSessionBase();

    @Override
    public DomainTransformerPipe createPipe() {
        return new DomainTransformerPipe();
    }


}
