/*
 * $Log: IsolatedServiceCaller.java,v $
 * Revision 1.11  2010-03-10 14:30:05  m168309
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

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.debug.IbisDebugger;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.log4j.Logger;

/**
 * Helper class for IbisLocalSender that wraps around {@link ServiceDispatcher} to make calls to a local Ibis adapter in a separate thread.
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version Id
 */
public class IsolatedServiceCaller extends Thread {
	public static final String version="$RCSfile: IsolatedServiceCaller.java,v $ $Revision: 1.11 $ $Date: 2010-03-10 14:30:05 $";
	protected Logger log = LogUtil.getLogger(this);
	private IbisDebugger ibisDebugger;

	String serviceName;
	String correlationID;
	String message;
	HashMap context;
	boolean targetIsJavaListener;
	String result = null;
	Throwable t = null;
	String threadID;
	
	
	private IsolatedServiceCaller(String serviceName, String correlationID, String message, HashMap context, boolean targetIsJavaListener, IbisDebugger ibisDebugger) {
		super();
		threadID = Misc.createSimpleUUID();
		setName(serviceName + " " + threadID);
		this.serviceName= serviceName;
		this.correlationID=correlationID;
		this.message=message;
		this.context=context;
		this.targetIsJavaListener=targetIsJavaListener;
		this.ibisDebugger = ibisDebugger;
		if (log.isDebugEnabled() && ibisDebugger!=null) {
			ibisDebugger.createThread(threadID, correlationID);
		}
	}

	public static void callServiceAsynchronous(String serviceName, String correlationID, String message, HashMap context, boolean targetIsJavaListener, IbisDebugger ibisDebugger) throws ListenerException {
		IsolatedServiceCaller call = new IsolatedServiceCaller(serviceName, correlationID,  message,  context, targetIsJavaListener, ibisDebugger);
		call.start();
	}
	
	public static String callServiceIsolated(String serviceName, String correlationID, String message, HashMap context, boolean targetIsJavaListener, IbisDebugger ibisDebugger) throws ListenerException {
		IsolatedServiceCaller call = new IsolatedServiceCaller(serviceName, correlationID,  message,  context, targetIsJavaListener, ibisDebugger);
		call.start();
		try {
			call.join();
		} catch (InterruptedException e) {
			throw new ListenerException(e);
		}

		if (call.t!=null) {
			if (call.t instanceof ListenerException) {
				throw (ListenerException)call.t;
			} else {
				throw new ListenerException(call.t);
			}
		} else {
			return call.result; 
		}
	}
	
	public void run() {
		try {
			if (log.isDebugEnabled() && ibisDebugger!=null) {
				ibisDebugger.startThread(threadID, correlationID, message);
			}
			if (targetIsJavaListener) {
				result = JavaListener.getListener(serviceName).processRequest(correlationID, message, context); 
			} else {
				result = ServiceDispatcher.getInstance().dispatchRequestWithExceptions(serviceName, correlationID, message, context); 
			}
			if (log.isDebugEnabled() && ibisDebugger!=null) {
				ibisDebugger.endThread(correlationID, result);
			}
		} catch (Throwable t) {
			log.warn("IsolatedServiceCaller caught exception",t);
			if (log.isDebugEnabled() && ibisDebugger!=null) {
				t = ibisDebugger.abortThread(correlationID, t);
			}
			this.t=t;
		}
	}

	public void setIbisDebugger(IbisDebugger ibisDebugger) {
		this.ibisDebugger = ibisDebugger;
	}

}
