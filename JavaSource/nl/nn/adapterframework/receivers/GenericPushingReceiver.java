/*
 * $Log: GenericPushingReceiver.java,v $
 * Revision 1.4  2004-08-23 13:10:48  L190409
 * updated JavaDoc
 *
 * Revision 1.3  2004/08/09 13:47:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
 * Plain extension to {@link ReceiverBase} that can be used directly in configurations.
 * Only extension is that the setter for its worker-object is public, and can therefore
 * be set from the configuration file.
 * For configuration options, see {@link ReceiverBase}.
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since 4.1
 * @deprecated please use {@link nl.nn.adapterframework.receivers.GenericReceiver} instead of this class.
 */
public class GenericPushingReceiver extends ReceiverBase {
	public static final String version="$Id: GenericPushingReceiver.java,v 1.4 2004-08-23 13:10:48 L190409 Exp $";

	public void setListener(IPushingListener listener) {
		super.setListener(listener);
	}
}
