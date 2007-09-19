/*
 * Created on 17-sep-07
 *
 */
package nl.nn.adapterframework.unmanaged;

import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IJmsConfigurator;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jms.JmsException;
import nl.nn.adapterframework.jms.JmsListener;
import nl.nn.adapterframework.receivers.GenericReceiver;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.task.TaskExecutor;
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
    implements IJmsConfigurator, BeanFactoryAware {
    
    public static final String version="$RCSfile: SpringJmsConfigurator.java,v $ $Revision: 1.1.2.2 $ $Date: 2007-09-19 14:19:42 $";
    
    
    private JmsListener jmsListener;
    private BeanFactory beanFactory;
    private Context context;
    private DefaultMessageListenerContainer jmsContainer;
    
    protected Context createContext() throws NamingException {
        return (Context) new InitialContext();
    }

    protected ConnectionFactory createConnectionFactory(String cfName) throws JmsException {
        ConnectionFactory connectionFactory;
        try {
            connectionFactory = (ConnectionFactory) getContext().lookup(cfName);
        } catch (NamingException e) {
            throw new JmsException("Problem looking up Queue Connection Factory with name '" +
                cfName + "'", e);
        }
        return connectionFactory;
    }
    
    /* (non-Javadoc)
     * @see nl.nn.adapterframework.configuration.IJmsConfigurator#configureReceiver(nl.nn.adapterframework.jms.JmsListener)
     */
    public void configureJmsReceiver(final JmsListener jmsListener) throws ConfigurationException {
        this.jmsListener = jmsListener;
        //jmsContainer = (DefaultMessageListenerContainer) beanFactory.getBean("proto-jmsContainer");
        jmsContainer = new DefaultMessageListenerContainer102();
        //jmsContainer.setTaskExecutor((TaskExecutor) beanFactory.getBean("taskExecutor"));
        if (jmsListener.isTransacted()) {
            jmsContainer.setTransactionManager((PlatformTransactionManager) beanFactory.getBean("txManager"));
        }
        
        try {
            String connectionFactoryName = jmsListener.getConnectionFactoryName();
            jmsContainer.setConnectionFactory(createConnectionFactory(connectionFactoryName));
            jmsContainer.setDestinationName(jmsListener.getDestinationName());
        } catch (Exception e) {
            throw new ConfigurationException("Cannot look up destination", e);
        }
        
        final GenericReceiver receiver = (GenericReceiver) jmsListener.getHandler();
        jmsContainer.setConcurrentConsumers(receiver.getNumThreads());
        
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
        ((AutowireCapableBeanFactory)beanFactory).configureBean(jmsContainer, "proto-jmsContainer");
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

}
