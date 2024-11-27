package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;

public class BytesOutputPipeTest extends PipeTestBase<BytesOutputPipe> {

	@Override
	public BytesOutputPipe createPipe() {
		return new BytesOutputPipe();
	}

	@Test
	public void emptyInput() {
		assertThrows(PipeRunException.class, () -> doPipe(pipe, "", session));
	}

	@Test
	public void nonEmptyInput() throws PipeRunException, IOException {
		final String input = " <fields> <field type=\"PackedDecimal\" value=\"+12345\" size=\"16\"/> </fields>";

		PipeRunResult result = doPipe(pipe, input, session);
		assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 18, 52, 92}, result.getResult().asByteArray());
	}

}
