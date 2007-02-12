/*
 * $Log: IsolatedServiceCaller.java,v $
 * Revision 1.5  2007-02-12 14:02:19  europe\L190409
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

import javax.transaction.UserTransaction;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.receivers.ServiceDispatcher;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * Helper class for IbisLocalSender that wraps around {@link ServiceDispatcher} to make calls to a local Ibis adapter in a separate thread.
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version Id
 */
public class IsolatedServiceCaller extends Thread {
	public static final String version="$RCSfile: IsolatedServiceCaller.java,v $ $Revision: 1.5 $ $Date: 2007-02-12 14:02:19 $";
	protected Logger log = LogUtil.getLogger(this);

	String serviceName;
	String correlationID;
	String message;
	HashMap context;
	boolean targetIsJavaListener;
	boolean doTransaction;
	String result = null;
	Throwable t = null;
	
	
	private IsolatedServiceCaller(String serviceName, String correlationID, String message, HashMap context, boolean targetIsJavaListener, boolean doTransaction) {
		super();
		this.serviceName= serviceName;
		this.correlationID=correlationID;
		this.message=message;
		this.context=context;
		this.targetIsJavaListener=targetIsJavaListener;
		this.doTransaction=doTransaction;
	}

	public static void callServiceAsynchronous(String serviceName, String correlationID, String message, HashMap context, boolean targetIsJavaListener, boolean doTransaction) throws ListenerException {
		IsolatedServiceCaller call = new IsolatedServiceCaller(serviceName, correlationID,  message,  context, targetIsJavaListener, doTransaction);
		call.start();
	}
	
	public static String callServiceIsolated(String serviceName, String correlationID, String message, HashMap context, boolean targetIsJavaListener, boolean doTransaction) throws ListenerException {
		IsolatedServiceCaller call = new IsolatedServiceCaller(serviceName, correlationID,  message,  context, targetIsJavaListener, doTransaction);
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
		UserTransaction utx=null;
		try {
			if (doTransaction) {
				log.debug("starting new transaction for correlation id ["+correlationID+"]");
				utx=JtaUtil.getUserTransaction();
				utx.begin();
			}
			if (targetIsJavaListener) {
				result = JavaListener.getListener(serviceName).processRequest(correlationID, message, context); 
			} else {
				result = ServiceDispatcher.getInstance().dispatchRequestWithExceptions(serviceName, correlationID, message, context); 
			}
			if (doTransaction) {
				if (JtaUtil.inTransaction(utx)) {
					log.debug("committing transaction for correlation id ["+correlationID+"]");
					utx.commit();
				} else {
					log.debug("rolling back transaction for correlation id ["+correlationID+"]");
					utx.rollback();
				}
			}
		} catch (Throwable t) {
			log.warn("IsolatedServiceCaller caught exception",t);
			this.t=t;
			if (doTransaction && utx!=null) {
				try {
					log.debug("trying to roll back transaction for correlation id ["+correlationID+"] after exception");
					utx.rollback();
				} catch (Throwable t2) {
					log.warn("exception rolling back transaction for correlation id ["+correlationID+"]",t2);
				}
			}
		}
	}

}
