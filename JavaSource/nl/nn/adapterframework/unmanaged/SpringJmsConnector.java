/*
 * $Log: SpringJmsConnector.java,v $
 * Revision 1.4  2008-01-03 15:57:58  europe\L190409
 * rework port connected listener interfaces
 *
 * Revision 1.3  2007/11/22 09:12:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added message as parameter of populateThreadContext
 *
 * Revision 1.2  2007/11/05 13:06:55  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Rename and redefine methods in interface IListenerConnector to remove 'jms' from names
 *
 * Revision 1.1  2007/11/05 12:24:01  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Rename 'SpringJmsConfigurator' to 'SpringJmsConnector'
 *
 * Revision 1.5  2007/11/05 10:33:15  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Move interface 'IListenerConnector' from package 'configuration' to package 'core' in preparation of renaming it
 *
 * Revision 1.4  2007/10/17 11:33:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * add at least one consumer
 *
 * Revision 1.3  2007/10/16 09:52:35  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Change over JmsListener to a 'switch-class' to facilitate smoother switchover from older version to spring version
 *
 * Revision 1.2  2007/10/15 13:11:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * copy from EJB branch
 *
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
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.util.Counter;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
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
 * @version Id
 */
public class SpringJmsConnector extends AbstractJmsConfigurator implements IListenerConnector, BeanFactoryAware, ExceptionListener, SessionAwareMessageListener {
	private static final Logger log = LogUtil.getLogger(SpringJmsConnector.class);
	
	public static final TransactionDefinition TXSUPPORTS = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS);
	public static final TransactionDefinition TXMANDATORY = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_MANDATORY);
    
	public static final String version="$RCSfile: SpringJmsConnector.java,v $ $Revision: 1.4 $ $Date: 2008-01-03 15:57:58 $";
    
	private PlatformTransactionManager txManager;
	private BeanFactory beanFactory;
	private DefaultMessageListenerContainer jmsContainer;
	private String messageListenerClassName;
    
    public static final int CACHE_LEVEL=DefaultMessageListenerContainer.CACHE_CONSUMER;
