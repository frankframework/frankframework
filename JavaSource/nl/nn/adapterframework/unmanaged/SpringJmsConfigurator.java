/*
 * Created on 17-sep-07
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
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IJmsConfigurator;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jms.JmsException;
import nl.nn.adapterframework.jms.JmsListener;
import nl.nn.adapterframework.receivers.GenericReceiver;
import org.apache.log4j.Logger;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer102;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Configure a Spring JMS Container from a {@link nl.nn.adapterframework.jms.JmsListener}.
 * 
 * This implementation expects to receive an instance of
 * org.springframework.jms.listener.DefaultMessageListenerContainer
 * from the Spring BeanFactory. If another type of MessageListenerContainer
 * is created by the BeanFactory, then another implementation of IJmsConfigurator
 * should be provided as well.
 * 
 * @author m00035f
 *
 */
public class SpringJmsConfigurator 
    implements IJmsConfigurator, BeanFactoryAware, ExceptionListener {
    private static final Logger log = Logger.getLogger(SpringJmsConfigurator.class);
    
    public static final String version="$RCSfile: SpringJmsConfigurator.java,v $ $Revision: 1.1.2.3 $ $Date: 2007-09-26 06:05:19 $";
    
    private String destinationName;
    private String connectionFactoryName;
    private Destination destination;
    private JmsListener jmsListener;
    private BeanFactory beanFactory;
    private Context context;
    private DefaultMessageListenerContainer jmsContainer;
    
    protected Context createContext() throws NamingException {
        return (Context) new InitialContext();
    }

    protected ConnectionFactory createConnectionFactory(String cfName) throws ConfigurationException {
        ConnectionFactory connectionFactory;
        try {
            connectionFactory = (ConnectionFactory) getContext().lookup(cfName);
        } catch (NamingException e) {
            throw new ConfigurationException("Problem looking up JMS " +
                    (jmsListener.isUseTopicFunctions()?"Topic":"Queue") +
                    "Connection Factory with name '" +
                    cfName + "'", e);
        }
        return connectionFactory;
    }
    
    public Destination getDestination() {
        return destination;
    }

    public Destination getDestination(String destinationName) throws JmsException, NamingException {
        try {
            return createDestination(destinationName);
        } catch (ConfigurationException ex) {
            Throwable t = ex.getCause();
            if (t instanceof NamingException) {
                throw (NamingException)t;
            } else if (t instanceof JmsException) {
                throw (JmsException)t;
            } else {
                throw new JmsException(ex.getMessage(), t);
            }
        }
    }
    
    protected Destination createDestination(String destinationName) throws ConfigurationException {
        try {
            return (Destination) getContext().lookup(destinationName);
        } catch (NamingException e) {
            throw new ConfigurationException("Problem looking up JMS " +
                    (jmsListener.isUseTopicFunctions()?"Topic":"Queue") +
                    "Destination with name '" +
                    destinationName + "'", e);
        }
    }
    
    /* (non-Javadoc)
     * @see nl.nn.adapterframework.configuration.IJmsConfigurator#configureReceiver(nl.nn.adapterframework.jms.JmsListener)
     */
    public void configureJmsReceiver(final JmsListener jmsListener) throws ConfigurationException {
        this.jmsListener = jmsListener;
        
        // Create the Message Listener Container manually.
        // This is needed, because otherwise the Spring Factory will
        // call afterPropertiesSet() on the object which will validate
        // that all required properties are set before we get a chance
        // to insert our dynamic values from the config. file.
        jmsContainer = new DefaultMessageListenerContainer102();
        
        if (jmsListener.isTransacted()) {
            jmsContainer.setTransactionManager((PlatformTransactionManager) beanFactory.getBean("txManager"));
        }
        
        // Initialize with a number of dynamic properties which come from the configuration file
        try {
            connectionFactoryName = jmsListener.getConnectionFactoryName();
            destinationName = jmsListener.getDestinationName();
            ConnectionFactory connectionFactory = createConnectionFactory(connectionFactoryName);
            
            destination = createDestination(destinationName);
            jmsContainer.setConnectionFactory(connectionFactory);
            jmsContainer.setDestination(destination);
        } catch (JmsException e) {
            throw new ConfigurationException("Cannot look up destination", e);
        }
        
        final GenericReceiver receiver = (GenericReceiver) jmsListener.getHandler();
        jmsContainer.setConcurrentConsumers(receiver.getNumThreads());
        jmsContainer.setExceptionListener(this);
        jmsContainer.setMessageListener(new SessionAwareMessageListener() {
            public void onMessage(Message message, Session session)
                throws JMSException {
                Map threadContext = new HashMap();
                jmsListener.populateThreadContext(threadContext, session);
                try {
                    receiver.processRawMessage(jmsListener, message, threadContext,
                        jmsListener.getTimeOut());
                } catch (ListenerException e) {
                    // TODO Proper way to handle this error
                    e.printStackTrace();
                    session.rollback();
                }
            }
        });
        // Use Spring BeanFactory to complete the auto-wiring of the JMS Listener Container,
        // and run the bean lifecycle methods.
        try {
            ((AutowireCapableBeanFactory) beanFactory).configureBean(jmsContainer, "proto-jmsContainer");
        } catch (BeansException e) {
            throw new ConfigurationException("Out of luck wiring up and configuring Default JMS Message Listener Container for JMS Listener "
                    + (jmsListener.getName() != null?jmsListener.getName():jmsListener.getLogPrefix()), e);
        }
        
        // Finally, set bean name to something we can make sense of
        if (jmsListener.getName() != null) {
            jmsContainer.setBeanName(jmsListener.getName());
        } else {
            jmsContainer.setBeanName(jmsListener.getLogPrefix());
        }
        
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
     */
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
    
    
    /**
     * @return
     */
    public Context getContext() throws NamingException {
        if (context == null) {
            context = createContext();
        }
        return context;
    }

    /**
     * @param context
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.configuration.IJmsConfigurator#openJmsReceiver()
     */
    public void openJmsReceiver() throws ListenerException {
        try {
            jmsContainer.start();
        } catch (Exception e) {
            throw new ListenerException("Cannot start Spring JMS Container", e);
        }
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.configuration.IJmsConfigurator#closeJmsReceiver()
     */
    public void closeJmsReceiver() throws ListenerException {
        try {
            jmsContainer.stop();
        } catch (Exception e) {
            throw new ListenerException("Cannot stop Spring JMS Container", e);
        }
    }

    public void onException(JMSException e) {
        log.error("JMS Exception occurred in Message Listener Container configured for " +
                (jmsListener.isUseTopicFunctions()?"Topic":"Queue")
                + "Connection Factory [" + connectionFactoryName
                + "], destination [" + destinationName
                + "]:", e);
        IbisExceptionListener ibisExceptionListener = jmsListener.getExceptionListener();
        if (ibisExceptionListener == null) {
            ibisExceptionListener = (IbisExceptionListener) jmsListener.getHandler();
        }
        if (ibisExceptionListener!= null) {
            log.error("Reporting the error to the IBIS Exception Listener");
            jmsListener.getExceptionListener().exceptionThrown(jmsListener, e);
        } else {
            log.error("Cannot report the error to an IBIS Exception Listener");
        }
    }

}
