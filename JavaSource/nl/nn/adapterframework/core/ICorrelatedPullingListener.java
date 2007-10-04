/*
 * $Log: ICorrelatedPullingListener.java,v $
 * Revision 1.4.6.3  2007-10-04 13:23:37  europe\L190409
 * synchronize with HEAD (4.7.0)
 *
 * Revision 1.5  2007/10/03 08:09:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map
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
 * Additional behaviour for pulling listeners that are able to listen to a specific
 * message, specified by a correlation ID.
 * 
 * @author  Gerrit van Brakel
 * @since   4.0
 * @version Id
 */
public interface ICorrelatedPullingListener extends IPullingListener{
	public static final String version = "$RCSfile: ICorrelatedPullingListener.java,v $ $Revision: 1.4.6.3 $ $Date: 2007-10-04 13:23:37 $";

	/**
	 * Retrieves messages from queue or other channel,  but retrieves only
	 * messages with the specified correlationId.
	 */
	Object getRawMessage(String correlationId, Map threadContext) throws ListenerException, TimeOutException;
}
