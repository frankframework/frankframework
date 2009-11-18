/*
 * $Log: EchoSender.java,v $
 * Revision 1.3  2009-11-18 17:28:03  m00f069
 * Added senders to IbisDebugger
 *
 * Revision 1.2  2008/07/17 16:17:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made synchronous configurable
 *
 * Revision 1.1  2008/05/15 15:08:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * created senders package
 * moved some sender to senders package
 * created special senders
 *
 * Revision 1.1  2007/07/19 15:12:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.debug.IbisDebugger;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

/**
 * Echos input to output. 
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSynchronous(boolean) synchronous}</td><td>hack to allow to introduce a correlationID</td><td>true</td></tr>
 * </table>
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class EchoSender extends SenderWithParametersBase {
	private IbisDebugger ibisDebugger;
	
	private boolean synchronous=true;

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		if (log.isDebugEnabled() && ibisDebugger!=null) {
			message = ibisDebugger.senderInput(this, correlationID, message);
			message = ibisDebugger.senderOutput(this, correlationID, message);
		}
		return message;
	}

	public void setSynchronous(boolean b) {
		synchronous = b;
	}
	public boolean isSynchronous() {
		return synchronous;
	}
	
	public void setIbisDebugger(IbisDebugger ibisDebugger) {
		this.ibisDebugger = ibisDebugger;
	}

}
