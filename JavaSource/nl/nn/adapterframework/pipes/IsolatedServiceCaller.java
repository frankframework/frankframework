/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.pipes;

import java.util.HashMap;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.RequestReplyExecutor;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Guard;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.springframework.core.task.TaskExecutor;

/**
 * Helper class for IbisLocalSender that wraps around {@link ServiceDispatcher} to make calls to a local Ibis adapter in a separate thread.
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version $Id$
 */
public class IsolatedServiceCaller {
	public static final String version="$RCSfile: IsolatedServiceCaller.java,v $ $Revision: 1.15 $ $Date: 2011-11-30 13:51:50 $";
	protected Logger log = LogUtil.getLogger(this);
	
	/**
	 * The thread-pool for spawning threads, injected by Spring
	 */
	private TaskExecutor taskExecutor;

	public void setTaskExecutor(TaskExecutor executor) {
		taskExecutor = executor;
	}

	public TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	public void callServiceAsynchronous(String serviceName, String correlationID, String message, HashMap context, boolean targetIsJavaListener) throws ListenerException {
		IsolatedServiceExecutor ise=new IsolatedServiceExecutor(serviceName, correlationID, message, context, targetIsJavaListener, null);
		getTaskExecutor().execute(ise);
	}
	
	public String callServiceIsolated(String serviceName, String correlationID, String message, HashMap context, boolean targetIsJavaListener) throws ListenerException {
		Guard guard= new Guard();
		guard.addResource();
		IsolatedServiceExecutor ise=new IsolatedServiceExecutor(serviceName, correlationID, message, context, targetIsJavaListener, guard);
		getTaskExecutor().execute(ise);
		try {
			guard.waitForAllResources();
		} catch (InterruptedException e) {
			throw new ListenerException(ClassUtils.nameOf(this)+" was interupted",e);
		}
		if (ise.getThrowable()!=null) {
			if (ise.getThrowable() instanceof ListenerException) {
				throw (ListenerException)ise.getThrowable();
			} else {
				throw new ListenerException(ise.getThrowable());
			}
		} else {
			return (String)ise.getReply();
		}
	}

	public class IsolatedServiceExecutor extends RequestReplyExecutor {
		String serviceName; 
		HashMap context;
		boolean targetIsJavaListener;
		Guard guard;
		
		public IsolatedServiceExecutor(String serviceName, String correlationID, String message, HashMap context, boolean targetIsJavaListener, Guard guard) {
			super();
			this.serviceName=serviceName;
			this.correlationID=correlationID;
			request=message;
			this.context=context;
			this.targetIsJavaListener=targetIsJavaListener;
			this.guard=guard;
		}

		public void run() {
			try {
				if (targetIsJavaListener) {
					reply = JavaListener.getListener(serviceName).processRequest(correlationID, request, context);
				} else {
					reply = ServiceDispatcher.getInstance().dispatchRequest(serviceName, correlationID, request, context);
				}
			} catch (Throwable t) {
				log.warn("IsolatedServiceCaller caught exception",t);
				throwable=t;
			} finally {
				if (guard != null) {
					guard.releaseResource();
				}
			}
		}

	}

}
