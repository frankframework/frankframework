/*
 * $Log: ResponseMatch.java,v $
 * Revision 1.1  2004-11-08 08:31:44  L190409
 * first version
 *
 */
package nl.nn.adapterframework.extensions.ifsa;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.util.Semaphore;

/**
 * Placeholder for response to an semi-asynchronous message. Allows a thread to wait
 * for a response that will be provided by another thread.
 * 
 * @see ResponseMatcher, ResponseMatcherSender, ResponseMatcherListener
 * @author Gerrit van Brakel
 * @version Id
 * @since  4.2d
 */
public class ResponseMatch {
	protected static Logger log = Logger.getLogger(ResponseMatch.class);
	
	private ResponseMatcher owner;
	private String correlationId;
	private String response;
	
	private Semaphore receiving = new Semaphore();
	private Semaphore delivered = new Semaphore();
	private long timeoutOnDelivery;
	
	public ResponseMatch(ResponseMatcher owner, String correlationId) {
		super();
		this.correlationId = correlationId;
		this.owner = owner;
	}
	
	protected void finish() {
		owner.removeMatch(this);
	}
	
	public String receive(long timeout) throws InterruptedException, TimeOutException {
		receiving.release();
		try {
			delivered.acquire(timeout);
			return response;
		} finally {
			finish();
		}
	}

	public void deliver(String response, long timeout) throws InterruptedException, TimeOutException {
		boolean timeoutOccurred=false;
		this.response = response;
		delivered.release();
		try {
			receiving.acquire(timeoutOnDelivery);
		} catch (TimeOutException e) {
			try {
				delivered.acquire(1);
				finish();
				timeoutOccurred=true;
			} catch (TimeOutException e2) {
				// receiver got message on the valreep, so let go
			}
		}
		if (timeoutOccurred) {
			throw new TimeOutException("ResponseMatch for ["+correlationId+"] response was not collected in due time ["+timeoutOnDelivery+"]");
		}
	}


	public String getCorrelationId() {
		return correlationId;
	}
	public void setCorrelationId(String string) {
		correlationId = string;
	}

}
