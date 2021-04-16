/*
   Copyright 2019-2021 WeAreFrank!

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
package nl.nn.adapterframework.stream;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.logging.IbisMaskingLayout;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.SpringTxManagerProxy;

public class ThreadConnector<T> {
	protected Logger log = LogUtil.getLogger(this);

	private ThreadLifeCycleEventListener<T> threadLifeCycleEventListener;
	private Thread parentThread;
	private T threadInfo;
	private Set<String> hideRegex;

	private SpringTxManagerProxy txManager;
	private Object parentThreadTransaction;
	
	private Map<Object, Object> resources;
	private List<TransactionSynchronization> synchronizations;
	private String currentTransactionName;
	private Boolean currentTransactionReadOnly;
	private Integer currentTransactionIsolationLevel;
	private Boolean actualTransactionActive;

	private TransactionStatus txStatus;

	public ThreadConnector(Object owner, ThreadLifeCycleEventListener<T> threadLifeCycleEventListener, PlatformTransactionManager txManager, String correlationId) {
		super();
		this.threadLifeCycleEventListener=threadLifeCycleEventListener;
		threadInfo=threadLifeCycleEventListener!=null?threadLifeCycleEventListener.announceChildThread(owner, correlationId):null;
		parentThread=Thread.currentThread();
		hideRegex= IbisMaskingLayout.getThreadLocalReplace();
		this.txManager = (SpringTxManagerProxy)txManager;
		storeTransactionInfo();
	}
	public ThreadConnector(Object owner, ThreadLifeCycleEventListener<T> threadLifeCycleEventListener, PlatformTransactionManager txManager, PipeLineSession session) {
		this(owner, threadLifeCycleEventListener, txManager, session==null?null:session.getMessageId());
	}
	
	public <M> M startThread(M input) {
		Thread currentThread = Thread.currentThread();
		if (currentThread!=parentThread) {
			currentThread.setName(parentThread.getName()+"/"+currentThread.getName());
			IbisMaskingLayout.addToThreadLocalReplace(hideRegex);
			applyTransactionInfo();
			if (threadLifeCycleEventListener!=null) {
				return threadLifeCycleEventListener.threadCreated(threadInfo, input);
			}
		} else {
			if (threadLifeCycleEventListener!=null) {
				threadLifeCycleEventListener.cancelChildThread(threadInfo);
				threadLifeCycleEventListener=null;
			}
		}
		return input;
	}

	public <M> M endThread(M response) {
		try {
			try {
				if (txManager!=null) {
					txManager.commit(txStatus);
				}
			} finally {
				if (threadLifeCycleEventListener!=null) {
					return threadLifeCycleEventListener.threadEnded(threadInfo, response);
				}
				return response;
			}
		} finally {
			IbisMaskingLayout.removeThreadLocalReplace();
		}
	}

	public Throwable abortThread(Throwable t) {
		try {
			try {
				if (txManager!=null) {
					txManager.commit(txStatus);
				}
			} finally {
				if (threadLifeCycleEventListener!=null) {
					Throwable t2 = threadLifeCycleEventListener.threadAborted(threadInfo, t);
					if (t2==null) {
						log.warn("Exception ignored by threadLifeCycleEventListener ("+t.getClass().getName()+"): "+t.getMessage());
					} else {
						return t2;
					}
				}
				return t;
			}
		} finally {
			IbisMaskingLayout.removeThreadLocalReplace();
		}
	}

	public void storeTransactionInfo() {
		if (txManager!=null) {
			parentThreadTransaction = txManager.getCurrentTransaction();
		}
//		resources = TransactionSynchronizationManager.getResourceMap();
//		if (TransactionSynchronizationManager.isSynchronizationActive()) {
//			synchronizations = TransactionSynchronizationManager.getSynchronizations();
//		}
//		currentTransactionName = TransactionSynchronizationManager.getCurrentTransactionName();
//		currentTransactionReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
//		currentTransactionIsolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
//		actualTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
	}
	
	public void applyTransactionInfo() {
		if (txManager!=null) {
			txManager.joinParentThreadsTransaction(parentThreadTransaction);
			TransactionDefinition txDef = txManager.getTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS, 0);
			txStatus = txManager.getTransaction(txDef);
		}
//		if (resources!=null) {
//			resources.forEach((k,v) ->TransactionSynchronizationManager.bindResource(k, v));
//		}
//		if (synchronizations!=null && TransactionSynchronizationManager.isSynchronizationActive()) {
//			synchronizations.forEach( v ->TransactionSynchronizationManager.registerSynchronization(v));
//		}
//		if (currentTransactionName!=null) {
//			TransactionSynchronizationManager.setCurrentTransactionName(currentTransactionName);
//		}
//		if (currentTransactionReadOnly!=null) {
//			TransactionSynchronizationManager.setCurrentTransactionReadOnly(currentTransactionReadOnly);
//		}
//		if (currentTransactionIsolationLevel!=null) {
//			TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(currentTransactionIsolationLevel);
//		}
//		if (actualTransactionActive!=null) {
//			TransactionSynchronizationManager.setActualTransactionActive(actualTransactionActive);
//		}
	}
}
