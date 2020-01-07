/*
   Copyright 2019 Integration Partners

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

import org.apache.log4j.Logger;

import nl.nn.adapterframework.util.LogUtil;

public class ThreadConnector {
	protected Logger log = LogUtil.getLogger(this);

	private ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener;
	private Thread parentThread;
	private Object threadInfo;
	private String hideRegex;
	
	public ThreadConnector(Object owner, ThreadLifeCycleEventListener<Object> threadLifeCycleEventListener, String correlationID) {
		super();
		this.threadLifeCycleEventListener=threadLifeCycleEventListener;
		threadInfo=threadLifeCycleEventListener!=null?threadLifeCycleEventListener.announceChildThread(owner, correlationID):null;
		parentThread=Thread.currentThread();
		hideRegex=LogUtil.getThreadHideRegex();
	}

	
	public Object startThread(Object input) {
		Thread currentThread = Thread.currentThread();
		if (currentThread!=parentThread) {
			currentThread.setName(parentThread.getName()+"/"+currentThread.getName());
			LogUtil.setThreadHideRegex(hideRegex);
			// Commented out code below. Do not set contextClassLoader, contextClassLoader is not reliable outside configure().
			// if (currentThread.getContextClassLoader()!=parentThread.getContextClassLoader()) {
			//	currentThread.setContextClassLoader(parentThread.getContextClassLoader());
			// }
		} else {
			threadLifeCycleEventListener=null;
		}
		if (threadLifeCycleEventListener!=null) {
			return threadLifeCycleEventListener.threadCreated(threadInfo, input);
		}
		return input;
	}

	public Object endThread(Object response) {
		try {
			if (threadLifeCycleEventListener!=null) {
				return threadLifeCycleEventListener.threadEnded(threadInfo, response);
			}
			return response;
		} finally {
			LogUtil.removeThreadHideRegex();
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
			LogUtil.removeThreadHideRegex();
		}
	}
	
}
