/*
   Copyright 2019-2024 WeAreFrank!

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
package org.frankframework.threading;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import org.frankframework.core.PipeLineSession;
import org.frankframework.jta.IThreadConnectableTransactionManager;
import org.frankframework.jta.TransactionConnector;
import org.frankframework.logging.IbisMaskingLayout;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.LogUtil;

/**
 * Connect a parent thread and a child thread to carry over important state from the parent
 * thread to the child thread, such as {@link ThreadContext}, thread-local hide-regexes
 * for masking sensitive information from logs (See {@link IbisMaskingLayout}, and transaction state
 * via a {@link TransactionConnector}.
 *
 * @param <T> Type of the {@code threadInfo} maintained by the {@link ThreadLifeCycleEventListener}.
 */
public class ThreadConnector<T> implements AutoCloseable {
	protected Logger log = LogUtil.getLogger(this);

	private ThreadLifeCycleEventListener<T> threadLifeCycleEventListener;
	private final Thread parentThread;
	private Thread childThread;
	private Map<String,String> savedThreadContext;
	private final T threadInfo;
	private final Collection<Pattern> hideRegex;

	private enum ThreadState {
		ANNOUNCED,
		CREATED,
		FINISHED
	}

	private ThreadState threadState=ThreadState.ANNOUNCED;
	private final TransactionConnector<?,?> transactionConnector;


	public ThreadConnector(Object owner, String description, ThreadLifeCycleEventListener<T> threadLifeCycleEventListener, IThreadConnectableTransactionManager<?,?> txManager, String correlationId) {
		super();
		this.threadLifeCycleEventListener = threadLifeCycleEventListener;
		threadInfo = threadLifeCycleEventListener != null ? threadLifeCycleEventListener.announceChildThread(owner, correlationId) : null;
		log.trace("[{}] announced thread [{}] for owner [{}] correlationId [{}]", this, threadInfo, owner, correlationId);
		parentThread = Thread.currentThread();
		// Get thread-local hide regexes from the parent thread so they will be carried over to the child thread
		hideRegex = IbisMaskingLayout.getThreadLocalReplace();
		transactionConnector = TransactionConnector.getInstance(txManager, owner, description);
		saveThreadContext();
	}

	public ThreadConnector(Object owner, String description, ThreadLifeCycleEventListener<T> threadLifeCycleEventListener, IThreadConnectableTransactionManager<?,?> txManager, PipeLineSession session) {
		this(owner, description, threadLifeCycleEventListener, txManager, session==null?null:session.getCorrelationId());
	}

	protected void saveThreadContext() {
		savedThreadContext = ThreadContext.getContext();
		log.trace("saved ThreadContext [{}]", savedThreadContext);
	}

	protected void restoreThreadContext() {
		if (savedThreadContext != null) {
			log.trace("restoring ThreadContext [{}]", savedThreadContext);
			ThreadContext.putAll(savedThreadContext);
			savedThreadContext = null;
		}
	}

	public <R> R startThread(R input) {
		childThread = Thread.currentThread();
		if (childThread != parentThread) {
			restoreThreadContext();
		}
		if (transactionConnector != null) {
			transactionConnector.beginChildThread();
		}
		if (childThread != parentThread) {
			childThread.setName(parentThread.getName() + "/" + childThread.getName());
			// Carry over hide regexes from the parent thread to the child thread
			IbisMaskingLayout.setThreadLocalReplace(hideRegex);
			if (threadLifeCycleEventListener!=null) {
				threadState = ThreadState.CREATED;
				log.trace("[{}] start thread [{}]", this, threadInfo);
				return threadLifeCycleEventListener.threadCreated(threadInfo, input);
			}
		} else {
			if (threadLifeCycleEventListener != null) {
				log.trace("[{}] cancel thread [{}]", this, threadInfo);
				threadLifeCycleEventListener.cancelChildThread(threadInfo);
				threadLifeCycleEventListener = null;
			}
		}
		return input;
	}

	public <R> R endThread(R response) {
		Thread currentThread = Thread.currentThread();
		if (currentThread != childThread) {
			throw new IllegalStateException("endThread() must be called from childThread");
		}
		R result;
		saveThreadContext();
		try {
			try {
				if (transactionConnector != null) {
					transactionConnector.endChildThread();
				}
			} finally {
				if (threadLifeCycleEventListener != null) {
					threadState = ThreadState.FINISHED;
					log.trace("[{}] end thread [{}]", this, threadInfo);
					result = threadLifeCycleEventListener.threadEnded(threadInfo, response);
				} else {
					result = response;
				}
			}
		} finally {
			IbisMaskingLayout.clearThreadLocalReplace();
		}
		return result;
	}

	public Throwable abortThread(Throwable t) {
		Thread currentThread = Thread.currentThread();
		if (currentThread != childThread) {
			Exception e = new IllegalStateException("abortThread() must be called from childThread");
			e.addSuppressed(t);
			return e;
		}
		Throwable result = t;
		saveThreadContext();
		try {
			try {
				if (transactionConnector != null) {
					transactionConnector.endChildThread();
				}
			} finally {
				if (threadLifeCycleEventListener != null) {
					threadState = ThreadState.FINISHED;
					log.trace("[{}] abort thread [{}]", this, threadInfo);
					result = threadLifeCycleEventListener.threadAborted(threadInfo, t);
					if (result == null) {
						log.warn("Exception ignored by threadLifeCycleEventListener ({}): {}", t.getClass().getName(), t.getMessage());
					}
				}
			}
		} finally {
			IbisMaskingLayout.clearThreadLocalReplace();
		}
		return result;
	}

	@Override
	public void close() {
		restoreThreadContext();
		CloseUtils.closeSilently(transactionConnector);
		if (threadLifeCycleEventListener != null) {
			switch (threadState) {
				case ANNOUNCED -> {
					log.trace("[{}] cancel thread [{}] in close", this, threadInfo);
					threadLifeCycleEventListener.cancelChildThread(threadInfo);
				}
				case CREATED -> {
					log.warn("thread was not properly closed");
					log.trace("[{}] end thread [{}] in close", this, threadInfo);
					threadLifeCycleEventListener.threadEnded(threadInfo, null);
				}
				case FINISHED -> {
					// No-op
				}
				default -> throw new IllegalStateException("Unknown ThreadState [" + threadState + "]");
			}
		}
	}
}
