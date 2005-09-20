/*
 * $Log: IsolatedServiceCaller.java,v $
 * Revision 1.2  2005-09-20 13:27:57  europe\L190409
 * added asynchronous call-facility
 *
 * Revision 1.1  2005/09/07 15:35:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of IsolatedServiceCaller
 *
 */
package nl.nn.adapterframework.pipes;

import java.util.HashMap;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.receivers.ServiceDispatcher;

/**
 * Helper class for IbisLocalSender that wraps around {@link ServiceDispatcher} to make calls to a local Ibis adapter in a separate thread.
 * 
 * @author  Gerrit van Brakel
 * @since   4.3
 * @version Id
 */
public class IsolatedServiceCaller extends Thread {
	public static final String version="$RCSfile: IsolatedServiceCaller.java,v $ $Revision: 1.2 $ $Date: 2005-09-20 13:27:57 $";
	protected Logger log = Logger.getLogger(this.getClass());

	String serviceName;
	String correlationID;
	String message;
	HashMap context;
	String result = null;
	Throwable t = null;
	
	
	private IsolatedServiceCaller(String serviceName, String correlationID, String message, HashMap context) {
		super();
		this.serviceName= serviceName;
		this.correlationID=correlationID;
		this.message=message;
		this.context=context;
	}

	public static void callServiceAsynchronous(String serviceName, String correlationID, String message, HashMap context) throws ListenerException {
		IsolatedServiceCaller call = new IsolatedServiceCaller(serviceName, correlationID,  message,  context);
		call.start();
	}
	
	public static String callServiceIsolated(String serviceName, String correlationID, String message, HashMap context) throws ListenerException {
		IsolatedServiceCaller call = new IsolatedServiceCaller(serviceName, correlationID,  message,  context);
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
			result = ServiceDispatcher.getInstance().dispatchRequestWithExceptions(serviceName, correlationID, message, context);
		} catch (Throwable t) {
			log.warn("IsolatedServiceCaller caught exception",t);
			this.t=t;
		}
	}

}
