/*
 * $Log: PostboxSenderPipe.java,v $
 * Revision 1.1  2004-05-21 07:59:30  a1909356#db2admin
 * Add (modifications) due to the postbox sender implementation
 *
 */
package nl.nn.adapterframework.pipes;


import java.util.ArrayList;
import java.util.HashMap;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPostboxSender;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.core.ParameterValueResolver;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;

/**
 * Sends a message using a {@link ISender} and optionally receives a reply from the same sender, or from a {@link ICorrelatedPullingListener listener}.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setResultOnTimeOut(String) resultOnTimeOut}</td><td>result returned when no return-message was received within the timeout limit</td><td>"receiver timed out"</td></tr>
 * <tr><td>{@link #setLinkMethod(String) linkMethod}</td><td>Indicates wether the server uses the correlationID or the messageID in the correlationID field of the reply,</td><td>CORRELATIONID</td></tr>
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
 */
public class PostboxSenderPipe extends MessageSendingPipe {
	public static final String version="$Id: PostboxSenderPipe.java,v 1.1 2004-05-21 07:59:30 a1909356#db2admin Exp $";
	
	/**
	 * @see MessageSendingPipe#setSender(ISender)
	 */
	public void setSender(ISender sender) {
		super.setSender(sender);
	}
	
	/** 
	 * @see nl.nn.adapterframework.core.IPipe#configure()
	 */
	public void configure() throws ConfigurationException {
		super.configure();
		
		if (! (getSender() instanceof IPostboxSender))
			throw new ConfigurationException(getLogPrefix(null) + "sender must be PostboxSender");
		
		if (getListener() != null)
			throw new ConfigurationException(getLogPrefix(null) + "postbox can have a listener");
	}

	/** 
	 * @see nl.nn.adapterframework.pipes.MessageSendingPipe#sendMessage(java.lang.Object, nl.nn.adapterframework.core.PipeLineSession, java.lang.String, nl.nn.adapterframework.core.ISender, java.util.HashMap)
	 */
	protected String sendMessage(Object input, PipeLineSession session, String correlationID, ISender sender, HashMap threadContext)
		throws SenderException {
		
		try {
			ParameterValueResolver resolver = new ParameterValueResolver(input, session);
			ArrayList paramValues = resolver.getValues(getParameterList());
			return ((IPostboxSender)getSender()).sendMessage(correlationID, (String)input, paramValues);
		}
		catch(SenderException e) {
			throw e;
		}
		catch(IbisException e) {
			throw new SenderException("Error while sending message in pipe " + getName(), e);			
		}
		 
	}
}
