package nl.nn.adapterframework.pipes;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.core.PipeRunException;

public class BytesOutputPipeTest extends PipeTestBase<BytesOutputPipe> {

	@Override
	public BytesOutputPipe createPipe() {
		return new BytesOutputPipe();
	}

	@Test
	public void emptyInput() {
		assertThrows(PipeRunException.class, () -> doPipe(pipe, "", session));
	}
}
