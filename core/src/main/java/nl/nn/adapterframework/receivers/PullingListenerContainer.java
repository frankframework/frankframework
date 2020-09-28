/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.SchedulingAwareRunnable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import nl.nn.adapterframework.core.IPeekableListener;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.core.IUsesInProcessState;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.util.Counter;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.Semaphore;


/**
 * Container that provides threads to exectue pulling listeners.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 */
public class PullingListenerContainer<M> implements IThreadCountControllable {
	protected Logger log = LogUtil.getLogger(this);

	private TransactionDefinition txNew = null;

	private ReceiverBase<M> receiver;
	private PlatformTransactionManager txManager;
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
	private TaskExecutor taskExecutor;

	private PullingListenerContainer() {
		super();
	}

	public void configure() {
		if (receiver.getNumThreadsPolling() > 0 && receiver.getNumThreadsPolling() < receiver.getNumThreads()) {
			pollToken = new Semaphore(receiver.getNumThreadsPolling());
		}
		processToken = new Semaphore(receiver.getNumThreads());
		maxThreadCount = receiver.getNumThreads();
		if (receiver.isTransacted()) {
			DefaultTransactionDefinition txDef = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
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
		return (int)threadsRunning.getValue();
	}

	@Override
	public int getMaxThreadCount() {
		return maxThreadCount;
	}

	@Override
	public void increaseThreadCount() {
		maxThreadCount++;
		processToken.release();
	}

	@Override
	public void decreaseThreadCount() {
		if (maxThreadCount>1) {
			maxThreadCount--;
			processToken.tighten();
		}
	}

	private class ControllerTask implements SchedulingAwareRunnable {

		public boolean isLongLived() {
			return true;
		}

		public void run() {
			log.debug(receiver.getLogPrefix()+" taskExecutor ["+ToStringBuilder.reflectionToString(taskExecutor)+"]");
			receiver.setRunState(RunStateEnum.STARTED);
			log.debug(receiver.getLogPrefix()+"started ControllerTask");
			try {
				while (receiver.isInRunState(RunStateEnum.STARTED) && !Thread.currentThread().isInterrupted()) {
					processToken.acquire();
					if (pollToken != null) {
						pollToken.acquire();
					}
					if (isIdle() && receiver.getPollInterval()>0) {
						if (log.isDebugEnabled() && receiver.getPollInterval()>600)log.debug(receiver.getLogPrefix()+"is idle, sleeping for ["+receiver.getPollInterval()+"] seconds");
						for (int i=0; i<receiver.getPollInterval() && receiver.isInRunState(RunStateEnum.STARTED); i++) {
							Thread.sleep(1000);
						}
					}
					taskExecutor.execute(new ListenTask());
				}
			} catch (InterruptedException e) {
				log.warn("polling interrupted", e);
			} finally {
				log.debug(receiver.getLogPrefix()+"closing down ControllerTask");
				if(!receiver.getRunState().equals(RunStateEnum.STOPPING) && !receiver.getRunState().equals(RunStateEnum.STOPPED)) {
					receiver.stopRunning();
				}
				receiver.closeAllResources();
				ThreadContext.removeStack();
			}
		}
	}

	private class ListenTask implements SchedulingAwareRunnable {

		private boolean useInProcessStatus=false;

		@Override
		public boolean isLongLived() {
			return false;
		}

		@Override
		public void run() {
			IPullingListener<M> listener = null;
			Map<String,Object> threadContext = null;
			boolean pollTokenReleased=false;
			try {
				threadsRunning.increase();
				if (receiver.isInRunState(RunStateEnum.STARTED)) {
					listener = (IPullingListener<M>) receiver.getListener();
					threadContext = listener.openThread();
					if (threadContext == null) {
						threadContext = new HashMap<>();
					}
					M rawMessage = null;
					TransactionStatus txStatus = null;
					try {
						try {
							boolean messageAvailable = true;
							if (isIdle() && listener instanceof IPeekableListener) {
								IPeekableListener peekableListener = (IPeekableListener) listener;
								if (peekableListener.isPeekUntransacted()) {
									messageAvailable = peekableListener.hasRawMessageAvailable();
								}
							}
							if (messageAvailable) {
								if (receiver.isTransacted()) {
									txStatus = txManager.getTransaction(txNew);
								}
								rawMessage = listener.getRawMessage(threadContext);
							}
							resetRetryInterval();
							setIdle(rawMessage==null);
						} catch (Exception e) {
							if (txStatus!=null) {
								txManager.rollback(txStatus);
							}
							if (receiver.isOnErrorContinue()) {
								increaseRetryIntervalAndWait(e);
							} else {
								receiver.error("stopping receiver after exception in retrieving message", e);
								receiver.stopRunning();
								return;
							}
						} finally {
							pollTokenReleased=true;
							if (pollToken != null) {
								pollToken.release();
							}
						}
						if (rawMessage != null) {
							tasksStarted.increase(); 
							log.debug(receiver.getLogPrefix()+"started ListenTask ["+tasksStarted.getValue()+"]");
							Thread.currentThread().setName(receiver.getName()+"-listener["+tasksStarted.getValue()+"]");
							// found a message, process it
							
							// first check if it needs to be set to 'inProcess'
							if (listener instanceof IUsesInProcessState &&
									(useInProcessStatus=((IUsesInProcessState<M>)listener).setMessageStateToInProcess(rawMessage, threadContext)) && txStatus!=null) {
								txManager.commit(txStatus);
								txStatus = txManager.getTransaction(txNew);
							}
							try {
								receiver.processRawMessage(listener, rawMessage, threadContext);
								if (txStatus != null) {
									if (txStatus.isRollbackOnly()) {
										receiver.warn("pipeline processing ended with status RollbackOnly, so rolling back transaction");
										rollBack(txStatus, listener, rawMessage, threadContext);
									} else {
										txManager.commit(txStatus);
									}
								}
							} catch (Exception e) {
								try {
									if (txStatus != null && !txStatus.isCompleted()) {
										rollBack(txStatus, listener, rawMessage, threadContext);
									}
								} catch (Exception e2) {
									receiver.error("caught Exception rolling back transaction after catching Exception", e2);
								} finally {
									if (receiver.isOnErrorContinue()) {
										receiver.error("caught Exception processing message, will continue processing next message", e);
									} else {
										receiver.error("stopping receiver after Exception in processing message", e);
										receiver.stopRunning();
									}
								}
							}
						}
					} finally {
						if (txStatus != null && !txStatus.isCompleted()) {
							rollBack(txStatus, listener, rawMessage, threadContext);
						}
					}
				}
			} catch (Throwable e) {
				receiver.error("error occured", e);
			} finally {
				processToken.release();
				if (!pollTokenReleased && pollToken != null) {
					pollToken.release();
				}
				threadsRunning.decrease();
				if (listener != null) {
					try {
						listener.closeThread(threadContext);
					} catch (ListenerException e) {
						receiver.error("Exception closing listener", e);
					}
				}
				ThreadContext.removeStack();
			}
		}
		
		private void rollBack(TransactionStatus txStatus, IPullingListener<M> listener, M rawMessage, Map<String,Object> threadContext) throws ListenerException {
			try {
				txManager.rollback(txStatus);
			} finally {
				if (useInProcessStatus) {
					txStatus = txManager.getTransaction(txNew);
					((IUsesInProcessState<M>)listener).revertInProcessStatusToAvailable(rawMessage, threadContext);
					txManager.commit(txStatus);
				}
			}
		}
	}


	private void resetRetryInterval() {
		synchronized (receiver) {
			if (retryInterval > ReceiverBase.RCV_SUSPENSION_MESSAGE_THRESHOLD) {
				receiver.throwEvent(ReceiverBase.RCV_SUSPENDED_MONITOR_EVENT);
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
		if (currentInterval*2 > ReceiverBase.RCV_SUSPENSION_MESSAGE_THRESHOLD) {
			receiver.throwEvent(ReceiverBase.RCV_SUSPENDED_MONITOR_EVENT);
		}
		while (receiver.isInRunState(RunStateEnum.STARTED) && currentInterval-- > 0) {
			try {
				Thread.sleep(1000);
			} catch (Exception e2) {
				receiver.error("sleep interupted", e2);
				receiver.stopRunning();
			}
		}
	}
	
	
	public void setReceiver(ReceiverBase<M> receiver) {
		this.receiver = receiver;
	}
	public ReceiverBase<M> getReceiver() {
		return receiver;
	}

	public void setTxManager(PlatformTransactionManager manager) {
		txManager = manager;
	}
	public PlatformTransactionManager getTxManager() {
		return txManager;
	}

	public void setTaskExecutor(TaskExecutor executor) {
		taskExecutor = executor;
	}
	public TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	public synchronized void setIdle(boolean b) {
		idle = b;
	}
	public synchronized boolean isIdle() {
		return idle;
	}

}
