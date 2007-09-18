/*
 * $Log: ICorrelatedPullingListener.java,v $
 * Revision 1.4.6.1  2007-09-18 11:20:37  europe\M00035F
 * * Update a number of method-signatures to take a java.util.Map instead of HashMap
 * * Rewrite JmsListener to be instance of IPushingListener; use Spring JMS Container
 *
 * Revision 1.4  2004/03/30 07:29:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.3  2004/03/26 10:42:50  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.core;

import java.util.Map;
/**
 *
 * Additional behaviour for pulling listeners that are able to listen to a specific
 * message, specified by a correlation ID.
 * 
 * @version Id
 * @author Gerrit van Brakel
 * @since 4.0
 */
public interface ICorrelatedPullingListener extends IPullingListener{
		public static final String version="$Id: ICorrelatedPullingListener.java,v 1.4.6.1 2007-09-18 11:20:37 europe\M00035F Exp $";

/**
 * Retrieves messages from queue or other channel,  but retrieves only
 * messages with the specified correlationId.
 */
Object getRawMessage(String correlationId, Map threadContext) throws ListenerException, TimeOutException;
}
