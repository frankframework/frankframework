package nl.nn.adapterframework.pipes;

import org.junit.Test;
import org.mockito.Mock;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;

public class BytesOutputPipeTest extends PipeTestBase<BytesOutputPipe> {

	@Mock
	private IPipeLineSession session;

	@Override
	public BytesOutputPipe createPipe() {
		return new BytesOutputPipe();
	}

	@Test(expected = PipeRunException.class)
	public void emptyInput() throws Exception {
		doPipe(pipe, "", session);
	}

}
