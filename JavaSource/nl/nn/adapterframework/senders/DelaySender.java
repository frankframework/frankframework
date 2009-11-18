/*
 * $Log: DelaySender.java,v $
 * Revision 1.2  2009-11-18 17:28:04  m00f069
 * Added senders to IbisDebugger
 *
 * Revision 1.1  2008/05/15 15:08:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * created senders package
 * moved some sender to senders package
 * created special senders
 *
 */
package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.debug.IbisDebugger;

/**
 * Sender that sleeps for a specified time, which defaults to 5000 msecs.
 * Useful for testing purposes.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDelayTime(long) delayTime}</td><td>the time the thread will be put to sleep</td><td>5000 [ms]</td></tr>
 * </table>
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class DelaySender extends SenderBase {
	private IbisDebugger ibisDebugger;

	private long delayTime=5000;


	public String sendMessage(String correlationID, String message) throws SenderException {
		if (log.isDebugEnabled() && ibisDebugger!=null) {
			message = ibisDebugger.senderInput(this, correlationID, message);
		}
		try {
			log.info(getLogPrefix()+"starts waiting for " + getDelayTime() + " ms.");
			Thread.sleep(getDelayTime());
		} catch (InterruptedException e) {
			throw new SenderException(getLogPrefix()+"delay interrupted", e);
		}
		log.info(getLogPrefix()+"ends waiting for " + getDelayTime() + " ms.");
		if (log.isDebugEnabled() && ibisDebugger!=null) {
			message = ibisDebugger.senderOutput(this, correlationID, message);
		}
		return message;
	}

	/**
	 * the time the thread will be put to sleep.
	 */
	public void setDelayTime(long l) {
		delayTime = l;
	}
	public long getDelayTime() {
		return delayTime;
	}
	
	public void setIbisDebugger(IbisDebugger ibisDebugger) {
		this.ibisDebugger = ibisDebugger;
	}

}
