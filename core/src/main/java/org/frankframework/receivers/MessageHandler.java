/*
   Copyright 2026 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.receivers;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.ThreadContext;
import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.Lifecycle;
import org.springframework.transaction.PlatformTransactionManager;

import org.frankframework.core.Adapter;
import org.frankframework.core.IListener;
import org.frankframework.core.IMessageHandler;
import org.frankframework.core.IPushingListener;
import org.frankframework.core.IbisTransaction;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.TransactionAttribute;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.RunState;

public class MessageHandler<M> implements IMessageHandler<M>, ApplicationContextAware {
	private IPushingListener<M> listener;
	private Receiver<M> receiver;

	private final PlatformTransactionManager txManager;
	private Adapter adapter;

	public MessageHandler(PlatformTransactionManager txManager) {
		this.txManager = txManager;
	}

	@Override
	public void setApplicationContext(@NonNull ApplicationContext context) {
		if (!(context instanceof Adapter adapterContext)) {
			throw new IllegalArgumentException("ApplicationContext must always be of type Adapter");
		}

		this.adapter = adapterContext;
	}

	public void setReceiver(@NonNull Receiver<M> receiver) {
		this.receiver = receiver;
		this.listener = (IPushingListener<M>) receiver.getListener();
	}

	/**
	 * This method processes the raw message from the listener.
	 * <br/>
	 * The method assumes that a transaction has been started where necessary.
	 * <p>If the {@code rawMessage} parameter is an instance of {@link org.frankframework.receivers.MessageWrapper} then this method
	 * will not call {@link IListener#extractMessage(RawMessageWrapper, Map)} but directly access the enclosed {@link org.frankframework.stream.Message} and the
	 * {@link RawMessageWrapper#getContext()}</p>.
	 */
	@Override
	public void processRawMessage(@NonNull IPushingListener<M> origin, @NonNull RawMessageWrapper<M> message, @NonNull PipeLineSession session, boolean duplicatesAlreadyChecked) throws ListenerException {
		shouldProcess(origin);

		try {
			doProcess(message, session, duplicatesAlreadyChecked);
		} finally {
			// Ensure we always clear the MDC.
			ThreadContext.clearAll();
		}
	}

	/**
	 * Process the received message with {@link IMessageHandler#processRequest(IPushingListener, MessageWrapper, PipeLineSession)}.
	 * <br/>
	 * A messageId is generated that is unique and consists of the name of this listener and a GUID.
	 * <p>
	 *     If the receiver is transactional, then this method will enforce the transactional requirements specified
	 *     by {@link Receiver#setTransactionAttribute(TransactionAttribute)}.
	 * </p>
	 * <p>
	 * N.B. callers of this method should clear the remaining ThreadContext if it's not to be returned to their callers.
	 * </p>
	 */
	@Override
	public @NonNull Message processRequest(@NonNull IPushingListener<M> origin, @NonNull MessageWrapper<M> messageWrapper, @NonNull PipeLineSession session) throws ListenerException {
		shouldProcess(origin);

		boolean manualRetry = session.get(PipeLineSession.MANUAL_RETRY_KEY, false);
		IbisTransaction itx = new IbisTransaction(txManager, receiver.getTxDef(), "Receiver ProcessRequest");

		try {
			return doProcess(messageWrapper, session, manualRetry); // If manual retry, history is checked by original caller
		} catch (ListenerException e) {
			receiver.exceptionThrown("exception processing message", e);
			throw e;
		} finally {
			itx.complete();
		}
	}

	private @NonNull Message doProcess(@NonNull RawMessageWrapper<M> message, @NonNull PipeLineSession session, boolean duplicatesAlreadyChecked) throws ListenerException {
		Objects.requireNonNull(session, "Session can not be null");

		Instant tsReceived = PipeLineSession.getTsReceived(session);
		Instant tsSent = PipeLineSession.getTsSent(session);
		PipeLineSession.updateListenerParameters(session, null, null, tsReceived, tsSent);

		try (final CloseableThreadContext.Instance ignored = getLoggingContext(session)) {
			return receiver.processRawMessage(message, session, duplicatesAlreadyChecked);
		}
	}

	private void shouldProcess(IPushingListener<M> origin) throws ListenerException {
		if (origin != listener) {
			throw new ListenerException("Listener requested ["+origin.getName()+"] is not my Listener");
		}
		if (receiver.getRunState() != RunState.STARTED) {
			throw new ListenerException("Receiver is not started");
		}
		if (origin instanceof Lifecycle lifecycle && !lifecycle.isRunning()) {
			throw new ListenerException("Listener is not started");
		}
	}

	private CloseableThreadContext.Instance getLoggingContext(@NonNull PipeLineSession session) {
		CloseableThreadContext.Instance result = LogUtil.getThreadContext(adapter, session.getMessageId(), session);
		result.put(LogUtil.MDC_LISTENER_KEY, listener.getName());
		result.put(Receiver.THREAD_CONTEXT_KEY_TYPE, ClassUtils.classNameOf(listener));
		return result;
	}
}
