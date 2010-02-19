/*
 * $Log: IReplyListener.java,v $
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
 * A reply listener can be used to receive a reply for a request send by a
 * sender.
 * 
 * @author Jaco de Groot
 */
public interface IReplyListener extends IListener {
	public static final String version="$Id: IReplyListener.java,v 1.1 2010-02-19 13:45:28 m00f069 Exp $";

	/**
	 * When <code>true</code>, this listener is used as a reply listener.
	 */
	public void isReplyListener(boolean isReplyListener);

	/**
	 * When <code>true</code>, this listener is used as a reply listener.
	 */
	public boolean isReplyListener();

}
