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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.logging.IbisMaskingLayout;
import nl.nn.adapterframework.util.LogUtil;

public class ThreadConnector<T> {
	protected Logger log = LogUtil.getLogger(this);

	private ThreadLifeCycleEventListener<T> threadLifeCycleEventListener;
	private Thread parentThread;
	private T threadInfo;
	private Set<String> hideRegex;

	private Map<Object, Object> resources;
	private List<TransactionSynchronization> synchronizations;
	private String currentTransactionName;
	private Boolean currentTransactionReadOnly;
	private Integer currentTransactionIsolationLevel;
	private Boolean actualTransactionActive;


	public ThreadConnector(Object owner, ThreadLifeCycleEventListener<T> threadLifeCycleEventListener, String correlationId) {
		super();
		this.threadLifeCycleEventListener=threadLifeCycleEventListener;
		threadInfo=threadLifeCycleEventListener!=null?threadLifeCycleEventListener.announceChildThread(owner, correlationId):null;
		parentThread=Thread.currentThread();
		hideRegex= IbisMaskingLayout.getThreadLocalReplace();
		storeTransactionInfo();
	}
	public ThreadConnector(Object owner, ThreadLifeCycleEventListener<T> threadLifeCycleEventListener, PipeLineSession session) {
		this(owner, threadLifeCycleEventListener, session==null?null:session.getMessageId());
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
			if (threadLifeCycleEventListener!=null) {
				return threadLifeCycleEventListener.threadEnded(threadInfo, response);
			}
			return response;
		} finally {
			IbisMaskingLayout.removeThreadLocalReplace();
		}
	}

	public Throwable abortThread(Throwable t) {
		try {
			if (threadLifeCycleEventListener!=null) {
				Throwable t2 = threadLifeCycleEventListener.threadAborted(threadInfo, t);
				if (t2==null) {
					log.warn("Exception ignored by threadLifeCycleEventListener ("+t.getClass().getName()+"): "+t.getMessage());
				} else {
					return t2;
				}
			}
			return t;
		} finally {
			IbisMaskingLayout.removeThreadLocalReplace();
		}
	}

	public void storeTransactionInfo() {
		resources = TransactionSynchronizationManager.getResourceMap();
//		if (TransactionSynchronizationManager.isSynchronizationActive()) {
//			synchronizations = TransactionSynchronizationManager.getSynchronizations();
//		}
		currentTransactionName = TransactionSynchronizationManager.getCurrentTransactionName();
		currentTransactionReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
		currentTransactionIsolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
		actualTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
	}
	
	public void applyTransactionInfo() {
		if (resources!=null) {
			resources.forEach((k,v) ->TransactionSynchronizationManager.bindResource(k, v));
		}
		if (synchronizations!=null) {
			synchronizations.forEach( v ->TransactionSynchronizationManager.registerSynchronization(v));
		}
		if (currentTransactionName!=null) {
			TransactionSynchronizationManager.setCurrentTransactionName(currentTransactionName);
		}
		if (currentTransactionReadOnly!=null) {
			TransactionSynchronizationManager.setCurrentTransactionReadOnly(currentTransactionReadOnly);
		}
		if (currentTransactionIsolationLevel!=null) {
			TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(currentTransactionIsolationLevel);
		}
		if (actualTransactionActive!=null) {
			TransactionSynchronizationManager.setActualTransactionActive(actualTransactionActive);
		}
	}
}
