/*
   Copyright 2013 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import io.micrometer.core.instrument.DistributionSummary;
import lombok.Getter;
import lombok.Setter;

import org.frankframework.core.HasName;
import org.frankframework.core.IHasProcessState;
import org.frankframework.core.IPeekableListener;
import org.frankframework.core.IPullingListener;
import org.frankframework.core.IThreadCountControllable;
import org.frankframework.core.ListenerException;
import org.frankframework.core.NameAware;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.ProcessState;
import org.frankframework.core.TransactionAttribute;
import org.frankframework.statistics.FrankMeterType;
import org.frankframework.statistics.MetricsInitializer;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.RunState;
import org.frankframework.util.StringUtil;
import org.frankframework.util.TimeProvider;


/**
 * Container that provides threads to execute pulling listeners.
 *
 * @author  Tim van der Leeuw
 * @since   4.8
 */
public class PullingListenerContainer<M> implements IThreadCountControllable {
	protected Logger log = LogUtil.getLogger(this);

	private TransactionDefinition txNew = null;

	private @Setter MetricsInitializer metricsInitializer;
	private DistributionSummary messagePeekingStatistics;
	private DistributionSummary messageReceivingStatistics;

	private @Getter @Setter Receiver<M> receiver;
	private @Getter @Setter PlatformTransactionManager txManager;
	private final AtomicInteger threadsRunning = new AtomicInteger();
	private final AtomicInteger tasksStarted = new AtomicInteger();
	private ResourceLimiter processToken = null; // guard against to many messages being processed at the same time
	private ResourceLimiter pollToken = null; // guard against to many threads polling at the same time
	private final AtomicBoolean idle = new AtomicBoolean(false); // true if the last messages received was null, will cause wait loop
	private int retryInterval = 1;

	/**
	 * The thread-pool for spawning threads, injected by Spring
	 */
	private @Getter @Setter TaskExecutor taskExecutor;

	public void configure() {
		if (receiver.getNumThreadsPolling() > 0 && receiver.getNumThreadsPolling() < receiver.getNumThreads()) {
			pollToken = new ResourceLimiter(receiver.getNumThreadsPolling());
		}

		processToken = new ResourceLimiter(receiver.getNumThreads());
		if (receiver.getTransactionAttribute() != TransactionAttribute.NOTSUPPORTED) {
			DefaultTransactionDefinition txDef = new DefaultTransactionDefinition(TransactionAttribute.REQUIRESNEW.getTransactionAttributeNum());
			if (receiver.getTransactionTimeout() > 0) {
				txDef.setTimeout(receiver.getTransactionTimeout());
			}
			txNew = txDef;
		}

		if (receiver.getListener() instanceof IPeekableListener<M>) {
			messagePeekingStatistics = metricsInitializer.createSubDistributionSummary(receiver, receiver.getListener(), FrankMeterType.LISTENER_MESSAGE_PEEKING);
		}

		messageReceivingStatistics = metricsInitializer.createSubDistributionSummary(receiver, receiver.getListener(), FrankMeterType.LISTENER_MESSAGE_RECEIVING);
	}

	public void start() {
		taskExecutor.execute(new ControllerTask());
	}

	public void stop() {
		// nothing special here
	}

	@Override
	public boolean isThreadCountReadable() {
		return true;
	}

	@Override
	public boolean isThreadCountControllable() {
		return true;
	}

	@Override
	public int getCurrentThreadCount() {
		return threadsRunning.get();
	}

	@Override
	public int getMaxThreadCount() {
		return processToken.getMaxResourceLimit();
	}

	@Override
	public void increaseThreadCount() {
		processToken.increaseMaxResourceCount(1);
	}

	@Override
	public void decreaseThreadCount() {
		if (processToken.getMaxResourceLimit() > 1) {
			processToken.reduceMaxResourceCount(1);
		}
	}

	private class ControllerTask implements SchedulingAwareRunnable, HasName {

		private final @Getter String name;

		@Override
		public boolean isLongLived() {
			return true;
		}

		public ControllerTask() {
			name = ClassUtils.nameOf(receiver);
		}

