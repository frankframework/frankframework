/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2022-2023 WeAreFrank!

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
import java.util.Map;

import javax.jms.Destination;
import javax.jms.QueueSession;

import com.ing.ifsa.IFSAMessage;
import com.ing.ifsa.IFSAServiceName;
import com.ing.ifsa.IFSAServicesProvided;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IListenerConnector;
import nl.nn.adapterframework.core.IListenerConnector.CacheMode;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPortConnectedListener;
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.core.ITransactionRequirements;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.extensions.ifsa.IfsaException;
import nl.nn.adapterframework.extensions.ifsa.IfsaMessageProtocolEnum;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.receivers.Receiver;

/**
 * Implementation of {@link IPortConnectedListener} that acts as an IFSA-service.
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
 *   <li>ifsaBifName</li>
 *   <li>ifsaBtcData</li>
 * </ul>
 * N.B.
 * Starting from IFSA-jms version 2.2.10.055(beta) a feature was created to have separate service-queues for Request/Reply
 * and for Fire & Forget services. This allows applications to provide both types of services, each in its own transaction
 * mode. This options is not compatible with earlier versions of IFSA-jms. If an earlier version of IFSA-jms is deployed on
 * the server, this behaviour must be disabled by the following setting in DeploymentSpecifics.properties:
 *
 * <code>ifsa.provider.useSelectors=false</code>
 *
 * <p>
 * For Fire&Forget providers, the message log might get cluttered with messages like:
 * <code><pre>
   [1-10-08 17:10:34:382 CEST] 209d4317 ConnectionMan W J2CA0075W: An active transaction should be present while processing method allocateMCWrapper.
   [1-10-08 17:10:34:382 CEST] 209d4317 ConnectionMan W J2CA0075W: An active transaction should be present while processing method initializeForUOW
 * </pre></code>
 * This is due to a IFSA requirement, that sessions be created using a parameter transacted=true, indicating
 * JMS transacted sessions.
 * </p>
 *
 * @author  Gerrit van Brakel
 * @since   4.2
 */
public class PushingIfsaProviderListener extends IfsaListener implements IPortConnectedListener<IFSAMessage>, IThreadCountControllable, ITransactionRequirements {


	private @Getter String listenerPort;
	private @Getter CacheMode cacheMode; // default is set in spring container

	private @Getter @Setter IListenerConnector jmsConnector;
	private @Getter @Setter IMessageHandler<IFSAMessage> handler;
	private @Getter @Setter Receiver receiver;
	private @Getter @Setter IbisExceptionListener exceptionListener;

	public PushingIfsaProviderListener() {
		super(); //instantiate as a provider
		setTimeOut(3000); // set default timeout, to be able to stop adapter!
	}


	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (jmsConnector==null) {
			throw new ConfigurationException(getLogPrefix()+" has no jmsConnector. It should be configured via springContext.xml");
		}
		Destination destination=null;
		try {
			destination=getServiceQueue();
		} catch (Exception e) {
			throw new ConfigurationException(getLogPrefix()+"could not get Destination",e);
		}
		try {
			jmsConnector.configureEndpointConnection(this, getMessagingSource().getConnectionFactory(), null,
					destination, getExceptionListener(), getCacheMode(), getAckMode(), isJmsTransacted(),
					getProviderSelector(), getTimeOut(), -1);
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
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
			jmsConnector.start();
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix(),e);
		}
	}


	@Override
	public void close() throws ListenerException {
		try {
			jmsConnector.stop();
			closeService();
		} catch (IfsaException e) {
			throw new ListenerException(getLogPrefix(),e);
		}
	}

	@Override
	public boolean transactionalRequired() {
		return this.getMessageProtocolEnum()==IfsaMessageProtocolEnum.FIRE_AND_FORGET;
	}

	@Override
	public boolean transactionalAllowed() {
		return true;
	}

	@Override
	public void afterMessageProcessed(PipeLineResult plr, RawMessageWrapper<IFSAMessage> rawMessageWrapper, Map<String,Object> threadContext) throws ListenerException {
		QueueSession session= (QueueSession) threadContext.get(IListenerConnector.THREAD_CONTEXT_SESSION_KEY);

		// on request-reply send the reply.
		if (getMessageProtocolEnum() == IfsaMessageProtocolEnum.REQUEST_REPLY) {
			javax.jms.Message originalRawMessage;
			if (rawMessageWrapper instanceof javax.jms.Message) {
				originalRawMessage = (javax.jms.Message)rawMessageWrapper;
			} else {
				originalRawMessage = (javax.jms.Message)threadContext.get(THREAD_CONTEXT_ORIGINAL_RAW_MESSAGE_KEY);
			}
			if (originalRawMessage==null) {
				String cid = (String) threadContext.get(PipeLineSession.correlationIdKey);
				log.warn(getLogPrefix()+"no original raw message found for correlationId ["+cid+"], cannot send result");
			} else {
				if (session==null) {
					throw new ListenerException(getLogPrefix()+"no session found in context, cannot send result");
				}
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
				}
			}
		}
	}


	@Override
	public IListenerConnector getListenerPortConnector() {
		return jmsConnector;
	}

	/**
	 * Name of the WebSphere listener port that this JMS Listener binds to. Optional.
	 *
	 * This property is only used in EJB Deployment mode and has no effect otherwise.
	 * If it is not set in EJB Deployment Mode, then the listener port name is
	 * constructed by the EjbListenerPortConnector from
	 * the Listener name, Adapter name and the Receiver name.
	 *
	 * @param listenerPort Name of the listener port, as configured in the application
	 * server.
	 */
	public void setListenerPort(String listenerPort) {
		this.listenerPort = listenerPort;
	}

	/**
	 * Controls caching of JMS objects. Must be one of CACHE_NONE, CACHE_CONNECTION, CACHE_SESSION, CACHE_CONSUMER
	 */
	public void setCacheMode(CacheMode cacheMode) {
		this.cacheMode = cacheMode;
	}

	@Override
	public boolean isThreadCountReadable() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			return tcc.isThreadCountReadable();
		}
		return false;
	}

	@Override
	public boolean isThreadCountControllable() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			return tcc.isThreadCountControllable();
		}
		return false;
	}

	@Override
	public int getCurrentThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			return tcc.getCurrentThreadCount();
		}
		return -1;
	}

	@Override
	public int getMaxThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			return tcc.getMaxThreadCount();
		}
		return -1;
	}

	@Override
	public void increaseThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			tcc.increaseThreadCount();
		}
	}

	@Override
	public void decreaseThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			tcc.decreaseThreadCount();
		}
	}

	@Override
	public RawMessageWrapper<IFSAMessage> wrapRawMessage(IFSAMessage rawMessage, Map<String, Object> threadContext) {
		getIdFromRawMessage(rawMessage, threadContext);
		String mid = (String) threadContext.get(PipeLineSession.messageIdKey);
		String cid = (String) threadContext.get(PipeLineSession.correlationIdKey);
		return new RawMessageWrapper<>(rawMessage, mid, cid);
	}
}
