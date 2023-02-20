/*
   Copyright 2013 Nationale-Nederlanden, 2021, 2022 WeAreFrank!

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
package nl.nn.adapterframework.unmanaged;

import java.util.Timer;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Supplier;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IListenerConnector;
import nl.nn.adapterframework.core.IPortConnectedListener;
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.jms.IbisMessageListenerContainer;
import nl.nn.adapterframework.jms.PushingJmsListener;
import nl.nn.adapterframework.util.Counter;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.JtaUtil;

/**
 * Configure a Spring JMS Container from a {@link PushingJmsListener}.
 *
 * <p>
 * This implementation expects to receive an instance of
 * org.springframework.jms.listener.DefaultMessageListenerContainer
 * from the Spring BeanFactory. If another type of MessageListenerContainer
 * is created by the BeanFactory, then another implementation of IListenerConnector
 * should be provided as well.
 * </p>
 * <p>
 * This implementation works only with a PushingJmsListener, and not with other types PortConnectedListeners.
 * </p>
 * @author  Tim van der Leeuw
 * @since   4.8
 */
public class SpringJmsConnector extends AbstractJmsConfigurator implements IListenerConnector<Message>, IThreadCountControllable, BeanFactoryAware, ExceptionListener, SessionAwareMessageListener<Message> {

	private @Getter @Setter PlatformTransactionManager txManager;
	private @Setter BeanFactory beanFactory;
	private @Getter DefaultMessageListenerContainer jmsContainer;
	private @Getter @Setter String messageListenerClassName;

	public static final int DEFAULT_CACHE_LEVEL_TRANSACTED=DefaultMessageListenerContainer.CACHE_NONE;
//	public static final int DEFAULT_CACHE_LEVEL_NON_TRANSACTED=DefaultMessageListenerContainer.CACHE_CONSUMER;
	public static final int DEFAULT_CACHE_LEVEL_NON_TRANSACTED=DefaultMessageListenerContainer.CACHE_NONE;

	public static final int IDLE_TASK_EXECUTION_LIMIT=1000;

	private CredentialFactory credentialFactory;
	private CacheMode cacheMode;
	private int acknowledgeMode;
	private boolean sessionTransacted;
	private String messageSelector;
	private long receiveTimeout;

	private TransactionDefinition txDefinition = null;

	final @Getter Counter threadsProcessing = new Counter(0);

	private @Getter @Setter long lastPollFinishedTime;
	private long pollGuardInterval;
	private Timer pollGuardTimer;

	protected DefaultMessageListenerContainer createMessageListenerContainer() throws ConfigurationException {
		try {
			Class<?> klass = Class.forName(messageListenerClassName);
			return (DefaultMessageListenerContainer) klass.newInstance();
		} catch (Exception e) {
			throw new ConfigurationException(getLogPrefix()+"error creating instance of MessageListenerContainer ["+messageListenerClassName+"]", e);
		}
	}

	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.configuration.IListenerConnector#configureReceiver(nl.nn.adapterframework.jms.PushingJmsListener)
	 */
	@Override
	public void configureEndpointConnection(final IPortConnectedListener<Message> jmsListener,
			ConnectionFactory connectionFactory, CredentialFactory credentialFactory, Destination destination,
			IbisExceptionListener exceptionListener, CacheMode cacheMode, int acknowledgeMode, boolean sessionTransacted,
			String messageSelector, long receiveTimeout, long pollGuardInterval) throws ConfigurationException {
		super.configureEndpointConnection(jmsListener, connectionFactory, destination, exceptionListener);
		this.credentialFactory = credentialFactory;
		this.cacheMode = cacheMode;
		this.acknowledgeMode = acknowledgeMode;
		this.sessionTransacted = sessionTransacted;
		this.messageSelector = messageSelector;
		this.receiveTimeout = receiveTimeout;
		this.pollGuardInterval = pollGuardInterval;
		configureEndpointConnection();
	}

