package nl.nn.adapterframework.pipes;

import java.io.IOException;
import java.util.Map;

import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.pipes.TestIteratingPipe.IteratingTestPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ReaderLineIterator;

public class TestIteratingPipe extends IteratingPipeTestBase<IteratingTestPipe> {

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
