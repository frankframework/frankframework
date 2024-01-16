package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.frankframework.core.PipeRunException;
import org.junit.jupiter.api.Test;

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
