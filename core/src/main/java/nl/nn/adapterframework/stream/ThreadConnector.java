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

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.logging.IbisMaskingLayout;
import nl.nn.adapterframework.util.LogUtil;

public class ThreadConnector<T> implements AutoCloseable {
	protected Logger log = LogUtil.getLogger(this);

	private ThreadLifeCycleEventListener<T> threadLifeCycleEventListener;
	private Thread parentThread;
	private T threadInfo;
	private Set<String> hideRegex;
	
	private enum ThreadState {
		ANNOUNCED,
		CREATED,
		FINISHED;
	};
	
	private ThreadState threadState=ThreadState.ANNOUNCED;
	
	public ThreadConnector(Object owner, ThreadLifeCycleEventListener<T> threadLifeCycleEventListener, String correlationId) {
		super();
		this.threadLifeCycleEventListener=threadLifeCycleEventListener;
		threadInfo=threadLifeCycleEventListener!=null?threadLifeCycleEventListener.announceChildThread(owner, correlationId):null;
		parentThread=Thread.currentThread();
		hideRegex= IbisMaskingLayout.getThreadLocalReplace();
	}
	public ThreadConnector(Object owner, ThreadLifeCycleEventListener<T> threadLifeCycleEventListener, PipeLineSession session) {
		this(owner, threadLifeCycleEventListener, session==null?null:session.getMessageId());
		if (session!=null) {
			session.scheduleCloseOnSessionExit(this);
		} 
	}
	
	public Object startThread(Object input) {
		Thread currentThread = Thread.currentThread();
		if (currentThread!=parentThread) {
			currentThread.setName(parentThread.getName()+"/"+currentThread.getName());
			IbisMaskingLayout.addToThreadLocalReplace(hideRegex);
			// Commented out code below. Do not set contextClassLoader, contextClassLoader is not reliable outside configure().
			// if (currentThread.getContextClassLoader()!=parentThread.getContextClassLoader()) {
			//	currentThread.setContextClassLoader(parentThread.getContextClassLoader());
			// }
			if (threadLifeCycleEventListener!=null) {
				threadState = ThreadState.CREATED;
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

	public Object endThread(Object response) {
		try {
			if (threadLifeCycleEventListener!=null) {
				threadState = ThreadState.FINISHED;
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
				threadState = ThreadState.FINISHED;
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
	@Override
	public void close() throws Exception {
		if (threadLifeCycleEventListener!=null) {
			switch (threadState) {
			case ANNOUNCED:
				threadLifeCycleEventListener.cancelChildThread(threadInfo);
				break;
			case CREATED:
				log.warn("thread was not properly closed");
				threadLifeCycleEventListener.threadEnded(threadInfo, null);
				break;
			case FINISHED:
				break;
			default: 
				throw new IllegalStateException("Unknown ThreadState ["+threadState+"]");
			}
		}
	}
	
}
