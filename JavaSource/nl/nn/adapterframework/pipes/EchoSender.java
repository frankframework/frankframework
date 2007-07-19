/*
 * $Log: EchoSender.java,v $
 * Revision 1.1  2007-07-19 15:12:08  europe\L190409
 * first version
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

/**
 * Echos input to output. 
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
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
