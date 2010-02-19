/*
 * $Log: SenderBase.java,v $
 * Revision 1.3  2010-02-19 13:45:27  m00f069
 * - Added support for (sender) stubbing by debugger
 * - Added reply listener and reply sender to debugger
 * - Use IbisDebuggerDummy by default
 * - Enabling/disabling debugger handled by debugger instead of log level
 * - Renamed messageId to correlationId in debugger interface
 *
 * Revision 1.2  2009/12/04 18:23:34  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added ibisDebugger.senderAbort and ibisDebugger.pipeRollback
 *
 * Revision 1.1  2008/05/15 15:08:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * created senders package
 * moved some sender to senders package
 * created special senders
 *
 */
package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.debug.IbisDebugger;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * Baseclass for senders.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Sender</td><td>&nbsp;</td></tr>
 * </table>
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public abstract class SenderBase implements ISender {
	protected Logger log = LogUtil.getLogger(this);
	protected IbisDebugger ibisDebugger;

	private String name;

	public void configure() throws ConfigurationException {
	}

	public void open() throws SenderException {
	}

	public void close() throws SenderException {
	}

	public abstract String sendMessage(String correlationID, String message) throws SenderException, TimeOutException;

	public boolean isSynchronous() {
		return true;
	}

	protected String getLogPrefix() {
		return getLogPrefix(this);
	}

	public static String getLogPrefix(INamedObject object) {
		return "["+object.getClass().getName()+"] ["+object.getName()+"] ";
	}

	public void setName(String name) {
		this.name=name;
	}
	public String getName() {
		return name;
	}

	public void throwSenderException(Throwable throwable) throws SenderException {
		throwSenderException(this, throwable);
	}
	
	public static void throwSenderException(ISender sender, Throwable throwable) throws SenderException {
		if (throwable instanceof SenderException) {
			throw (SenderException)throwable;
		} else {
			throw new SenderException(SenderBase.getLogPrefix(sender)+"unexpected throwable",throwable);
		}
	}
	
	public void throwSenderOrTimeOutException(Throwable throwable) throws SenderException, TimeOutException {
		throwSenderOrTimeOutException(this, throwable);
	}
	
	public static void throwSenderOrTimeOutException(ISender sender, Throwable throwable) throws SenderException, TimeOutException {
		if (throwable instanceof TimeOutException) {
			throw (TimeOutException)throwable;
		} else {
			throwSenderException(sender, throwable);
		}
	}
	
	public void setIbisDebugger(IbisDebugger ibisDebugger) {
		this.ibisDebugger = ibisDebugger;
	}

}
