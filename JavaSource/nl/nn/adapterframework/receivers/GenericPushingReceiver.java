/*
 * $Log: GenericPushingReceiver.java,v $
 * Revision 1.3  2004-08-09 13:47:53  L190409
 * replaced PushingReceiverBase with ReceiverBase
 *
 * Revision 1.2  2004/07/15 07:48:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed IMessagePusher to IPushingListener
 *
 * Revision 1.1  2004/06/22 12:12:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of MessagePushers and PushingReceivers
 *
 */
package nl.nn.adapterframework.receivers;

import nl.nn.adapterframework.core.IPushingListener;

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
public class GenericPushingReceiver extends ReceiverBase {
	public static final String version="$Id: GenericPushingReceiver.java,v 1.3 2004-08-09 13:47:53 L190409 Exp $";

	public void setListener(IPushingListener listener) {
		super.setListener(listener);
	}
}
