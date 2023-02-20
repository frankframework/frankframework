/*
   Copyright 2013 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
package nl.nn.adapterframework.receivers;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.IHasProcessState;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPeekableListener;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ProcessState;
import nl.nn.adapterframework.core.TransactionAttribute;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Counter;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.RunState;
import nl.nn.adapterframework.util.Semaphore;


/**
 * Container that provides threads to execute pulling listeners.
 *
 * @author  Tim van der Leeuw
 * @since   4.8
 */
public class PullingListenerContainer<M> implements IThreadCountControllable {
	protected Logger log = LogUtil.getLogger(this);

	private TransactionDefinition txNew = null;

	private @Getter @Setter Receiver<M> receiver;
	private @Getter @Setter PlatformTransactionManager txManager;
	private Counter threadsRunning = new Counter(0);
	private Counter tasksStarted = new Counter(0);
	private Semaphore processToken = null; // guard against to many messages being processed at the same time
	private Semaphore pollToken = null; // guard against to many threads polling at the same time
	private boolean idle = false; // true if the last messages received was null, will cause wait loop
	private int retryInterval = 1;
	private int maxThreadCount = 1;

	/**
	 * The thread-pool for spawning threads, injected by Spring
	 */
	private @Getter @Setter TaskExecutor taskExecutor;

	private PullingListenerContainer() {
		super();
	}

	public void configure() {
		if (receiver.getNumThreadsPolling() > 0 && receiver.getNumThreadsPolling() < receiver.getNumThreads()) {
			pollToken = new Semaphore(receiver.getNumThreadsPolling());
		}

		processToken = new Semaphore(receiver.getNumThreads());
		maxThreadCount = receiver.getNumThreads();
		if (receiver.getTransactionAttribute() != TransactionAttribute.NOTSUPPORTED) {
			DefaultTransactionDefinition txDef = new DefaultTransactionDefinition(TransactionAttribute.REQUIRESNEW.getTransactionAttributeNum());
			if (receiver.getTransactionTimeout() > 0) {
				txDef.setTimeout(receiver.getTransactionTimeout());
			}
			txNew = txDef;
		}
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
		log.trace("Get current thread-count, synchronized (lock) on threadsRunning[{}]", threadsRunning);
		try {
			return (int)threadsRunning.getValue();
		} finally {
			log.trace("Get current thread-count, lock on threadsRunning[{}] released", threadsRunning);
		}
	}

	@Override
	public int getMaxThreadCount() {
		return maxThreadCount;
	}

	@Override
	public void increaseThreadCount() {
		maxThreadCount++;
		log.trace("PullingListenerContainer - increaseThreadCount - release processToken - synchronize (lock) on processToken[{}]", pollToken);
		processToken.release();
		log.trace("PullingListenerContainer - increaseThreadCount - released processToken - lock on processToken[{}] released", pollToken);
	}

	@Override
	public void decreaseThreadCount() {
		if (maxThreadCount>1) {
			maxThreadCount--;
			log.trace("PullingListenerContainer - decreaseThreadCount - tighten processToken - synchronize (lock) on processToken[{}]", pollToken);
			processToken.tighten();
			log.trace("PullingListenerContainer - decreaseThreadCount - tightened processToken - lock on processToken[{}] released", pollToken);
		}
	}

	private class ControllerTask implements SchedulingAwareRunnable, INamedObject {

		private @Getter @Setter String name;

		@Override
		public boolean isLongLived() {
			return true;
		}

		public ControllerTask() {
			setName(ClassUtils.nameOf(receiver));
		}

		@Override
		public void run() {
			ThreadContext.push(getName());
			log.debug("taskExecutor [{}]", ()->ToStringBuilder.reflectionToString(taskExecutor));
			receiver.setRunState(RunState.STARTED);
			log.debug("started ControllerTask");
			try {
				while (receiver.isInRunState(RunState.STARTED) && !Thread.currentThread().isInterrupted()) {
					log.trace("PullingListenerContainer - ControllerTask - acquire processToken - synchronize (lock) on processToken[{}]", pollToken);
					processToken.acquire();
					log.trace("PullingListenerContainer - ControllerTask - acquired processToken - lock on processToken[{}] released", pollToken);
					if (pollToken != null) {
						log.trace("PullingListenerContainer - ControllerTask - acquire pollToken - synchronize (lock) on pollToken[{}]", pollToken);
						pollToken.acquire();
						log.trace("PullingListenerContainer - ControllerTask - acquired pollToken - lock on pollToken[{}] released", pollToken);
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
					receiver.stopRunning();
				}
				receiver.closeAllResources(); //We have to call closeAllResources as the receiver won't do this for IPullingListeners

				ThreadContext.removeStack(); // potentially redundant, makes sure to remove the NDC/MDC
			}
		}
	}

