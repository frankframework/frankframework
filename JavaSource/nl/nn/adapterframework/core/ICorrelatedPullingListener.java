/**
 * $Log: ICorrelatedPullingListener.java,v $
 * Revision 1.3  2004-03-26 10:42:50  NNVZNL01#L180564
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.core;

import java.util.HashMap;
/**
 *
 * Additional behaviour for pulling listeners that are able to listen to a specific
 * message, specified by a correlation ID.
 * 
 * @version Id
 *
 * @author Gerrit van Brakel
 * @since 4.0
 */
public interface ICorrelatedPullingListener extends IPullingListener{
		public static final String version="$Id: ICorrelatedPullingListener.java,v 1.3 2004-03-26 10:42:50 NNVZNL01#L180564 Exp $";

/**
 * Retrieves messages from queue or other channel,  but retrieves only
 * messages with the specified correlationId.
 */
Object getRawMessage(String correlationId, HashMap threadContext) throws ListenerException, TimeOutException;
}
