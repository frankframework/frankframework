/*
   Copyright 2013 Nationale-Nederlanden

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

import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IListenerConnector;
import nl.nn.adapterframework.core.IPortConnectedListener;
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.util.Counter;
import nl.nn.adapterframework.util.DateUtils;

import org.apache.commons.lang.StringUtils;
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

/**
 * Configure a Spring JMS Container from a {@link nl.nn.adapterframework.jms.PushingJmsListener}.
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
 * @version $Id$
 */
public class SpringJmsConnector extends AbstractJmsConfigurator implements IListenerConnector, IThreadCountControllable, BeanFactoryAware, ExceptionListener, SessionAwareMessageListener {

 	private PlatformTransactionManager txManager;
	private BeanFactory beanFactory;
	private DefaultMessageListenerContainer jmsContainer;
	private String messageListenerClassName;
    
    public static final int DEFAULT_CACHE_LEVEL_TRANSACTED=DefaultMessageListenerContainer.CACHE_NONE;
//	public static final int DEFAULT_CACHE_LEVEL_NON_TRANSACTED=DefaultMessageListenerContainer.CACHE_CONSUMER;
	public static final int DEFAULT_CACHE_LEVEL_NON_TRANSACTED=DefaultMessageListenerContainer.CACHE_NONE;
	
//	public static final int MAX_MESSAGES_PER_TASK=100;
	public static final int IDLE_TASK_EXECUTION_LIMIT=1000;
 
	private TransactionDefinition TX = null;

	final Counter threadsProcessing = new Counter(0);
    