	private void configureEndpointConnection() throws ConfigurationException {
		// Create the Message Listener Container manually.
		// This is needed, because otherwise the Spring Factory will
		// call afterPropertiesSet() on the object which will validate
		// that all required properties are set before we get a chance
		// to insert our dynamic values from the config. file.
		jmsContainer = createMessageListenerContainer();

		if (jmsContainer instanceof IbisMessageListenerContainer) {
			IbisMessageListenerContainer ibisMessageListenerContainer = (IbisMessageListenerContainer)jmsContainer;
			ibisMessageListenerContainer.setCredentialFactory(credentialFactory);
		}

		if (getReceiver().isTransacted()) {
			log.debug("{} setting transaction manager to [{}]", this::getLogPrefix, ()->txManager);
			jmsContainer.setTransactionManager(txManager);
			if (getReceiver().getTransactionTimeout()>0) {
				jmsContainer.setTransactionTimeout(getReceiver().getTransactionTimeout());
			}
			txDefinition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
			if (receiveTimeout > txDefinition.getTimeout() && txDefinition.getTimeout() != -1) {
				throw new ConfigurationException(getLogPrefix() + "receive timeout [" + receiveTimeout
						+ "] should be smaller than transaction timeout [" + txDefinition.getTimeout()
						+ "] as the receive time is part of the total transaction time");
			}
		} else {
			log.debug("{} setting no transaction manager", this::getLogPrefix);
		}
		if (sessionTransacted) {
			jmsContainer.setSessionTransacted(sessionTransacted);
		}
		if (StringUtils.isNotEmpty(messageSelector)) {
			jmsContainer.setMessageSelector(messageSelector);
		}
		jmsContainer.setReceiveTimeout(receiveTimeout);

		// Initialize with a number of dynamic properties which come from the configuration file
		jmsContainer.setConnectionFactory(getConnectionFactory());
		jmsContainer.setDestination(getDestination());

		jmsContainer.setExceptionListener(this);

		if (getReceiver().getNumThreads() > 0) {
			jmsContainer.setMaxConcurrentConsumers(getReceiver().getNumThreads());
		} else {
			jmsContainer.setMaxConcurrentConsumers(1);
		}
		jmsContainer.setIdleTaskExecutionLimit(IDLE_TASK_EXECUTION_LIMIT);

		if (cacheMode!=null) {
			jmsContainer.setCacheLevelName(cacheMode.name());
		} else {
			if (getReceiver().isTransacted()) {
				jmsContainer.setCacheLevel(DEFAULT_CACHE_LEVEL_TRANSACTED);
			} else {
				jmsContainer.setCacheLevel(DEFAULT_CACHE_LEVEL_NON_TRANSACTED);
			}
		}
		if (acknowledgeMode>=0) {
			jmsContainer.setSessionAcknowledgeMode(acknowledgeMode);
		}
		jmsContainer.setMessageListener(this);
		// Use Spring BeanFactory to complete the auto-wiring of the JMS Listener Container,
		// and run the bean lifecycle methods.
		try {
			((AutowireCapableBeanFactory) this.beanFactory).configureBean(this.jmsContainer, "proto-jmsContainer");
		} catch (BeansException e) {
			throw new ConfigurationException(getLogPrefix()+"Out of luck wiring up and configuring Default JMS Message Listener Container for JMS Listener ["+ (getListener().getName()+"]"), e);
		}

		// Finally, set bean name to something we can make sense of
		if (getListener().getName() != null) {
			jmsContainer.setBeanName(getListener().getName());
		} else {
			jmsContainer.setBeanName(getReceiver().getName());
		}
	}

	@Override
	public void start() throws ListenerException {
		log.debug("{} starting", this::getLogPrefix);
		if (jmsContainer == null) {
			try {
				configureEndpointConnection();
			} catch (ConfigurationException e) {
				throw new ListenerException(e);
			}
		}
		if (jmsContainer!=null) {
			try {
				jmsContainer.start();
				if (pollGuardInterval != -1 && jmsContainer instanceof IbisMessageListenerContainer) {
					log.trace("{} Creating poll-guard timer with interval [{}ms] while starting SpringJmsConnector", this::getLogPrefix, () -> pollGuardInterval);
					PollGuard pollGuard = new PollGuard();
					pollGuard.setSpringJmsConnector(this);
					pollGuardTimer = new Timer(true);
					pollGuardTimer.schedule(pollGuard, pollGuardInterval, pollGuardInterval);
				}
			} catch (Exception e) {
				throw new ListenerException(getLogPrefix()+"cannot start", e);
			}
		} else {
			throw new ListenerException(getLogPrefix()+"no jmsContainer defined");
		}
	}

