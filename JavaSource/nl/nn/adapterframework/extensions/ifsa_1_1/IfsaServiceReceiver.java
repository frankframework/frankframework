package nl.nn.adapterframework.extensions.ifsa_1_1;

import nl.nn.adapterframework.receivers.PullingReceiverBase;

/** 
 * IFSA Server-side Receiver: the easy way to turn your application into an IFSA service.
 * The property <code>listener.messageProtocol</code>
 * should be set to indicate wether it is a Request/Reply or Fire &amp; Forget
 * receiver. When you use this class the property <code>listener.serverName</code> should be
 * set, the property <code>listener.clientName</code> should not be used (left to null).
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.extensions.ifsa.IfsaServiceReceiver</td><td>&nbsp;</td></tr>
 * <tr><td>{@link PullingReceiverBase#setName(String) name}</td>  <td>name of the receiver as known to the adapter</td><td>&nbsp;</td></tr>
 * <tr><td>{@link PullingReceiverBase#setNumThreads(int) numThreads}</td><td>the number of threads listening in parallel for messages</td><td>1</td></tr>
 * <tr><td>{@link PullingReceiverBase#setOnError(String) onError}</td><td>one of 'continue' or 'close'. Controls the behaviour of the receiver when it encounters an error sending a reply</td><td>continue</td></tr>
 * <tr><td>{@link IfsaServiceListener#setMessageProtocol(String) listener.messageProtocol}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link IfsaServiceListener#setServiceName(String) listener.ServiceName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link IfsaServiceListener#setServerName(String) listener.ServerName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link IfsaServiceListener#setCommitOnState(String) listener.CommitOnState}</td><td>For Fire & Forget messages, messages are only committed when the result of 
 * executing the adapter and pipeline equals the value set by this attribute</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * @author Johan Verrips / Gerrit van Brakel
 * @version Id
 */
public class IfsaServiceReceiver extends PullingReceiverBase {
	public static final String version="$Id: IfsaServiceReceiver.java,v 1.1 2004-07-06 07:07:26 L190409 Exp $";
public IfsaServiceReceiver() {
	super();
	setListener(new IfsaServiceListener());
}
}
