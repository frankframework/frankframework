/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2023 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.extensions.ifsa.jms;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueReceiver;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.ing.ifsa.IFSAHeader;
import com.ing.ifsa.IFSAMessage;
import com.ing.ifsa.IFSAPoisonMessage;
import com.ing.ifsa.IFSAServiceName;
import com.ing.ifsa.IFSAServicesProvided;
import com.ing.ifsa.IFSATextMessage;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.extensions.ifsa.IfsaException;
import nl.nn.adapterframework.extensions.ifsa.IfsaMessageProtocolEnum;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.RunState;
import nl.nn.adapterframework.util.RunStateEnquirer;
import nl.nn.adapterframework.util.RunStateEnquiring;

/**
 * Implementation of {@link IPullingListener} that acts as an IFSA-service.
 *
 * There is no need or possibility to set the ServiceId as the Provider will receive all messages
 * for this Application on the same serviceQueue.
 *
 * The following session keys are set for each message:
 * <ul>
 *   <li>id (the message id)</li>
 *   <li>cid (the correlation id)</li>
 *   <li>timestamp</li>
 *   <li>replyTo</li>
 *   <li>messageText</li>
 *   <li>fullIfsaServiceName</li>
 *   <li>ifsaServiceName</li>
 *   <li>ifsaGroup</li>
 *   <li>ifsaOccurrence</li>
 *   <li>ifsaVersion</li>
 * </ul>
 * N.B.
 * Starting from IFSA-jms version 2.2.10.055(beta) a feature was created to have separate service-queues for Request/Reply
 * and for Fire & Forget services. This allows applications to provide both types of services, each in its own transaction
 * mode. This options is not compatible with earlier versions of IFSA-jms. If an earlier version of IFSA-jms is deployed on
 * the server, this behaviour must be disabled by the following setting in DeploymentSpecifics.properties:
 *
 * <code>ifsa.provider.useSelectors=false</code>
 *
 * @author  Gerrit van Brakel
 * @since   4.2
 */
public class PullingIfsaProviderListener extends IfsaListener implements IPullingListener<IFSAMessage>, INamedObject, RunStateEnquiring {

	private static final String THREAD_CONTEXT_SESSION_KEY = "session";
	private static final String THREAD_CONTEXT_RECEIVER_KEY = "receiver";
	private RunStateEnquirer runStateEnquirer=null;

	public PullingIfsaProviderListener() {
		super(); //instantiate as a provider
		setTimeOut(3000); // set default timeout, to be able to stop adapter!
	}


	protected QueueSession getSession(Map<String, Object> threadContext) throws ListenerException {
		if (isSessionsArePooled()) {
			try {
				return createSession();
			} catch (IfsaException e) {
				throw new ListenerException(getLogPrefix()+"exception creating QueueSession", e);
			}
		}
		return (QueueSession) threadContext.get(THREAD_CONTEXT_SESSION_KEY);
	}

	protected void releaseSession(Session session) throws ListenerException {
		if (isSessionsArePooled()) {
			closeSession(session);
		}
	}

	protected QueueReceiver getReceiver(Map threadContext, QueueSession session) throws ListenerException {
		if (isSessionsArePooled()) {
			try {
				return getServiceReceiver(session);
			} catch (IfsaException e) {
				throw new ListenerException(getLogPrefix()+"exception creating QueueReceiver", e);
			}
		}
		return (QueueReceiver) threadContext.get(THREAD_CONTEXT_RECEIVER_KEY);
	}

	protected void releaseReceiver(QueueReceiver receiver) throws ListenerException {
		if (isSessionsArePooled() && receiver != null) {
			try {
				receiver.close();
				// do not write to log, this occurs too often
			} catch (Exception e) {
				throw new ListenerException(getLogPrefix()+"exception closing QueueReceiver", e);
			}
		}
	}


	@Override
	public void configure() throws ConfigurationException {
		super.configure();
//		if (IfsaMessageProtocolEnum.FIRE_AND_FORGET.equals(getMessageProtocolEnum())) {
//			if (!isXaEnabledForSure()) {
//				if (isNotXaEnabledForSure()) {
//					log.warn(getLogPrefix()+"The installed IFSA libraries do not have XA enabled. Transaction integrity cannot be fully guaranteed");
//				} else {
//					log.warn(getLogPrefix()+"XA-support of the installed IFSA libraries cannot be determined. It is assumed XA is NOT enabled. Transaction integrity cannot be fully guaranteed");
//				}
//			}
//		}
	}



