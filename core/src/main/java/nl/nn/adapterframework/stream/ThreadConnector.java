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

import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.logging.IbisMaskingLayout;
import nl.nn.adapterframework.util.LogUtil;

public class ThreadConnector<T> {
	protected Logger log = LogUtil.getLogger(this);

	private ThreadLifeCycleEventListener<T> threadLifeCycleEventListener;
	private Thread parentThread;
	private T threadInfo;
	private Set<String> hideRegex;

	private TransactionConnector transactionConnector;

	public ThreadConnector(Object owner, ThreadLifeCycleEventListener<T> threadLifeCycleEventListener, PlatformTransactionManager txManager, String correlationId) {
		super();
		this.threadLifeCycleEventListener=threadLifeCycleEventListener;
		threadInfo=threadLifeCycleEventListener!=null?threadLifeCycleEventListener.announceChildThread(owner, correlationId):null;
		parentThread=Thread.currentThread();
		hideRegex= IbisMaskingLayout.getThreadLocalReplace();
		transactionConnector = new TransactionConnector(txManager);
	}
	public ThreadConnector(Object owner, ThreadLifeCycleEventListener<T> threadLifeCycleEventListener, PlatformTransactionManager txManager, PipeLineSession session) {
		this(owner, threadLifeCycleEventListener, txManager, session==null?null:session.getMessageId());
	}
	
	public <M> M startThread(M input) {
		Thread currentThread = Thread.currentThread();
		if (currentThread!=parentThread) {
			currentThread.setName(parentThread.getName()+"/"+currentThread.getName());
			IbisMaskingLayout.addToThreadLocalReplace(hideRegex);
			transactionConnector.applyTransactionInfo();
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
				transactionConnector.commit();
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
				transactionConnector.rollback();
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

	
}
