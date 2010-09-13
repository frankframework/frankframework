/*
 * $Log: CoreSenderWrapperProcessor.java,v $
 * Revision 1.1  2010-09-13 14:02:11  L190409
 * split SenderWrapper-processing in chain of processors
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.senders.SenderWrapperBase;

/**
 * @author  Gerrit van Brakel
 * @since   4.11
 * @version Id
 */
public class CoreSenderWrapperProcessor implements SenderWrapperProcessor {
	
	public String sendMessage(SenderWrapperBase senderWrapperBase, String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		return senderWrapperBase.doSendMessage(correlationID, message, prc);
	}

}
