package org.frankframework.pipes;

import java.io.IOException;
import java.util.Map;

import org.frankframework.core.IDataIterator;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.pipes.TestIteratingBasePipe.IteratingTestPipe;
import org.frankframework.stream.Message;
import org.frankframework.util.ReaderLineIterator;

public class TestIteratingBasePipe extends IteratingPipeTestBase<IteratingTestPipe> {

	final class IteratingTestPipe extends IteratingPipe<String> {

		@Override
		protected IDataIterator<String> getIterator(Message input, PipeLineSession session, Map<String, Object> threadContext) throws SenderException {
			try {
				if (input.isEmpty()) {
					return null;
				}
				return new ReaderLineIterator(input.asReader());
			} catch (IOException e) {
				throw new SenderException(e);
			}
		}

	}

	@Override
	public IteratingTestPipe createPipe() {
		return new IteratingTestPipe();
	}
}