//	public static final int MAX_MESSAGES_PER_TASK=100;
	public static final int IDLE_TASK_EXECUTION_LIMIT=10;
 
	final Counter threadsProcessing = new Counter(0);
    
	protected DefaultMessageListenerContainer createMessageListenerContainer() throws ConfigurationException {
		try {
			Class klass = Class.forName(messageListenerClassName);
			return (DefaultMessageListenerContainer) klass.newInstance();
		} catch (Exception e) {
			throw new ConfigurationException("Error creating instance of MessageListenerContainer ["+messageListenerClassName+"]", e);
		}
	}
    
	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.configuration.IListenerConnector#configureReceiver(nl.nn.adapterframework.jms.PushingJmsListener)
	 */
	public void configureEndpointConnection(final IPortConnectedListener jmsListener, ConnectionFactory connectionFactory, Destination destination, IbisExceptionListener exceptionListener, String cacheMode, boolean sessionTransacted, String messageSelector) throws ConfigurationException {
		super.configureEndpointConnection(jmsListener, connectionFactory, destination, exceptionListener);
        
		// Create the Message Listener Container manually.
		// This is needed, because otherwise the Spring Factory will
		// call afterPropertiesSet() on the object which will validate
		// that all required properties are set before we get a chance
		// to insert our dynamic values from the config. file.
		this.jmsContainer = createMessageListenerContainer();
		
        
		if (getReceiver().isTransacted()) {
			this.jmsContainer.setTransactionManager(this.txManager);
		}
		if (sessionTransacted) { 
			this.jmsContainer.setSessionTransacted(sessionTransacted);
		} 
		if (StringUtils.isNotEmpty(messageSelector)) {
			jmsContainer.setMessageSelector(messageSelector);
		}
		
		// Initialize with a number of dynamic properties which come from the configuration file
		this.jmsContainer.setConnectionFactory(getConnectionFactory());
		this.jmsContainer.setDestination(getDestination());
        
		this.jmsContainer.setExceptionListener(this);
		// the following is not required, the timeout set is the time waited to start a new poll attempt.
		//this.jmsContainer.setReceiveTimeout(getJmsListener().getTimeOut());
        
		if (getReceiver().getNumThreads() > 0) {
			this.jmsContainer.setConcurrentConsumers(getReceiver().getNumThreads());
		} else {
			this.jmsContainer.setConcurrentConsumers(1);
		}
		this.jmsContainer.setIdleTaskExecutionLimit(IDLE_TASK_EXECUTION_LIMIT);
//		this.jmsContainer.setCacheLevel(CACHE_LEVEL);
		this.jmsContainer.setCacheLevelName(cacheMode);
		this.jmsContainer.setMessageListener(this);
		// Use Spring BeanFactory to complete the auto-wiring of the JMS Listener Container,
		// and run the bean lifecycle methods.
		try {
			((AutowireCapableBeanFactory) this.beanFactory).configureBean(this.jmsContainer, "proto-jmsContainer");
		} catch (BeansException e) {
			throw new ConfigurationException("Out of luck wiring up and configuring Default JMS Message Listener Container for JMS Listener ["+ (getListener().getName()+"]"), e);
		}
        
		// Finally, set bean name to something we can make sense of
		if (getListener().getName() != null) {
			this.jmsContainer.setBeanName(getListener().getName());
		} else {
			this.jmsContainer.setBeanName(getReceiver().getName());
		}
        
	}

	public void onMessage(Message message, Session session)
		throws JMSException {
                
		threadsProcessing.increase();
		Thread.currentThread().setName(getReceiver().getName()+"["+threadsProcessing.getValue()+"]");
                
		TransactionStatus txStatus = null;
		Map threadContext = new HashMap();
		try {
			IPortConnectedListener listener = getListener();
			String messageText=listener.getStringFromRawMessage(message, threadContext);
			String cid=listener.getIdFromRawMessage(message, threadContext);
			String mid = (String)threadContext.get("id");
			threadContext.put("session",session);
			getReceiver().processRequest(getListener(), mid, cid, messageText, threadContext,-1);
		} catch (ListenerException e) {
			invalidateSessionTransaction(e, session, txStatus);
		} finally {
			threadsProcessing.decrease();
		}
	}

	private void invalidateSessionTransaction(Throwable t, Session session,	TransactionStatus txStatus) throws JMSException {
		// TODO Proper way to handle this error
		if (!getReceiver().isTransacted()) {
			log.debug("Exception caught in onMessage; rolling back JMS Session", t);
			session.rollback();
		} else {
			if (txStatus == null) {
				log.error("Unable to rollback current global transaction since it is null! Original exception for which we want to rollback:", t);
				throw new javax.jms.IllegalStateException("Trying to roll back current global transaction for transacted listener ["
						+ getListener().getName()+"] but reference to transaction-status is null; original exception for which we want to rollback is instance of "
						+ t.getClass().getName()+"; exception message: "+t.getMessage());
			}
			log.debug("Exception caught in onMessage; rolling back global transaction", t);
			txStatus.setRollbackOnly();
			throw new JMSException("Forcing rollback because Receiver could not process the message");
		}
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
	 */
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.configuration.IListenerConnector#openJmsReceiver()
	 */
	public void start() throws ListenerException {
		log.debug("Starting Spring JMS Container");
		try {
			jmsContainer.start();
		} catch (Exception e) {
			throw new ListenerException("Cannot start Spring JMS Container", e);
		}
	}

	/* (non-Javadoc)
	 * @see nl.nn.adapterframework.configuration.IListenerConnector#closeJmsReceiver()
	 */
	public void stop() throws ListenerException {
		log.debug("Stopping Spring JMS Container");
		try {
			jmsContainer.stop();
		} catch (Exception e) {
			throw new ListenerException("Exception while trying to stop Spring JMS Container", e);
		}
	}

	public void onException(JMSException e) {
		IbisExceptionListener ibisExceptionListener = getExceptionListener();
		if (ibisExceptionListener!= null) {
			ibisExceptionListener.exceptionThrown(getListener(), e);
		} else {
			log.error("Cannot report the error to an IBIS Exception Listener", e);
		}
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