	protected DefaultMessageListenerContainer createMessageListenerContainer() throws ConfigurationException {
		try {
			Class klass = Class.forName(messageListenerClassName);
			return (DefaultMessageListenerContainer) klass.newInstance();
		} catch (Exception e) {
			throw new ConfigurationException(getLogPrefix()+"error creating instance of MessageListenerContainer ["+messageListenerClassName+"]", e);
		}
	}
    
	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.configuration.IListenerConnector#configureReceiver(nl.nn.adapterframework.jms.PushingJmsListener)
	 */
	public void configureEndpointConnection(final IPortConnectedListener jmsListener, ConnectionFactory connectionFactory, Destination destination, IbisExceptionListener exceptionListener, String cacheMode, int acknowledgeMode, boolean sessionTransacted, String messageSelector) throws ConfigurationException {
		super.configureEndpointConnection(jmsListener, connectionFactory, destination, exceptionListener);
        
		// Create the Message Listener Container manually.
		// This is needed, because otherwise the Spring Factory will
		// call afterPropertiesSet() on the object which will validate
		// that all required properties are set before we get a chance
		// to insert our dynamic values from the config. file.
		this.jmsContainer = createMessageListenerContainer();
		
        
		if (getReceiver().isTransacted()) {
			log.debug(getLogPrefix()+"setting transction manager to ["+txManager+"]");
			jmsContainer.setTransactionManager(txManager);
			if (getReceiver().getTransactionTimeout()>0) {
				jmsContainer.setTransactionTimeout(getReceiver().getTransactionTimeout());
			}
			TX = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
		} else { 
			log.debug(getLogPrefix()+"setting no transction manager");
		}
		if (sessionTransacted) { 
			jmsContainer.setSessionTransacted(sessionTransacted);
		} 
		if (StringUtils.isNotEmpty(messageSelector)) {
			jmsContainer.setMessageSelector(messageSelector);
		}
		
		// Initialize with a number of dynamic properties which come from the configuration file
		jmsContainer.setConnectionFactory(getConnectionFactory());
		jmsContainer.setDestination(getDestination());
        
		jmsContainer.setExceptionListener(this);
		// the following is not required, the timeout set is the time waited to start a new poll attempt.
		//this.jmsContainer.setReceiveTimeout(getJmsListener().getTimeOut());
        
		if (getReceiver().getNumThreads() > 0) {
			jmsContainer.setMaxConcurrentConsumers(getReceiver().getNumThreads());
		} else {
			jmsContainer.setMaxConcurrentConsumers(1);
		}
		jmsContainer.setIdleTaskExecutionLimit(IDLE_TASK_EXECUTION_LIMIT);

		if (StringUtils.isNotEmpty(cacheMode)) {
			jmsContainer.setCacheLevelName(cacheMode);
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

	public void start() throws ListenerException {
		log.debug(getLogPrefix()+"starting");
		if (jmsContainer!=null) {
			try {
				jmsContainer.start();
			} catch (Exception e) {
				throw new ListenerException(getLogPrefix()+"cannot start", e);
			}
		} else {
			throw new ListenerException(getLogPrefix()+"no jmsContainer defined");
		}
	}

	public void stop() throws ListenerException {
		log.debug(getLogPrefix()+"stopping");
		if (jmsContainer!=null) {
			try {
				jmsContainer.stop();
			} catch (Exception e) {
				throw new ListenerException(getLogPrefix()+"Exception while trying to stop", e);
			}
		} else {
			throw new ListenerException(getLogPrefix()+"no jmsContainer defined");
		}
	}


	public void onMessage(Message message, Session session)	throws JMSException {
		TransactionStatus txStatus=null;
               
		long onMessageStart= System.currentTimeMillis();
		long jmsTimestamp= message.getJMSTimestamp();
		threadsProcessing.increase();
		Thread.currentThread().setName(getReceiver().getName()+"["+threadsProcessing.getValue()+"]");

		try {		
			if (TX!=null) {
				txStatus = txManager.getTransaction(TX);
			}
                
			Map threadContext = new HashMap();
			try {
				IPortConnectedListener listener = getListener();
				threadContext.put(THREAD_CONTEXT_SESSION_KEY,session);
//				if (log.isDebugEnabled()) log.debug("transaction status before: "+JtaUtil.displayTransactionStatus());
				getReceiver().processRawMessage(listener, message, threadContext);
//				if (log.isDebugEnabled()) log.debug("transaction status after: "+JtaUtil.displayTransactionStatus());
			} catch (ListenerException e) {
				getReceiver().increaseRetryIntervalAndWait(e,getLogPrefix());
				if (txStatus!=null) {
					txStatus.setRollbackOnly();
				} else {
					JMSException jmse = new JMSException(getLogPrefix()+"caught exception: "+e.getMessage());
					jmse.setLinkedException(e);
					throw jmse;
				}
//				if (JtaUtil.inTransaction()) {
//					log.warn(getLogPrefix()+"caught exception processing message, setting rollbackonly", e);
//					JtaUtil.setRollbackOnly();
//				} else {
//					if (jmsContainer.isSessionTransacted()) {
//						log.warn(getLogPrefix()+"caught exception processing message, rolling back JMS session", e);
//						session.rollback();
//					} else {
//						JMSException jmse = new JMSException(getLogPrefix()+"caught exception, no transactional stuff to rollback");
//						jmse.initCause(e);
//						throw jmse;
//					}
//				}
			} finally {
				if (txStatus==null && jmsContainer.isSessionTransacted()) {
					log.debug(getLogPrefix()+"committing JMS session");
					session.commit();
				}
			}
		} finally {
			if (txStatus!=null) {
				txManager.commit(txStatus);
			}
			threadsProcessing.decrease();
			if (log.isInfoEnabled()) {
				long onMessageEnd= System.currentTimeMillis();

				log.info(getLogPrefix()+"A) JMSMessageTime ["+DateUtils.format(jmsTimestamp)+"]");
				log.info(getLogPrefix()+"B) onMessageStart ["+DateUtils.format(onMessageStart)+"] diff (~'queing' time) ["+(onMessageStart-jmsTimestamp)+"]");
				log.info(getLogPrefix()+"C) onMessageEnd   ["+DateUtils.format(onMessageEnd)+"] diff (process time) ["+(onMessageEnd-onMessageStart)+"]");
			}
			
//			boolean simulateCrashAfterCommit=true;
//			if (simulateCrashAfterCommit) {
//				toggle=!toggle;
//				if (toggle) {
//					JtaUtil.setRollbackOnly();
//					throw new JMSException("simulate crash just before final commit");
//				}
//			}
		}
	}

//	private boolean toggle=true;

	public void onException(JMSException e) {
		IbisExceptionListener ibisExceptionListener = getExceptionListener();
		if (ibisExceptionListener!= null) {
			ibisExceptionListener.exceptionThrown(getListener(), e);
		} else {
			log.error(getLogPrefix()+"Cannot report the error to an IBIS Exception Listener", e);
		}
	}


	public boolean isThreadCountReadable() {
		return jmsContainer!=null;
	}
	public boolean isThreadCountControllable() {
		return jmsContainer!=null;
	}

	public int getCurrentThreadCount() {
		if (jmsContainer!=null) {
			return jmsContainer.getActiveConsumerCount();
		}
		return 0;
	}

	public int getMaxThreadCount() {
		if (jmsContainer!=null) {
			return jmsContainer.getMaxConcurrentConsumers();
		}
		return 0;
	}

	public void increaseThreadCount() {
		if (jmsContainer!=null) {
			jmsContainer.setMaxConcurrentConsumers(jmsContainer.getMaxConcurrentConsumers()+1);	
		}
	}

	public void decreaseThreadCount() {
		if (jmsContainer!=null) {
			int current=getMaxThreadCount();
			if (current>1) {
				jmsContainer.setMaxConcurrentConsumers(current-1);	
			}
		}
	}


	public String getLogPrefix() {
		String result="SpringJmsContainer ";
		if (getListener()!=null && getListener().getReceiver()!=null) {
			result += "of Receiver ["+getListener().getReceiver().getName()+"] ";
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}


	public void setTxManager(PlatformTransactionManager txManager) {
		this.txManager = txManager;
	}
	public PlatformTransactionManager getTxManager() {
		return txManager;
	}


	public void setMessageListenerClassName(String messageListenerClassName) {
		this.messageListenerClassName = messageListenerClassName;
	}
	public String getMessageListenerClassName() {
		return messageListenerClassName;
	}

}
