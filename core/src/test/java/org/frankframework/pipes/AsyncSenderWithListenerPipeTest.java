package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ICorrelatedPullingListener;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.TimeoutException;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.senders.EchoSender;
import org.frankframework.stream.Message;

public class AsyncSenderWithListenerPipeTest extends PipeTestBase<AsyncSenderWithListenerPipe<String>> {

	@Override
	public AsyncSenderWithListenerPipe<String> createPipe() throws ConfigurationException {
		return new AsyncSenderWithListenerPipe<String>();
	}

	@Test
	public void setName() throws ConfigurationException {
		pipe.setName("test");
		EchoSender sender = spy(EchoSender.class);
		doReturn(false).when(sender).isSynchronous();
		pipe.setSender(sender);
		pipe.setListener(new CorrelatedListener());

		assertNull(pipe.getListener().getName());
		assertNull(pipe.getSender().getName());

		pipe.configure();

		assertEquals("test-sender", pipe.getSender().getName());
		assertEquals("test-replylistener", pipe.getListener().getName());
	}

	private static class CorrelatedListener implements ICorrelatedPullingListener<String> {

		private @Setter @Getter ApplicationContext applicationContext;
		private @Setter @Getter String name;

		@Override
		public Map<String, Object> openThread() throws ListenerException {
			return new HashMap<String, Object>();
		}

		@Override
		public void closeThread(Map<String, Object> threadContext) throws ListenerException {
			// NO OP
		}

		@Override
		public RawMessageWrapper<String> getRawMessage(Map<String, Object> threadContext) throws ListenerException {
			return null;
		}

		@Override
		public void start() {
			// NO OP
		}

		@Override
		public void stop() {
			// NO OP
		}

		@Override
		public Message extractMessage(RawMessageWrapper<String> rawMessage, Map<String, Object> context) throws ListenerException {
			return null;
		}

		@Override
		public void afterMessageProcessed(PipeLineResult processResult, RawMessageWrapper<String> rawMessage, PipeLineSession pipeLineSession) throws ListenerException {
			// NO OP
		}

		@Override
		public void configure() throws ConfigurationException {
			// NO OP
		}

		@Override
		public RawMessageWrapper<String> getRawMessage(String correlationId, Map<String, Object> threadContext) throws ListenerException, TimeoutException {
			return null;
		}
	}
}
