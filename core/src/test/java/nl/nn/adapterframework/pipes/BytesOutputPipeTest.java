package nl.nn.adapterframework.pipes;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeRunException;

public class BytesOutputPipeTest extends PipeTestBase<BytesOutputPipe> {

	@Override
	public BytesOutputPipe createPipe() {
		return new BytesOutputPipe();
	}

	@Test(expected = PipeRunException.class)
	public void emptyInput() throws Exception {
		doPipe(pipe, "", session);
	}

}
