/*
 * $Log: IPushingListener.java,v $
 * Revision 1.1  2004-07-15 07:38:22  L190409
 * introduction of IListener as common root for Pulling and Pushing listeners
 *
 * Revision 1.2  2004/06/30 10:04:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added INamedObject implementation, added setExceptionListener
 *
 * Revision 1.1  2004/06/22 11:52:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.receivers.ServiceClient;

import java.util.HashMap;

/**
 * Defines listening behaviour of message driven receivers.
 * 
 * @version Id
 * @author Gerrit van Brakel
 * @since 4.2
 */
public interface IPushingListener extends IListener {
	public static final String version="$Id: IPushingListener.java,v 1.1 2004-07-15 07:38:22 L190409 Exp $";


/**
 * Set the handler that will do the processing of the message.
 * Each of the received messages must be pushed through handler.processMessage()
 */
void setHandler(ServiceClient handler);

/**
 * Set a (single) listener that will be notified of any exceptions.
 */
void setExceptionListener(IbisExceptionListener listener);

}
