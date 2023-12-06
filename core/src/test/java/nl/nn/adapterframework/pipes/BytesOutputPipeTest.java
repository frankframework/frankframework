package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeRunException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class BytesOutputPipeTest extends PipeTestBase<BytesOutputPipe> {

	@Override
	public BytesOutputPipe createPipe() {
		return new BytesOutputPipe();
	}

	@Test
	public void emptyInput() {
		assertThrows(PipeRunException.class, () -> {
			doPipe(pipe, "", session);
		});
	}
}
