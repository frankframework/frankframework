/*
 * $Log: DelaySender.java,v $
 * Revision 1.6  2010-09-07 15:55:13  m00f069
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 * Revision 1.5  2010/03/10 14:30:04  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * rolled back testtool adjustments (IbisDebuggerDummy)
 *
 * Revision 1.3  2009/12/04 18:23:34  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added ibisDebugger.senderAbort and ibisDebugger.pipeRollback
 *
 * Revision 1.2  2009/11/18 17:28:04  Jaco de Groot <jaco.de.groot@ibissource.org>
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

	private long delayTime=5000;


	public String sendMessage(String correlationID, String message) throws SenderException {
		try {
			log.info(getLogPrefix()+"starts waiting for " + getDelayTime() + " ms.");
			Thread.sleep(getDelayTime());
		} catch (InterruptedException e) {
			throw new SenderException(getLogPrefix()+"delay interrupted", e);
		}
		log.info(getLogPrefix()+"ends waiting for " + getDelayTime() + " ms.");
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

}
