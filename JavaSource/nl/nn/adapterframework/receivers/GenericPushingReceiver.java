/*
 * $Log: GenericPushingReceiver.java,v $
 * Revision 1.1  2004-06-22 12:12:52  L190409
 * introduction of MessagePushers and PushingReceivers
 *
 */
package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.core.IMessagePusher;

/**
 * Plain extension to {@link PushingReceiverBase} that can be used directly in configurations.
 * Only extension is that the setter for its worker-object is public, and can therefore
 * be set from the configuration file.
 * For configuration options, see {@link PushingReceiverBase}.
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since 4.1
 */
public class GenericPushingReceiver extends PushingReceiverBase {
	public static final String version="$Id: GenericPushingReceiver.java,v 1.1 2004-06-22 12:12:52 L190409 Exp $";

	public void setListener(IMessagePusher listener) {
		super.setListener(listener);
	}
}
