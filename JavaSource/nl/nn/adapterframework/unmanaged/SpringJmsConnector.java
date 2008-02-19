/*
 * $Log: SpringJmsConnector.java,v $
 * Revision 1.11  2008-02-19 13:58:35  europe\L190409
 * tiny little bug, pushed into 4.8.0
 *
 * Revision 1.10  2008/02/15 14:11:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid NPE when not configured
 *
 * Revision 1.9  2008/02/13 13:32:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * show detailed processing times
 *
 * Revision 1.8  2008/02/06 16:38:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for setting of transaction timeout
 * removed global transaction inserted for jmsTransacted handling
 *
 * Revision 1.7  2008/01/29 12:17:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for thread number control
 *
 * Revision 1.6  2008/01/17 16:24:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * txManager in onMessage only for only local transacted sessions
 *
 * Revision 1.5  2008/01/11 10:23:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed a lot of things
 *
 * Revision 1.4  2008/01/03 15:57:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.util.Counter;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.JtaUtil;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.transaction.PlatformTransactionManager;

import com.ing.ifsa.IFSAMessage;

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
public class SpringJmsConnector extends AbstractJmsConfigurator implements IListenerConnector, IThreadCountControllable, BeanFactoryAware, ExceptionListener, SessionAwareMessageListener {

 	private PlatformTransactionManager txManager;
	private BeanFactory beanFactory;
	private DefaultMessageListenerContainer jmsContainer;
	private String messageListenerClassName;
    
    public static final int CACHE_LEVEL=DefaultMessageListenerContainer.CACHE_CONSUMER;
//	public static final int MAX_MESSAGES_PER_TASK=100;
	public static final int IDLE_TASK_EXECUTION_LIMIT=1000;
 
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
	public void configureEndpointConnection(final IPortConnectedListener jmsListener, ConnectionFactory connectionFactory, Destination destination, IbisExceptionListener exceptionListener, String cacheMode, boolean sessionTransacted, String messageSelector) throws ConfigurationException {
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
				jmsContainer.setCacheLevel(DefaultMessageListenerContainer.CACHE_NONE);
			} else {
				jmsContainer.setCacheLevel(DefaultMessageListenerContainer.CACHE_CONSUMER);
			}
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
		try {
			jmsContainer.start();
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix()+"cannot start", e);
		}
	}

	public void stop() throws ListenerException {
		log.debug(getLogPrefix()+"stopping");
		try {
			jmsContainer.stop();
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix()+"Exception while trying to stop", e);
		}
	}


	public void onMessage(Message message, Session session)	throws JMSException {
                
		long onMessageStart= System.currentTimeMillis();
		long jmsTimestamp= message.getJMSTimestamp();
		threadsProcessing.increase();
		Thread.currentThread().setName(getReceiver().getName()+"["+threadsProcessing.getValue()+"]");
                
		Map threadContext = new HashMap();
		try {
			IPortConnectedListener listener = getListener();
			threadContext.put("session",session);
//			if (log.isDebugEnabled()) log.debug("transaction status before: "+JtaUtil.displayTransactionStatus());
			getReceiver().processRawMessage(listener, message, threadContext);
//			if (log.isDebugEnabled()) log.debug("transaction status after: "+JtaUtil.displayTransactionStatus());
		} catch (ListenerException e) {
			if (JtaUtil.inTransaction()) {
				log.warn(getLogPrefix()+"caught exception, setting rollbackonly");
				JtaUtil.setRollbackOnly();
			} else {
				if (jmsContainer.isSessionTransacted()) {
					log.warn("rolling back JMS session");
					session.rollback();
				} else {
					log.warn(getLogPrefix()+"caught exception, no transactional stuff to rollback, rethrowing ListenerException as JmsException");
					JMSException jmse = new JMSException(e.getMessage());
					throw jmse;
				}
			}
		} finally {
			if (jmsContainer.isSessionTransacted()) {
				log.debug(getLogPrefix()+"committing JMS session");
				session.commit();
			}
			threadsProcessing.decrease();
			if (log.isInfoEnabled()) {
				long onMessageEnd= System.currentTimeMillis();

				log.info(getLogPrefix()+"A) JMSMessageTime ["+DateUtils.format(jmsTimestamp)+"]");
				log.info(getLogPrefix()+"B) onMessageStart ["+DateUtils.format(onMessageStart)+"] diff ["+(onMessageStart-jmsTimestamp)+"]");
				log.info(getLogPrefix()+"C) onMessageEnd   ["+DateUtils.format(onMessageEnd)+"] diff ["+(onMessageEnd-onMessageStart)+"]");

			
				if (message instanceof IFSAMessage) {
					IFSAMessage im = (IFSAMessage)message;
					long busStartTime=im.getBusinessProcessingStartTime();
					long busFinTime=im.getBusinessProcessingFinishTime();
					log.info(getLogPrefix()+"D) Ifsa BusinessProcessingStartTime ["+DateUtils.format(busStartTime)+"]");
					log.info(getLogPrefix()+"E) BusinessProcessingFinishTime     ["+DateUtils.format(busFinTime)+"] diff ["+(busFinTime-busStartTime)+"]");
				}
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