	@Override
	public void stop() throws ListenerException {
		log.debug("{} stopping", this::getLogPrefix);
		if (pollGuardTimer != null) {
			log.debug("Cancelling previous poll-guard timer while stopping SpringJmsConnector");
			pollGuardTimer.cancel();
			pollGuardTimer = null;
		}
		if (jmsContainer!=null) {
			try {
				jmsContainer.stop();
				jmsContainer.destroy();
				jmsContainer = null;
				log.debug("{} jmsContainer is destroyed", this::getLogPrefix);
			} catch (Exception e) {
				throw new ListenerException(getLogPrefix()+"Exception while trying to stop", e);
			}
		} else {
			throw new ListenerException(getLogPrefix()+"no jmsContainer defined");
		}
	}


	@Override
	public void onMessage(Message message, Session session)	throws JMSException {
		TransactionStatus txStatus=null;

		final long onMessageStart= System.currentTimeMillis();
		final long jmsTimestamp= message.getJMSTimestamp();
		threadsProcessing.increase();
		Thread.currentThread().setName(getReceiver().getName()+"["+threadsProcessing.getValue()+"]");

		final String logPrefix = getLogPrefix();
		try (PipeLineSession pipeLineSession = new PipeLineSession()) {
			if (txDefinition != null) {
				txStatus = txManager.getTransaction(txDefinition);
			}

			try {
				IPortConnectedListener<Message> listener = getListener();
				listener.checkTransactionManagerValidity();
				pipeLineSession.put(THREAD_CONTEXT_SESSION_KEY, session);
				if (log.isTraceEnabled()) log.trace("transaction status before processRawMessage: "+JtaUtil.displayTransactionStatus(txStatus));
				getReceiver().processRawMessage(listener, message, pipeLineSession, false);
				if (log.isTraceEnabled()) log.trace("transaction status after processRawMessage: "+JtaUtil.displayTransactionStatus(txStatus));
			} catch (ListenerException e) {
				getReceiver().increaseRetryIntervalAndWait(e, logPrefix);
				if (txStatus != null) {
					txStatus.setRollbackOnly();
				}
			} finally {
				if (txStatus == null && jmsContainer.isSessionTransacted()) {
					log.debug("{} committing JMS session", logPrefix);
					session.commit();
				}
			}
		} finally {
			if (txStatus!=null) {
				log.debug("{} committing transaction {}", logPrefix, txStatus);
				txManager.commit(txStatus);
			}
			threadsProcessing.decrease();
			if (log.isInfoEnabled()) {
				long onMessageEnd= System.currentTimeMillis();

				log.info("{} A) JMSMessageTime [{}]", logPrefix, DateUtils.format(jmsTimestamp));
				log.info("{} B) onMessageStart [{}] diff (~'queing' time) [{}]", logPrefix, DateUtils.format(onMessageStart), (onMessageStart-jmsTimestamp));
				log.info("{} C) onMessageEnd   [{}] diff (process time) [{}]", logPrefix, DateUtils.format(onMessageEnd), (onMessageEnd-onMessageStart));
			}
		}
	}

	@Override
	public void onException(JMSException e) {
		IbisExceptionListener ibisExceptionListener = getExceptionListener();
		if (ibisExceptionListener!= null) {
			ibisExceptionListener.exceptionThrown(getListener(), e);
		} else {
			log.error("{} Cannot report the error to an IBIS Exception Listener", (Supplier<?>) this::getLogPrefix, e);
		}
	}


	@Override
	public boolean isThreadCountReadable() {
		return jmsContainer!=null;
	}
	@Override
	public boolean isThreadCountControllable() {
		return jmsContainer!=null;
	}

	@Override
	public int getCurrentThreadCount() {
		if (jmsContainer!=null) {
			return jmsContainer.getActiveConsumerCount();
		}
		return 0;
	}

	@Override
	public int getMaxThreadCount() {
		if (jmsContainer!=null) {
			return jmsContainer.getMaxConcurrentConsumers();
		}
		return 0;
	}

	@Override
	public void increaseThreadCount() {
		if (jmsContainer!=null) {
			jmsContainer.setMaxConcurrentConsumers(jmsContainer.getMaxConcurrentConsumers()+1);
		}
	}

	@Override
	public void decreaseThreadCount() {
		if (jmsContainer!=null) {
			int current=getMaxThreadCount();
			if (current>1) {
				jmsContainer.setMaxConcurrentConsumers(current-1);
			}
		}
	}


	public String getLogPrefix() {
		String result="SpringJmsConnector ";
		if (getListener()!=null && getListener().getReceiver()!=null) {
			result += "of Receiver ["+getListener().getReceiver().getName()+"] ";
		}
		return result;
	}


}

