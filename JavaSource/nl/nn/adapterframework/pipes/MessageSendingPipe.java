package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;


import java.util.HashMap;

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
 * <tr><td><code>sender.*</td><td>any attribute of the sender instantiated by descendant classes</td><td>&nbsp;</td></tr>
 * </table>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link ISender sender}</td><td>specification of sender to send messages with</td></tr>
 * <tr><td>{@link ICorrelatedPullingListener listener}</td><td>specification of listener to listen to for replies</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default when a good message was retrieved (synchronous sender), or the message was successfully sent and no listener was specified and the sender was not synchronous</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified, and otherwise under same condition as "success"</td></tr>
 * <tr><td>"timeout"</td><td>no data was received (timeout on listening), if the sender was synchronous or a listener was specified.</td></tr>
 * </table>
 * </p>
 * @version Id</p>
 * @author Gerrit van Brakel
 */

public class MessageSendingPipe extends FixedForwardPipe implements HasSender {
	public static final String version =
		"$Id: MessageSendingPipe.java,v 1.4 2004-03-26 10:42:34 NNVZNL01#L180564 Exp $";

	private ISender sender = null;
	private ICorrelatedPullingListener listener = null;
	private String resultOnTimeOut = "receiver timed out";
	private String linkMethod = "CORRELATIONID";

	/**
	 * For async communication, the server side may either use the messageID or the correlationID
	 * in the correlationID field of the message. Use this property to set the behaviour.
	 * <p>Use <code>MESSAGEID</code> to let the listener wait for a message with the messageID of the
	 * sent message in the correlation ID field</p>
	 * <p>Use <code>CORRELATIONID</code> to let the listener wait for a message with the correlationID of the
	 * sent message in the correlation ID field</p>
	 * When you use the method CORRELATIONID you have the advantage that you can trace your request
	 * as the messageID as it is known in the Adapter is used as the correlationID. In the logging you should be able
	 * to follow the message more clearly. When you use the method MESSAGEID, the messageID (unique for every
	 * message) will be expected in the correlationID field of the returned message.
	 * 
	 * @param method either MESSAGEID or CORRELATIONID
	 */
	public void setLinkMethod(String method) {
		linkMethod = method;
	}
	public String getLinkMethod() {
		return linkMethod;
	}
	public MessageSendingPipe() {
		super();
	}
	/**
	 * Checks whether a sender is defined for this pipe.
	 */
	public void configure() throws ConfigurationException {
		super.configure();
		if (getSender() == null) {
			throw new ConfigurationException(
				getLogPrefix(null) + "no sender defined ");
		}
		String senderName = getSender().getName();

		if (senderName == null || senderName.equals("")) {
			getSender().setName(getName() + "-sender");
		}

		getSender().configure();
		if (getListener() != null) {
			if (getSender().isSynchronous()) {
				throw new ConfigurationException(
					getLogPrefix(null)
						+ "cannot have listener with synchronous sender");
			}
			getListener().configure();
		}
		if (!(getLinkMethod().equalsIgnoreCase("MESSAGEID"))
			&& (!(getLinkMethod().equalsIgnoreCase("CORRELATIONID"))))
			throw new ConfigurationException(
				"Invalid argument for property LinkMethod ["
					+ getLinkMethod()
					+ "]. it should be either MESSAGEID or CORRELATIONID");

	}

	public PipeRunResult doPipe(Object input, PipeLineSession session)
		throws PipeRunException {
		if (!(input instanceof String)) {
			throw new PipeRunException(
				this,
				"String expected, got a [" + input.getClass().getName() + "]");
		}
		String result = null;
		try {
			String correlationID = session.getMessageId();

			String messageID = null;
			// sendResult has a messageID for async senders, the result for sync senders
			String sendResult =
				getSender().sendMessage(correlationID, (String) input);

			if (getSender().isSynchronous()) {
				if (log.isInfoEnabled())
					log.info(
						getLogPrefix(session)
							+ "sending message to ["
							+ getSender().getName()
							+ "] synchronously");
				result = sendResult;
			} else {
				messageID = sendResult;
				// if linkMethod is MESSAGEID overwrite correlationID with the messageID
				// as this will be used with the listener
				if (getLinkMethod().equalsIgnoreCase("MESSAGEID"))
					correlationID = sendResult;
				if (log.isInfoEnabled())
					log.info(
						getLogPrefix(session)
							+ "sending message to ["
							+ getSender().getName()
							+ "] messageID ["
							+ messageID
							+ "] correlationID ["
							+ correlationID
							+ "] linkMethod ["
							+ getLinkMethod()
							+ "]");
			}

			ICorrelatedPullingListener replyListener = getListener();
			if (replyListener != null) {
				if (log.isDebugEnabled())
					log.debug(
						getLogPrefix(session)
							+ "starts listening for return message with correlationID ["
							+ correlationID
							+ "]");
				HashMap threadContext = replyListener.openThread();
				Object msg =
					replyListener.getRawMessage(correlationID, threadContext);
				result =
					replyListener.getStringFromRawMessage(msg, threadContext);
				replyListener.closeThread(threadContext);
			}
			if (result == null) {
				result = "";
			}
			return new PipeRunResult(getForward(), result);
		} catch (TimeOutException toe) {
			log.warn(getLogPrefix(session) + "timeout occured");
			return new PipeRunResult(
				findForward("timeout"),
				getResultOnTimeOut());

		} catch (Exception e) {
			throw new PipeRunException(
				this,
				getLogPrefix(session) + "caught exception",
				e);
		}
	}
	public ICorrelatedPullingListener getListener() {
		return listener;
	}
	/**
	 * Insert the method's description here.
	 * Creation date: (27-10-2003 17:01:22)
	 * @return java.lang.String
	 */
	public java.lang.String getResultOnTimeOut() {
		return resultOnTimeOut;
	}
	public ISender getSender() {
		return sender;
	}
	/**
	 * Register a {@link ICorrelatedPullingListener} at this Pipe
	 */
	protected void setListener(ICorrelatedPullingListener listener) {
		this.listener = listener;
		log.debug(
			"pipe ["
				+ getName()
				+ " registered listener ["
				+ listener.toString()
				+ "]");
	}
	/**
	 * Insert the method's description here.
	 * Creation date: (27-10-2003 17:01:22)
	 * @param newResultOnTimeOut java.lang.String
	 */
	public void setResultOnTimeOut(java.lang.String newResultOnTimeOut) {
		resultOnTimeOut = newResultOnTimeOut;
	}
	/**
	 * Register a ISender at this Pipe
	 * @see ISender
	 */
	protected void setSender(ISender sender) {
		this.sender = sender;
		log.debug(
			"pipe ["
				+ getName()
				+ " registered sender ["
				+ sender.getName()
				+ "] with properties ["
				+ sender.toString()
				+ "]");
	}
	public void start() throws PipeStartException {
		try {
			getSender().open();
			if (getListener() != null) {
				getListener().open();
			}

		} catch (Exception e) {
			throw new PipeStartException(e);
		}
	}
	public void stop() {
		log.info(getLogPrefix(null) + " is closing");
		try {
			getSender().close();
		} catch (SenderException e) {
			log.warn(getLogPrefix(null) + " exception closing sender", e);
		}
		if (getListener() != null) {
			try {
				log.info(getLogPrefix(null) + " is closing; closing listener");
				getListener().close();
			} catch (ListenerException e) {
				log.warn(getLogPrefix(null) + " Exception closing listener", e);
			}
		}
	}
}
