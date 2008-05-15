/*
 * $Log: EchoSender.java,v $
 * Revision 1.1  2008-05-15 15:08:27  europe\L190409
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
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

/**
 * Echos input to output. 
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
public class EchoSender extends SenderWithParametersBase {

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		return message;
	}

	public boolean isSynchronous() {
		return true;
	}

}