		@Override
		public void run() {
			ThreadContext.push(getName());
			log.debug("taskExecutor [{}]", ()->ToStringBuilder.reflectionToString(taskExecutor));
			receiver.setRunState(RunState.STARTED);
			log.debug("started ControllerTask");
			try {
				while (receiver.isInRunState(RunState.STARTED) && !Thread.currentThread().isInterrupted()) {
					processToken.acquire();
					if (pollToken != null) {
						pollToken.acquire();
					}
					if (isIdle() && receiver.getPollInterval()>0) {
						if (log.isDebugEnabled() && receiver.getPollInterval()>600) log.debug("is idle, sleeping for [{}] seconds", receiver.getPollInterval());
						for (int i=0; i<receiver.getPollInterval() && receiver.isInRunState(RunState.STARTED); i++) {
							Thread.sleep(1000);
						}
					}
					taskExecutor.execute(new ListenTask());
				}
			} catch (InterruptedException e) {
				log.warn("polling interrupted", e);
				Thread.currentThread().interrupt();
			} finally {
				log.debug("closing down ControllerTask");
				if(receiver.getRunState()!=RunState.STOPPING && receiver.getRunState()!=RunState.EXCEPTION_STOPPING && receiver.getRunState()!=RunState.STOPPED) { // Prevent circular reference in Receiver. IPullingListeners stop as their threads finish
					receiver.stop();
				}
				receiver.closeAllResources(); // We have to call closeAllResources as the receiver won't do this for IPullingListeners

				ThreadContext.removeStack(); // potentially redundant, makes sure to remove the NDC/MDC
			}
		}
	}

	private class ListenTask implements SchedulingAwareRunnable, HasName, NameAware {

		private @Getter @Setter String name;
		private IHasProcessState<M> inProcessStateManager=null;

		@Override
		public boolean isLongLived() {
			return false;
		}

