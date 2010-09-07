/*
 * $Log: IsolatedServiceCaller.java,v $
 * Revision 1.12  2010-09-07 15:55:13  m00f069
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 * Revision 1.11  2010/03/10 14:30:05  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * rolled back testtool adjustments (IbisDebuggerDummy)
 *
 * Revision 1.9  2009/11/18 17:28:04  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added senders to IbisDebugger
 *
 * Revision 1.8  2009/09/07 13:28:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused code
 *
 * Revision 1.7  2007/12/10 10:10:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * no transactions in IsolatedServiceCaller
 *
 * Revision 1.6  2007/06/07 15:19:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * set names of isolated threads
 *
 * Revision 1.5  2007/02/12 14:02:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.4  2006/08/22 06:50:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added asynchronous and transactional features
 *
 * Revision 1.3  2005/09/26 11:54:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * enabeld isolated calls from IbisLocalSender to JavaListener as well as to WebServiceListener
 *
 * Revision 1.2  2005/09/20 13:27:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added asynchronous call-facility
 *
 * Revision 1.1  2005/09/07 15:35:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of IsolatedServiceCaller
 *
 */
package nl.nn.adapterframework.pipes;

import java.util.HashMap;

import nl.nn.adapterframework.core.ISender;
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
 * @version Id
 */
public class IsolatedServiceCaller {
	public static final String version="$RCSfile: IsolatedServiceCaller.java,v $ $Revision: 1.12 $ $Date: 2010-09-07 15:55:13 $";
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
					reply = ServiceDispatcher.getInstance().dispatchRequestWithExceptions(serviceName, correlationID, request, context);
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
