/*
 * $Log: ResponseMatcher.java,v $
 * Revision 1.1  2004-11-08 08:31:44  L190409
 * first version
 *
 */
package nl.nn.adapterframework.extensions.ifsa;

import java.util.HashMap;
import java.util.Iterator;

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.TimeOutException;

/**
/**
 * Placeholder for response to an semi-asynchronous message. Allows a thread to wait
 * for a response that will be provided by another thread.
 * 
 * @see ResponseMatcher, ResponseMatcherSender, ResponseMatcherListener
 * @author Gerrit van Brakel
 * @since  4.2d
 *
 */
public class ResponseMatcher implements INamedObject {
	
	private String name;
	
	private static HashMap matchers; 
	private HashMap matches; 
	
	protected ResponseMatcher(String name) {
		super();
		this.name=name;
	}
	
	public static synchronized ResponseMatcher getMatcher(String name) {
		ResponseMatcher result = (ResponseMatcher)matchers.get(name);
		if (result==null) {
			result = new ResponseMatcher(name);
			matchers.put(name, result);
		}
		return result;
	}

	public synchronized ResponseMatch getMatch(String correlationId) {
		ResponseMatch result = (ResponseMatch)matches.get(correlationId);
		if (result==null) {
			result = new ResponseMatch(this, correlationId);
			matches.put(name, result);
		}
		return result;
	}
	
	public synchronized void removeMatch(ResponseMatch match) {
		synchronized (match) {
			matches.remove(match.getCorrelationId()); 
		}
	}

	public void deliver(String correlationId, String message, long timeout)  throws TimeOutException, InterruptedException { 
		getMatch(correlationId).deliver(message,timeout);
	}
	
	public String receive(String correlationId, long timeout) throws TimeOutException, InterruptedException {
		return getMatch(correlationId).receive(timeout);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public synchronized void clear() {
		for (Iterator i=matches.keySet().iterator(); i.hasNext();) {
			String correlationId = (String) i.next();
			removeMatch(getMatch(correlationId));
		}
	}
}
