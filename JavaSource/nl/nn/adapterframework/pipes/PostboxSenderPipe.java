/*
 * $Log: PostboxSenderPipe.java,v $
 * Revision 1.5  2008-12-30 17:01:12  m168309
 * added configuration warnings facility (in Show configurationStatus)
 *
 * Revision 1.4  2004/10/05 11:38:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * deprecated, all functionality is in GenericMessageSendingPipe
 *
 * Revision 1.3  2004/08/23 13:10:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated JavaDoc
 *
 * Revision 1.2  2004/05/21 10:47:30  unknown <unknown@ibissource.org>
 * Add (modifications) due to the postbox retriever implementation
 *
 * Revision 1.1  2004/05/21 07:59:30  unknown <unknown@ibissource.org>
 * Add (modifications) due to the postbox sender implementation
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;

/**
 * Sends a message using a {@link ISender} and optionally receives a reply from the same sender, or from a {@link nl.nn.adapterframework.core.ICorrelatedPullingListener listener}.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #addParameter(Parameter) parameterList}</td><td>Parameters of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td><code>sender.*</td><td>any attribute of the sender instantiated by descendant classes</td><td>&nbsp;</td></tr>
 * </table>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link IPostboxSender sender}</td><td>specification of postbox sender to send messages with</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default when the message was successfully sent</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified, and otherwise under same condition as "success"</td></tr>
 * </table>
 * </p>
  * 
 * @author John Dekker
 * @version Id
 * @deprecated please use plain GenericMessageSendingPipe, that has same functionality (since 4.2d)
 */
public class PostboxSenderPipe extends GenericMessageSendingPipe  {
	public static final String version="$Id: PostboxSenderPipe.java,v 1.5 2008-12-30 17:01:12 m168309 Exp $";

	public void configure() throws ConfigurationException {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = getLogPrefix(null)+"The class ["+getClass().getName()+"] has been deprecated. Please change to ["+getClass().getSuperclass().getName()+"]";
		configWarnings.add(log, msg);
		super.configure();
	}
}