		public ListenTask() {
			setName("Receiver ["+receiver.getName()+"]");
		}

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			final IPullingListener<M> listener = (IPullingListener<M>) receiver.getListener();
			Map<String,Object> threadContext = null;
			boolean pollTokenReleased=false;
			try {
				threadsRunning.incrementAndGet();
				log.trace("ListenTask-run - increased threadsRunning - on threadsRunning[{}]", threadsRunning);
				if (receiver.isInRunState(RunState.STARTED)) {
					if (listener instanceof IHasProcessState<?> state && state.knownProcessStates().contains(ProcessState.INPROCESS)) {
						inProcessStateManager = (IHasProcessState<M>)listener;
					}
					threadContext = listener.openThread();
					RawMessageWrapper<M> rawMessage = null;
					TransactionStatus txStatus = null;
					boolean messageHandled = false;
					try { //  doesn't catch anything, rolls back transaction in finally clause when required
						try {
							try {
								boolean messageAvailable = true;
								if (isIdle() && listener instanceof IPeekableListener peekableListener) {
									if (peekableListener.isPeekUntransacted()) {
										long start = System.currentTimeMillis();
										messageAvailable = peekableListener.hasRawMessageAvailable();
										long end = System.currentTimeMillis();

										messagePeekingStatistics.record((double) end - start);
									}
								}
								if (messageAvailable) {
									// Start a transaction if the entire processing is transacted, or
									// messages needs to be moved to inProcess, and transaction control is not inhibited by setting transactionAttribute=NotSupported.
									if (receiver.isTransacted() || (inProcessStateManager != null && receiver.getTransactionAttribute() != TransactionAttribute.NOTSUPPORTED)) {
										txStatus = txManager.getTransaction(txNew);
										log.trace("Transaction Started, Get Message from Listener");
									}

									long start = System.currentTimeMillis();
									rawMessage = listener.getRawMessage(threadContext);
									long end = System.currentTimeMillis();

									messageReceivingStatistics.record((double) end - start);
								}
								resetRetryInterval();
								setIdle(rawMessage==null);
							} catch (Exception e) {
								if (txStatus!=null) {
									log.debug("Rollback; exception", e);
									txManager.rollback(txStatus);
								}
								if (receiver.isOnErrorContinue()) {
									increaseRetryIntervalAndWait(e);
								} else {
									receiver.exceptionThrown("exception occurred while retrieving message", e); //actually use ON_ERROR and don't just stop the receiver
									return;
								}
							}
							if (rawMessage == null) {
								if (txStatus!=null) {
									log.trace("Rollback; raw message == null"); //Why do we do a rollback here? There is no message to process?
									txManager.rollback(txStatus);
								}
								return;
							}

							if (inProcessStateManager!=null) {
								log.debug("Set message-state to IN_PROCESSING");
								if ((rawMessage = inProcessStateManager.changeProcessState(rawMessage, ProcessState.INPROCESS, "start processing"))==null) {
									if (txStatus!=null) {
										log.debug("Rollback; raw message from inProcessStateManager == null");
										txManager.rollback(txStatus);
									}
									return;
								}
								// If inProcess-state is used, we'll commit the transaction that set the message state to the inProcess.
								// This releases the lock on the record being processed.
								// This is necessary for dbmses like older MariaDB, that have no 'SKIP LOCKED' functionality, and for pipelines that do not support roll back.
								// This also is needed for when processing multiple listener threads.
								if (txStatus!=null) {
									txManager.commit(txStatus);
									if (receiver.isTransacted()) {
										txStatus = txManager.getTransaction(txNew);
									} else {
										txStatus = null;
									}
								}
							}

							// found a message, process it
							int tasks = tasksStarted.incrementAndGet();
							log.debug("{} started ListenTask [{}]", receiver::getLogPrefix, () -> tasks);
							Thread.currentThread().setName(receiver.getName() + "-listener[" + tasks + "]"); // Becomes `MyReceiverListener-listener[123]`
						} finally {
							// release pollToken after message has been moved to inProcess, so it is not seen as 'available' by the next thread
							pollTokenReleased=true;
							if (pollToken != null) {
								pollToken.release();
							}
						}

						try {
							try (PipeLineSession session = new PipeLineSession()) {
								session.putAll(threadContext);
								receiver.updateMessageReceiveCount(rawMessage);
								if (receiver.isSupportProgrammaticRetry() || !receiver.isDeliveryRetryLimitExceededBeforeMessageProcessing(rawMessage, session, false)) {
									receiver.processRawMessage(listener, rawMessage, session, true);
								} else {
									Instant receivedDate = TimeProvider.now();
									String errorMessage = StringUtil.concatStrings("too many retries", "; ", receiver.getCachedErrorMessage(rawMessage));
									receiver.moveInProcessToError(rawMessage, session, receivedDate, errorMessage, Receiver.TXREQUIRED);
								}
							}
							messageHandled = true;
							if (txStatus != null) {
								if (txStatus.isRollbackOnly()) {
									messageHandled = false;
									receiver.warn("pipeline processing ended with status RollbackOnly, so rolling back transaction");
									rollBack(txStatus, rawMessage, "Pipeline processing ended with status RollbackOnly");
								} else {
									log.debug("Message processed successfully, committing transaction");
									txManager.commit(txStatus);
								}
								txStatus = null;
							}
						} catch (Exception e) {
							receiver.error("caught Exception processing message", e);
							try {
								if (txStatus != null && !txStatus.isCompleted()) {
									messageHandled = false;
									log.debug("Rollback because exception occurred and message handling transaction was not completed yet.");
									rollBack(txStatus, rawMessage, "Exception caught ("+e.getClass().getTypeName()+"): "+e.getMessage());
									txStatus = null;
								}
							} catch (Exception e2) {
								receiver.error("caught Exception rolling back transaction after catching Exception", e2);
							} finally {
								if (receiver.isOnErrorContinue()) {
									receiver.error("caught Exception processing message, will continue processing next message", e);
								} else {
									receiver.exceptionThrown("exception occurred while processing message", e); //actually use ON_ERROR and don't just stop the receiver
								}
							}
						}
					} finally {
						if (txStatus != null && !txStatus.isCompleted()) {
							log.debug("Rollback because in finally-clause, message handling transaction was not completed.");
							messageHandled = false;
							rollBack(txStatus, rawMessage, "Rollback because transaction has terminated unexpectedly");
							txStatus = null;
						}
					}

					if (!messageHandled && inProcessStateManager != null) {
						txStatus = receiver.isTransacted() || receiver.getTransactionAttribute() != TransactionAttribute.NOTSUPPORTED ? txManager.getTransaction(txNew) : null;
						boolean noMoreRetries = receiver.isDeliveryRetryLimitExceededAfterMessageProcessed(rawMessage);
						ProcessState targetState = noMoreRetries ? ProcessState.ERROR : ProcessState.AVAILABLE;
						String errorMessage = StringUtil.concatStrings(noMoreRetries ? "too many retries" : null, "; ", receiver.getCachedErrorMessage(rawMessage));
						inProcessStateManager.changeProcessState(rawMessage, targetState, errorMessage!=null ? errorMessage : "processing not successful");
						if (txStatus!=null) {
							txManager.commit(txStatus);
							txStatus = null;
						}
					}
				}
			} catch (Exception e) {
				receiver.error("error occurred", e);
			} finally {
				processToken.release();
				if (!pollTokenReleased && pollToken != null) {
					pollToken.release();
				}
				threadsRunning.decrementAndGet();
				if (listener != null) {
					try {
						listener.closeThread(threadContext);
					} catch (ListenerException e) {
						receiver.error("Exception closing listener thread", e);
					}
				}
				ThreadContext.removeStack(); //Cleanup the MDC stack that was created during message processing
			}
		}

