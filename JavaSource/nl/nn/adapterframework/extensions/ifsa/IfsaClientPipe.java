package nl.nn.adapterframework.extensions.ifsa;

import nl.nn.adapterframework.pipes.MessageSendingPipe;

/**
 * Perform a call to an IFSA Service. 
 * With fire & forget messages, the input is returned as output.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.extension.ifsa.IfsaClient#setClientName(String) sender.clientName}</td><td>name of the client application, on which behalf the service is called</td><td></td></tr>
 * <tr><td>{@link nl.nn.adapterframework.extension.ifsa.IfsaClient#setJndiPath(String) sender.JndiPath}</td><td></td><td></td></tr>
 * <tr><td>{@link nl.nn.adapterframework.extension.ifsa.IfsaClient#setServiceName(String) sender.serviceName}</td><td>name of the service to be called</td><td></td></tr>
 * <tr><td>{@link nl.nn.adapterframework.extension.ifsa.IfsaClient#setMessageProtocol(String) sender.messageProtocol}</td><td>protocol of IFSA-Service to be called. Possible values 
 * <ul>
 *   <li>"FF": Fire & Forget protocol</li>
 *   <li>"RR": Request-Reply protocol</li>
 * </ul></td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>the message was sent (FF) and a good message was retrieved (RR) and no <i>forwardName</i>-attribute was specified</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>item, if a <i>forwardName</i>-attribute was specified</td></tr>
 * <tr><td>"timeout"</td><td>no data was received (timeout on listening), while a request/reply was specified</td></tr>
 * </table>
 * </p>
 * @author Johan Verrips IOS
 * @version Id
 */
 
public class IfsaClientPipe extends MessageSendingPipe {
	public static final String version="$Id: IfsaClientPipe.java,v 1.3 2004-03-26 09:50:51 NNVZNL01#L180564 Exp $";
public IfsaClientPipe() {
	super();
	setSender(new IfsaClient());
}
}
