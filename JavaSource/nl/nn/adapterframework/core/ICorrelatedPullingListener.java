package nl.nn.adapterframework.core;

import java.util.HashMap;
/**
 *
 * Additional behaviour for pulling listeners that are able to listen to a specific
 * message, specified by a correlation ID.
 * 
 * <p>$Id: ICorrelatedPullingListener.java,v 1.2 2004-02-04 10:02:00 a1909356#db2admin Exp $</p>
 *
 * @author Gerrit van Brakel
 * @since 4.0
 */
public interface ICorrelatedPullingListener extends IPullingListener{
		public static final String version="$Id: ICorrelatedPullingListener.java,v 1.2 2004-02-04 10:02:00 a1909356#db2admin Exp $";

/**
 * Retrieves messages from queue or other channel,  but retrieves only
 * messages with the specified correlationId.
 */
Object getRawMessage(String correlationId, HashMap threadContext) throws ListenerException, TimeOutException;
}