		private void rollBack(TransactionStatus txStatus, RawMessageWrapper<M> rawMessage, String reason) {
			if (log.isDebugEnabled()) {
				String stackTrace = Arrays.stream(Thread.currentThread().getStackTrace())
					.map(StackTraceElement::toString)
					.reduce("\n", (acc, element) -> acc + "    at " + element + "\n");
				log.debug("Rolling back TX, reason: {}, stack:{}", reason, stackTrace);
			}
			try {
				txManager.rollback(txStatus);
			} finally {
				// TODO: Check if we do, or do not, need the change in process state below or if it will always be done in other part of the PullingListenerContainer code anyway
				if (inProcessStateManager!=null) {
					TransactionStatus txStatusRevert = txManager.getTransaction(txNew);
					try {
						log.debug("Changing message state back to AVAILABLE in rollback, reason: {}", reason);
						inProcessStateManager.changeProcessState(rawMessage, ProcessState.AVAILABLE, reason);
						txManager.commit(txStatusRevert);
					} catch (Exception e) {
						log.error("Error in post-rollback actions", e);
					}
				}
			}
		}
	}

	private void resetRetryInterval() {
		synchronized (receiver) {
			if (retryInterval > Receiver.RCV_SUSPENSION_MESSAGE_THRESHOLD) {
				receiver.throwEvent(Receiver.RCV_SUSPENDED_MONITOR_EVENT);
			}
			retryInterval = 1;
		}
	}

	private void increaseRetryIntervalAndWait(Throwable t) {
		long currentInterval;
		synchronized (receiver) {
			currentInterval = retryInterval;
			retryInterval = retryInterval * 2;
			if (retryInterval > 3600) {
				retryInterval = 3600;
			}
		}
		receiver.error("caught Exception retrieving message, will continue retrieving messages in [" + currentInterval + "] seconds", t);
		if (currentInterval*2 > Receiver.RCV_SUSPENSION_MESSAGE_THRESHOLD) {
			receiver.throwEvent(Receiver.RCV_SUSPENDED_MONITOR_EVENT);
		}
		while (receiver.isInRunState(RunState.STARTED) && currentInterval-- > 0) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e2) {
				Thread.currentThread().interrupt();
				receiver.error("sleep interrupted", e2);
				receiver.stop();
			}
		}
	}

	public void setIdle(boolean b) {
		idle.set(b);
	}

	public boolean isIdle() {
		return idle.get();
	}
}
