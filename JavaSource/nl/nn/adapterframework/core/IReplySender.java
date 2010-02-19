/*
 * $Log: IReplySender.java,v $
 * Revision 1.1  2010-02-19 13:45:28  m00f069
 * - Added support for (sender) stubbing by debugger
 * - Added reply listener and reply sender to debugger
 * - Use IbisDebuggerDummy by default
 * - Enabling/disabling debugger handled by debugger instead of log level
 * - Renamed messageId to correlationId in debugger interface
 *
 */
package nl.nn.adapterframework.core;

/**
 * A reply sender can be used to send a reply for a request received by a
 * listener.
 * 
 * @author Jaco de Groot
 */
public interface IReplySender extends ISender {
	public static final String version="$Id: IReplySender.java,v 1.1 2010-02-19 13:45:28 m00f069 Exp $";

	/**
	 * When <code>true</code>, this sender is used as a reply sender.
	 */
	public void isReplySender(boolean isReplySender);

	/**
	 * When <code>true</code>, this sender is used as a reply sender.
	 */
	public boolean isReplySender();

}
