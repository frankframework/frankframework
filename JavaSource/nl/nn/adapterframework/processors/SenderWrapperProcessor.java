/*
 * $Log: SenderWrapperProcessor.java,v $
 * Revision 1.3  2011-11-30 13:51:53  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2010/09/13 14:02:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
