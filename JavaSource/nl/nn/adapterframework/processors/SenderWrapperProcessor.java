/*
 * $Log: SenderWrapperProcessor.java,v $
 * Revision 1.1  2010-09-13 14:02:12  L190409
 * split SenderWrapper-processing in chain of processors
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.senders.SenderWrapperBase;

/**
 * Interface for handlers in SenderWrapper processor chain.
 * 
 * @author  Gerrit van Brakel
 * @since   4.11
 * @version Id
 */
public interface SenderWrapperProcessor {

	public String sendMessage(SenderWrapperBase senderWrapperBase, String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException;

}