	private class ListenTask implements SchedulingAwareRunnable, INamedObject {

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
				log.trace("ListenTask-run - increase threadsRunning - synchronize (lock) on threadsRunning[{}]", threadsRunning);
				threadsRunning.increase();
				log.trace("ListenTask-run - increased threadsRunning - lock on threadsRunning[{}] released", threadsRunning);
				if (receiver.isInRunState(RunState.STARTED)) {
					if (listener instanceof IHasProcessState<?> && ((IHasProcessState<?>)listener).knownProcessStates().contains(ProcessState.INPROCESS)) {
						inProcessStateManager = (IHasProcessState<M>)listener;
					}
					threadContext = listener.openThread();
					if (threadContext == null) {
						threadContext = new HashMap<>();
					}
					M rawMessage = null;
					TransactionStatus txStatus = null;
					int deliveryCount=0;
					boolean messageHandled = false;
					String messageId = null;
					try { //  doesn't catch anything, rolls back transaction in finally clause when required
						try {
							try {
								boolean messageAvailable = true;
								if (isIdle() && listener instanceof IPeekableListener) {
									IPeekableListener<?> peekableListener = (IPeekableListener<?>) listener;
									if (peekableListener.isPeekUntransacted()) {
										messageAvailable = peekableListener.hasRawMessageAvailable();
									}
								}
								if (messageAvailable) {
									// Start a transaction if the entire processing is transacted, or
									// messages needs to be moved to inProcess, and transaction control is not inhibited by setting transactionAttribute=NotSupported.
									if (receiver.isTransacted() || inProcessStateManager!=null && receiver.getTransactionAttribute() != TransactionAttribute.NOTSUPPORTED) {
										txStatus = txManager.getTransaction(txNew);
										log.debug("Transaction Started, Get Message from Listener");
									}
									rawMessage = listener.getRawMessage(threadContext);
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
									receiver.exceptionThrown("exception occured while retrieving message", e); //actually use ON_ERROR and don't just stop the receiver
									return;
								}
							}
							if (rawMessage == null) {
								if (txStatus!=null) {
									log.debug("Rollback; raw message == null");
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
								// This is necessary for dbmses like MariaDB, that have no 'SKIP LOCKED' functionality, and for pipelines that do not support roll back
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
							log.trace("{} PullingListenerContainer - ListenTask - run - increase tasksStarted synchronize (lock) on tasksStarted[{}]", receiver::getLogPrefix, tasksStarted::toString);
							synchronized (tasksStarted) {
								tasksStarted.increase();
								log.debug("{} started ListenTask [{}]", receiver::getLogPrefix, tasksStarted::getValue);
								Thread.currentThread().setName(receiver.getName()+"-listener["+tasksStarted.getValue()+"]");
							}
							log.trace("{} PullingListenerContainer - ListenTask - run - increased tasksStarted lock on tasksStarted[{}] released", receiver::getLogPrefix, tasksStarted::toString);
						} finally {
							// release pollToken after message has been moved to inProcess, so it is not seen as 'available' by the next thread
							pollTokenReleased=true;
							if (pollToken != null) {
								log.trace("PullingListenerContainer - ListenTask - release pollToken - synchronize (lock) on pollToken[{}]", pollToken);
								pollToken.release();
								log.trace("PullingListenerContainer - ListenTask - released pollToken - lock on pollToken[{}] released", pollToken);
							}
						}

						try {
							if (receiver.getMaxRetries()>=0) {
								messageId = listener.getIdFromRawMessage(rawMessage, threadContext);
								deliveryCount = receiver.getDeliveryCount(messageId, rawMessage);
							}
							if (receiver.getMaxRetries()<0 || deliveryCount <= receiver.getMaxRetries()+1 || receiver.isSupportProgrammaticRetry()) {
								try (PipeLineSession session = new PipeLineSession()) {
									session.putAll(threadContext);
									receiver.processRawMessage(listener, rawMessage, session, true);
								}
							} else {
								String correlationId = (String) threadContext.get(PipeLineSession.correlationIdKey);
								Date receivedDate = new Date();
								String errorMessage = Misc.concatStrings("too many retries", "; ", receiver.getCachedErrorMessage(messageId));
								final M rawMessageFinal = rawMessage;
								final Map<String,Object> threadContextFinal = threadContext;
								receiver.moveInProcessToError(messageId, correlationId, () -> listener.extractMessage(rawMessageFinal, threadContextFinal), receivedDate, errorMessage, rawMessage, Receiver.TXREQUIRED);
								log.trace("Run PullingListenerContainer - CacheProcessResult - synchronize (lock) on Receiver[{}]", receiver);
								receiver.cacheProcessResult(messageId, errorMessage, receivedDate); // required here to increase delivery count
								log.trace("Run PullingListenerContainer - CacheProcessResult - lock on Receiver[{}] released", receiver);
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
									receiver.exceptionThrown("exception occured while processing message", e); //actually use ON_ERROR and don't just stop the receiver
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
					if (!messageHandled && inProcessStateManager!=null) {
						txStatus = receiver.isTransacted() || receiver.getTransactionAttribute() != TransactionAttribute.NOTSUPPORTED ? txManager.getTransaction(txNew) : null;
						boolean noMoreRetries = receiver.getMaxRetries()>=0 && deliveryCount>receiver.getMaxRetries();
						ProcessState targetState = noMoreRetries ? ProcessState.ERROR : ProcessState.AVAILABLE;
						log.debug("noMoreRetries [{}] deliveryCount [{}] targetState [{}]", noMoreRetries, deliveryCount, targetState);
						String errorMessage = Misc.concatStrings(noMoreRetries? "too many retries":null, "; ", receiver.getCachedErrorMessage(messageId));
						((IHasProcessState<M>)listener).changeProcessState(rawMessage, targetState, errorMessage!=null ? errorMessage : "processing not successful");
						if (txStatus!=null) {
							txManager.commit(txStatus);
							txStatus = null;
						}
					}
				}
			} catch (Exception e) {
				receiver.error("error occurred", e);
			} finally {
				log.trace("PullingListenerContainer - ListenTask - release processToken - synchronize (lock) on processToken[{}]", pollToken);
				processToken.release();
				log.trace("PullingListenerContainer - ListenTask - released processToken - lock on processToken[{}] released", pollToken);
				if (!pollTokenReleased && pollToken != null) {
					log.trace("PullingListenerContainer - ListenTask - release pollToken - synchronize (lock) on pollToken[{}]", pollToken);
					pollToken.release();
					log.trace("PullingListenerContainer - ListenTask - released pollToken - lock on pollToken[{}] released", pollToken);
				}
				log.trace("ListenTask-run - decrease threadsRunning - synchronize (lock) on threadsRunning[{}]", threadsRunning);
				threadsRunning.decrease();
				log.trace("ListenTask-run - decreased threadsRunning - lock on threadsRunning[{}] released", threadsRunning);
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

		private void rollBack(TransactionStatus txStatus, M rawMessage, String reason) throws ListenerException {
			if (log.isDebugEnabled()) {
				String stackTrace = Arrays.stream(Thread.currentThread().getStackTrace())
					.map(StackTraceElement::toString)
					.reduce("\n", (acc, element) -> acc + "    at " + element + "\n");
				log.debug("Rolling back TX, reason: {}, stack:{}", reason, stackTrace);
			}
			try {
				txManager.rollback(txStatus);
			} finally {
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
		log.trace("Reset receiver retry interval - synchronize (lock) on receiver {}", receiver::getLogPrefix);
		try {
			synchronized (receiver) {
				if (retryInterval > Receiver.RCV_SUSPENSION_MESSAGE_THRESHOLD) {
					receiver.throwEvent(Receiver.RCV_SUSPENDED_MONITOR_EVENT);
				}
				retryInterval = 1;
			}
		} finally {
			log.trace("Reset receiver retry interval - lock on receiver {} released", receiver::getLogPrefix);
		}
	}

	private void increaseRetryIntervalAndWait(Throwable t) {
		long currentInterval;
		log.trace("Reset increase retry interval - synchronize (lock) on receiver {}", receiver::getLogPrefix);
		synchronized (receiver) {
			currentInterval = retryInterval;
			retryInterval = retryInterval * 2;
			if (retryInterval > 3600) {
				retryInterval = 3600;
			}
		}
		log.trace("Reset retry interval increased - lock on receiver {} released", receiver::getLogPrefix);
		receiver.error("caught Exception retrieving message, will continue retrieving messages in [" + currentInterval + "] seconds", t);
		if (currentInterval*2 > Receiver.RCV_SUSPENSION_MESSAGE_THRESHOLD) {
			receiver.throwEvent(Receiver.RCV_SUSPENDED_MONITOR_EVENT);
		}
		while (receiver.isInRunState(RunState.STARTED) && currentInterval-- > 0) {
			try {
				Thread.sleep(1000);
			} catch (Exception e2) {
				receiver.error("sleep interupted", e2);
				receiver.stopRunning();
			}
		}
	}



	public void setIdle(boolean b) {
		log.trace("{} Set PullingListenerContainer idle={} - synchronize (lock) on PullingListenerContainer[{}]", receiver::getLogPrefix, ()->b, this::toString);
		try {
			synchronized (this) {
				idle = b;
			}
		} finally {
			log.trace("{} Set PullingListenerContainer idle={} - lock on PullingListenerContainer[{}] released", receiver::getLogPrefix, ()->b, this::toString);
		}
	}
	public boolean isIdle() {
		log.trace("{} Check if PullingListenerContainer is idle - synchronize (lock) on PullingListenerContainer[{}]", receiver::getLogPrefix, this::toString);
		try {
			synchronized (this) {
				return idle;
			}
		} finally {
			log.trace("{} Check if PullingListenerContainer is idle - lock on PullingListenerContainer[{}] released", receiver::getLogPrefix, this::toString);
		}
	}

}
