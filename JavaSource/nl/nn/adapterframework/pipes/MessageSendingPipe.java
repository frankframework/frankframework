/*
 * $Log: MessageSendingPipe.java,v $
 * Revision 1.25  2007-02-05 14:59:40  europe\L190409
 * update javadoc
 *
 * Revision 1.24  2006/12/28 14:21:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.23  2006/12/13 16:29:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * catch null input
 *
 * Revision 1.22  2006/01/05 14:36:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.21  2005/10/24 09:20:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made namespaceAware an attribute of AbstractPipe
 *
 * Revision 1.20  2005/09/08 15:59:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * return something when asynchronous sender has no listener
 *
 * Revision 1.19  2005/08/24 15:53:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved error message for configuration exception
 *
 * Revision 1.18  2005/07/05 11:51:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging of receiving result
 *
 * Revision 1.17  2004/10/19 06:39:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified parameter handling, introduced IWithParameters
 *
 * Revision 1.16  2004/10/14 16:11:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ParameterResolutionContext from Object,Hashtable to String, PipelineSession
 *
 * Revision 1.15  2004/10/05 10:53:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for parameterized senders
 *
 * Revision 1.14  2004/09/08 14:16:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * catch more throwables in doPipe()
 *
 * Revision 1.13  2004/09/01 11:28:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added exception-forward
 *
 * Revision 1.12  2004/08/23 13:10:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated JavaDoc
 *
 * Revision 1.11  2004/08/09 13:52:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of function propagateName()
 * catches more exceptions in start()
 *
 * Revision 1.10  2004/07/19 13:23:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging + no exception but only warning on timeout if no timeoutforward exists
 *
 * Revision 1.9  2004/07/07 13:49:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved handling of timeout when no timeout-forward exists
 *
 * Revision 1.8  2004/06/21 09:58:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Changed exception handling for starting pipe; Exception thrown now contains pipename
 *
 * Revision 1.7  2004/05/21 07:59:30  unknown <unknown@ibissource.org>
 * Add (modifications) due to the postbox sender implementation
 *
 * Revision 1.6  2004/04/15 15:07:57  Johan Verrips <johan.verrips@ibissource.org>
 * when a timeout occured, the receiver was not closed. Fixed it.
 *
 * Revision 1.5  2004/03/30 07:30:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.ICorrelatedPullingListener;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.errormessageformatters.ErrorMessageFormatter;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;


import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

/**
 * Sends a message using a {@link ISender} and optionally receives a reply from the same sender, or from a {@link nl.nn.adapterframework.core.ICorrelatedPullingListener listener}.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.MessageSendingPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, nl.nn.adapterframework.core.PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPreserveInput(boolean) preserveInput}</td><td>when set <code>true</code>, the input of a pipe is restored before processing the next one</td><td>false</td></tr>
 * <tr><td>{@link #setNamespaceAware(boolean) namespaceAware}</td><td>controls namespace-awareness of possible XML parsing in descender-classes</td><td>application default</td></tr>
 * <tr><td>{@link #setTransactionAttribute(String) transactionAttribute}</td><td>Defines transaction and isolation behaviour. Equal to <A href="http://java.sun.com/j2ee/sdk_1.2.1/techdocs/guides/ejb/html/Transaction2.html#10494">EJB transaction attribute</a>. Possible values are: 
 *   <table border="1">
 *   <tr><th>transactionAttribute</th><th>callers Transaction</th><th>Pipe excecuted in Transaction</th></tr>
 *   <tr><td colspan="1" rowspan="2">Required</td>    <td>none</td><td>T2</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">RequiresNew</td> <td>none</td><td>T2</td></tr>
 * 											      <tr><td>T1</td>  <td>T2</td></tr>
 *   <tr><td colspan="1" rowspan="2">Mandatory</td>   <td>none</td><td>error</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">NotSupported</td><td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>none</td></tr>
 *   <tr><td colspan="1" rowspan="2">Supports</td>    <td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">Never</td>       <td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>error</td></tr>
 *  </table></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setBeforeEvent(int) beforeEvent}</td>      <td>METT eventnumber, fired just before a message is processed by this Pipe</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setAfterEvent(int) afterEvent}</td>        <td>METT eventnumber, fired just after message processing by this Pipe is finished</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setExceptionEvent(int) exceptionEvent}</td><td>METT eventnumber, fired when message processing by this Pipe resulted in an exception</td><td>-1 (disabled)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setResultOnTimeOut(String) resultOnTimeOut}</td><td>result returned when no return-message was received within the timeout limit</td><td>"receiver timed out"</td></tr>
 * <tr><td>{@link #setLinkMethod(String) linkMethod}</td><td>Indicates wether the server uses the correlationID or the messageID in the correlationID field of the reply</td><td>CORRELATIONID</td></tr>
 * <tr><td><code>sender.*</td><td>any attribute of the sender instantiated by descendant classes</td><td>&nbsp;</td></tr>
 * </table>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ISender sender}</td><td>specification of sender to send messages with</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ICorrelatedPullingListener listener}</td><td>specification of listener to listen to for replies</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the pipe will be handed to the sender, if this is a {@link nl.nn.adapterframework.core.ISenderWithParameters ISenderWithParameters}</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default when a good message was retrieved (synchronous sender), or the message was successfully sent and no listener was specified and the sender was not synchronous</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified, and otherwise under same condition as "success"</td></tr>
 * <tr><td>"timeout"</td><td>no data was received (timeout on listening), if the sender was synchronous or a listener was specified.</td></tr>
 * <tr><td>"exception"</td><td>an exception was thrown by the Sender or its reply-Listener. The result passed to the next pipe is the exception that was caught.</td></tr>
 * </table>
 * </p>
 * @version Id</p>
 * @author  Gerrit van Brakel
 */