	@Override
	public void open() throws ListenerException {
		try {
			openService();

			IFSAServicesProvided services = getServiceQueue().getIFSAServicesProvided();

			for (int i = 0; i < services.getNumberOfServices(); i++) {
				IFSAServiceName service = services.getService(i);

				String protocol=(service.IsFireAndForgetService() ? "Fire and Forget" : "Request/Reply");
				log.info(getLogPrefix()+"providing ServiceName ["+service.getServiceName()+"] ServiceGroup [" + service.getServiceGroup()+"] protocol [" + protocol+"] ServiceVersion [" + service.getServiceVersion()+"]");
			}
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix(),e);
		}
	}

	@Override
	public Map<String,Object> openThread() throws ListenerException {
		Map<String,Object> threadContext = new HashMap<>();

		try {
			if (!isSessionsArePooled()) {
				QueueSession session = createSession();
				threadContext.put(THREAD_CONTEXT_SESSION_KEY, session);

				QueueReceiver receiver;
				receiver = getServiceReceiver(session);
				threadContext.put(THREAD_CONTEXT_RECEIVER_KEY, receiver);
			}
			return threadContext;
		} catch (IfsaException e) {
			throw new ListenerException(getLogPrefix()+"exception in openThread()", e);
		}
	}

	@Override
	public void close() throws ListenerException {
		try {
			closeService();
		} catch (IfsaException e) {
			throw new ListenerException(getLogPrefix(),e);
		}
	}

	@Override
	public void closeThread(Map threadContext) throws ListenerException {

		if (!isSessionsArePooled()) {
			QueueReceiver receiver = (QueueReceiver) threadContext.remove(THREAD_CONTEXT_RECEIVER_KEY);
			releaseReceiver(receiver);

			QueueSession session = (QueueSession) threadContext.remove(THREAD_CONTEXT_SESSION_KEY);
			closeSession(session);
		}
	}


	@Override
	public void afterMessageProcessed(PipeLineResult plr, RawMessageWrapper<IFSAMessage> rawMessage, Map<String, Object> threadContext) throws ListenerException {

		try {
			if (isJmsTransacted() && !(getMessagingSource().isXaEnabledForSure() && JtaUtil.inTransaction())) {
				QueueSession session = (QueueSession) threadContext.get(THREAD_CONTEXT_SESSION_KEY);

				try {
					session.commit();
				} catch (JMSException e) {
					log.error(getLogPrefix()+"got error committing the received message", e);
				}
				if (isSessionsArePooled()) {
					threadContext.remove(THREAD_CONTEXT_SESSION_KEY);
					releaseSession(session);
				}
			}
		} catch (Exception e) {
			log.error(getLogPrefix()+"exception in closing or releasing session", e);
		}
		// on request-reply send the reply.
		if (getMessageProtocolEnum() == IfsaMessageProtocolEnum.REQUEST_REPLY) {
			javax.jms.Message originalRawMessage;
			if (rawMessage instanceof javax.jms.Message) {
				originalRawMessage = (javax.jms.Message)rawMessage;
			} else {
				originalRawMessage = (javax.jms.Message)threadContext.get(THREAD_CONTEXT_ORIGINAL_RAW_MESSAGE_KEY);
			}
			if (originalRawMessage==null) {
				String id = (String) threadContext.get(PipeLineSession.messageIdKey);
				String cid = (String) threadContext.get(PipeLineSession.correlationIdKey);
				log.warn(getLogPrefix()+"no original raw message found for messageId ["+id+"] correlationId ["+cid+"], cannot send result");
			} else {
				QueueSession session = getSession(threadContext);
				try {
					String result="<exception>no result</exception>";
					if (plr!=null && plr.getResult()!=null) {
						result=plr.getResult().asString();
					}
					sendReply(session, originalRawMessage, result);
				} catch (IfsaException | IOException e) {
					try {
						sendReply(session, originalRawMessage, "<exception>"+e.getMessage()+"</exception>");
					} catch (IfsaException e2) {
						log.warn(getLogPrefix()+"exception sending errormessage as reply",e2);
					}
					throw new ListenerException(getLogPrefix()+"Exception on sending result", e);
				} finally {
					releaseSession(session);
				}
			}
		}
	}

	private boolean sessionNeedsToBeSavedForAfterProcessMessage(Object result)
	{
		try {
			return isJmsTransacted() &&
					!(getMessagingSource().isXaEnabledForSure() && JtaUtil.inTransaction()) &&
					isSessionsArePooled()&&
					result != null &&
					!(result instanceof IFSAPoisonMessage) ;
		} catch (Throwable t) {
			log.warn(t);
			return false;
		}
	}

	/**
	 * Retrieves messages to be processed by the server, implementing an IFSA-service, but does no processing on it.
	 */
	@Override
	public RawMessageWrapper<IFSAMessage> getRawMessage(Map<String, Object> threadContext) throws ListenerException {
		javax.jms.Message result=null;
		QueueSession session=null;
		QueueReceiver receiver=null;

		threadContext.remove(THREAD_CONTEXT_ORIGINAL_RAW_MESSAGE_KEY);
		try {
			session = getSession(threadContext);
			try {
				receiver = getReceiver(threadContext, session);
				result = receiver.receive(getTimeOut());
				while (result==null && canGoOn() && !JtaUtil.inTransaction()) {
					result = receiver.receive(getTimeOut());
				}
			} catch (Exception e) {
				throw new ListenerException(getLogPrefix(),e);
			} finally {
				releaseReceiver(receiver);
			}
		} finally {
			if (sessionNeedsToBeSavedForAfterProcessMessage(result)) {
				threadContext.put(THREAD_CONTEXT_SESSION_KEY, session);
			} else {
				releaseSession(session);
			}
		}

		if (result == null) {
			return null;
		}

		if (result instanceof IFSAPoisonMessage) {
			IFSAHeader header = ((IFSAPoisonMessage) result).getIFSAHeader();
			String source;
			try {
				source = header.getIFSA_Source();
			} catch (Exception e) {
				source = "unknown due to exception:"+e.getMessage();
			}
			String msg=getLogPrefix()+ "received IFSAPoisonMessage "
						+ "source [" + source + "]"
						+ "content [" + ToStringBuilder.reflectionToString(result) + "]";
			log.warn(msg);
		}
		return wrapRawMessage(result, threadContext);
	}

	public RawMessageWrapper<IFSAMessage> wrapRawMessage(Message rawMessage, Map<String, Object> threadContext) throws ListenerException {
		RawMessageWrapper<IFSAMessage> rawMessageWrapper;
		try {
			if ((rawMessage instanceof IFSATextMessage || rawMessage instanceof IFSAPoisonMessage) &&
				 JtaUtil.inTransaction()
				) {
				// TODO: Cleanup rawMessageWrapper creation, and storing of original raw message
				threadContext.put(THREAD_CONTEXT_ORIGINAL_RAW_MESSAGE_KEY, rawMessage);
				populateContextFromMessage((IFSAMessage) rawMessage, threadContext);
				String mid = (String) threadContext.get(PipeLineSession.messageIdKey);
				String cid = (String) threadContext.get(PipeLineSession.correlationIdKey);
				rawMessageWrapper = new RawMessageWrapper<>((IFSAMessage) rawMessage, mid, cid);
			} else {
				rawMessageWrapper = new RawMessageWrapper<>((IFSAMessage) rawMessage, rawMessage.getJMSMessageID(), rawMessage.getJMSCorrelationID());
			}
		} catch (Exception e) {
			throw new ListenerException("cannot wrap message in wrapper",e);
		}
		return rawMessageWrapper;
	}

	protected boolean canGoOn() {
		return runStateEnquirer!=null && runStateEnquirer.getRunState() == RunState.STARTED;
	}

	@Override
	public void SetRunStateEnquirer(RunStateEnquirer enquirer) {
		runStateEnquirer=enquirer;
	}
}
