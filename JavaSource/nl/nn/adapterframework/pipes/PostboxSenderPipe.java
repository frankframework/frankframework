/*
 * $Log: PostboxSenderPipe.java,v $
 * Revision 1.3  2004-08-23 13:10:09  L190409
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


import java.util.ArrayList;
import java.util.HashMap;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.IPostboxSender;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.core.ParameterValueResolver;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;

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
 */
public class PostboxSenderPipe  extends FixedForwardPipe implements HasSender  {
	public static final String version="$Id: PostboxSenderPipe.java,v 1.3 2004-08-23 13:10:09 L190409 Exp $";
	private IPostboxSender sender = null;
		
	public void configure() throws ConfigurationException {
		super.configure();

		if (getSender() == null) {
				throw new ConfigurationException(getLogPrefix(null) + "no sender defined ");
		}
		String senderName = getSender().getName();
		if (senderName == null || senderName.equals("")) {
			getSender().setName(getName() + "-sender");
		}
		getSender().configure();
	}

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

	public ISender getSender() {
		return sender;
	}

	public void setSender(IPostboxSender sender) {
		this.sender = sender;
	}

	public void start() throws PipeStartException {
		try {
			getSender().open();
		} 
		catch (Exception e) {
			throw new PipeStartException(e);
		}
	}

	public void stop() {
		try {
			getSender().close();
		} 
		catch (Exception e) {
			log.warn(getLogPrefix(null) + " exception closing sender", e);
		}
	}

	public PipeRunResult doPipe(Object input, PipeLineSession session) throws PipeRunException {
		if (! (input instanceof String)) {
			throw new PipeRunException(this, "String expected, got a [" + input.getClass().getName() + "]");
		}

		try {
			HashMap threadContext=new HashMap();
			String correlationID = session.getMessageId();

			// sendResult has a messageID for async senders, the result for sync senders
			String result = sendMessage(input, session, correlationID, getSender(), threadContext);

			if (log.isInfoEnabled()) {
					log.info(getLogPrefix(session) + "sending message to [" + getSender().getName());
			} 
			
			return new PipeRunResult(getForward(), result);
		} 
		catch (Exception e) {
			throw new PipeRunException( this, getLogPrefix(session) + "caught exception", e);
		} 
	}

}