public class MessageSendingPipe extends FixedForwardPipe implements HasSender {
	public static final String version = "$RCSfile: MessageSendingPipe.java,v $ $Revision: 1.25 $ $Date: 2007-02-05 14:59:40 $";
	private final static String TIMEOUTFORWARD = "timeout";
	private final static String EXCEPTIONFORWARD = "exception";

	private String resultOnTimeOut = "receiver timed out";
	private String linkMethod = "CORRELATIONID";

	private ISender sender = null;
	private ICorrelatedPullingListener listener = null;

	
	protected void propagateName() {
		ISender sender=getSender();
		if (sender!=null && StringUtils.isEmpty(sender.getName())) {
			sender.setName(getName() + "-sender");
		}
		ICorrelatedPullingListener listener=getListener();
		if (listener!=null && StringUtils.isEmpty(listener.getName())) {
			listener.setName(getName() + "-replylistener");
		}
	}

	public void setName(String name) {
		super.setName(name);
		propagateName();
	}

	public void addParameter(Parameter p){
		if (getSender() instanceof ISenderWithParameters && getParameterList()!=null) {
			((ISenderWithParameters)getSender()).addParameter(p);
		}
	}


	/**
	 * Checks whether a sender is defined for this pipe.
	 */
	public void configure() throws ConfigurationException {
		super.configure();
		propagateName();
		if (getSender() == null) {
			throw new ConfigurationException(
				getLogPrefix(null) + "no sender defined ");
		}

		try {
			getSender().configure();
		} catch (ConfigurationException e) {
			throw new ConfigurationException(getLogPrefix(null)+"while configuring sender",e);
		}
		if (getSender() instanceof HasPhysicalDestination) {
			log.info(getLogPrefix(null)+"has sender on "+((HasPhysicalDestination)getSender()).getPhysicalDestinationName());
		}
		if (getListener() != null) {
			if (getSender().isSynchronous()) {
				throw new ConfigurationException(
					getLogPrefix(null)
						+ "cannot have listener with synchronous sender");
			}
			try {
				getListener().configure();
			} catch (ConfigurationException e) {
				throw new ConfigurationException(getLogPrefix(null)+"while configuring listener",e);
			}
			if (getListener() instanceof HasPhysicalDestination) {
				log.info(getLogPrefix(null)+"has listener on "+((HasPhysicalDestination)getListener()).getPhysicalDestinationName());
			}
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
		if (input==null) {
			throw new PipeRunException(
				this,
				"received null as input");
		}
		if (!(input instanceof String)) {
			throw new PipeRunException(
				this,
				"String expected, got a [" + input.getClass().getName() + "]");
		}
		String result = null;
		ICorrelatedPullingListener replyListener = getListener();
		HashMap threadContext=new HashMap();
		try {
			String correlationID = session.getMessageId();

			String messageID = null;
			// sendResult has a messageID for async senders, the result for sync senders
			String sendResult = sendMessage(input, session, correlationID, getSender(), threadContext);

			if (getSender().isSynchronous()) {
				if (log.isInfoEnabled()) {
					log.info(getLogPrefix(session)+ "sent message to ["+ getSender().getName()+ "] synchronously");
				}
				result = sendResult;
			} else {
				messageID = sendResult;
				// if linkMethod is MESSAGEID overwrite correlationID with the messageID
				// as this will be used with the listener
				if (getLinkMethod().equalsIgnoreCase("MESSAGEID")) {
					correlationID = sendResult;
				}
				if (log.isInfoEnabled()) {
					log.info(getLogPrefix(session) + "sent message to [" + getSender().getName()+ "] messageID ["+ messageID+ "] correlationID ["+ correlationID+ "] linkMethod ["+ getLinkMethod()	+ "]");
				}
			}
			
			if (replyListener != null) {
				if (log.isDebugEnabled()) {
					log.debug(getLogPrefix(session)	+ "starts listening for return message with correlationID ["+ correlationID	+ "]");
				}
				threadContext = replyListener.openThread();
				Object msg = replyListener.getRawMessage(correlationID, threadContext);
				if (msg==null) {	
					log.info(getLogPrefix(session)+"received null reply message");
				} else {
					log.info(getLogPrefix(session)+"received reply message");
				}
				result =
					replyListener.getStringFromRawMessage(msg, threadContext);
			} else {
				result = sendResult;
			}
			if (result == null) {
				result = "";
			}
			return new PipeRunResult(getForward(), result);
		} catch (TimeOutException toe) {
			PipeForward timeoutForward = findForward(TIMEOUTFORWARD);
			if (timeoutForward==null) {
				log.warn(getLogPrefix(session) + "timeout occured, but no timeout-forward defined", toe);
				timeoutForward=getForward();
			} else {
				log.warn(getLogPrefix(session) + "timeout occured", toe);
			}
			return new PipeRunResult(timeoutForward,getResultOnTimeOut());

		} catch (Throwable t) {
			PipeForward exceptionForward = findForward(EXCEPTIONFORWARD);
			if (exceptionForward!=null) {
				log.warn(getLogPrefix(session) + "exception occured, forwarded to ["+exceptionForward.getPath()+"]", t);
				String resultmsg=new ErrorMessageFormatter().format(getLogPrefix(session),t,this,(String)input,session.getMessageId(),0);
				return new PipeRunResult(exceptionForward,resultmsg);
			}
			throw new PipeRunException(this, getLogPrefix(session) + "caught exception", t);
		} finally {
			if (getListener()!=null)
				try {
					log.debug(getLogPrefix(session)+"is closing listener");
					replyListener.closeThread(threadContext);
				} catch (ListenerException le) {
					log.error(getLogPrefix(session)+"got error closing listener", le);
				}
		}
	}

	protected String sendMessage(Object input, PipeLineSession session, String correlationID, ISender sender, HashMap threadContext) throws SenderException, TimeOutException {
		// sendResult has a messageID for async senders, the result for sync senders
		if (sender instanceof ISenderWithParameters && getParameterList()!=null) {
			ISenderWithParameters psender = (ISenderWithParameters) sender;
			ParameterResolutionContext prc = new ParameterResolutionContext((String)input, session, isNamespaceAware());
			return psender.sendMessage(correlationID, (String) input, prc);
		} 
		return sender.sendMessage(correlationID, (String) input);
	}

	public void start() throws PipeStartException {
		try {
			getSender().open();
			if (getListener() != null) {
				getListener().open();
			}

		} catch (Throwable t) {
			PipeStartException pse = new PipeStartException(getLogPrefix(null)+"could not start", t);
			pse.setPipeNameInError(getName());
			throw pse;
		}
	}
	public void stop() {
		log.info(getLogPrefix(null) + "is closing");
		try {
			getSender().close();
		} catch (SenderException e) {
			log.warn(getLogPrefix(null) + "exception closing sender", e);
		}
		if (getListener() != null) {
			try {
				log.info(getLogPrefix(null) + "is closing; closing listener");
				getListener().close();
			} catch (ListenerException e) {
				log.warn(getLogPrefix(null) + "Exception closing listener", e);
			}
		}
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
	public ICorrelatedPullingListener getListener() {
		return listener;
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
	public ISender getSender() {
		return sender;
	}
	
	/**
	 * The message that is returned when the time listening for a reply message
	 * exceeds the timeout, or in other situations no reply message is received.
	 */
	public void setResultOnTimeOut(String newResultOnTimeOut) {
		resultOnTimeOut = newResultOnTimeOut;
	}
	public String getResultOnTimeOut() {
		return resultOnTimeOut;
	}

	/**
	 * For asynchronous communication, the server side may either use the messageID or the correlationID
	 * in the correlationID field of the reply message. Use this property to set the behaviour of the reply-listener.
	 * <ul>
	 * <li>Use <code>MESSAGEID</code> to let the listener wait for a message with the messageID of the
	 * sent message in the correlation ID field</li>
	 * <li>Use <code>CORRELATIONID</code> to let the listener wait for a message with the correlationID of the
	 * sent message in the correlation ID field</li>
	 * </ul>
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

}
