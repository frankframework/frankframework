/*
 * AbstractJmsConfigurator.java
 * 
 * Created on 2-okt-2007, 9:45:07
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.nn.adapterframework.unmanaged;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.jms.JmsException;
import nl.nn.adapterframework.jms.JmsListener;

/**
 * Base class for JMS Configurator implementations
 * @author m00035f
 */
abstract public class AbstractJmsConfigurator {
    
    private JmsListener jmsListener;
    private Context context;
    private Destination destination;
    private String destinationName;

    public void configureJmsReceiver(final JmsListener jmsListener) throws ConfigurationException {
        this.jmsListener = jmsListener;
        setDestinationName(jmsListener.getDestinationName());
            setDestination(createDestination(jmsListener.getDestinationName()));
        
    }

    protected ConnectionFactory createConnectionFactory(String cfName) throws ConfigurationException {
        ConnectionFactory connectionFactory;
        try {
            connectionFactory = (ConnectionFactory) getContext().lookup(cfName);
        } catch (NamingException e) {
            throw new ConfigurationException("Problem looking up JMS " + (jmsListener.isUseTopicFunctions() ? "Topic" : "Queue") + "Connection Factory with name \'" + cfName + "\'", e);
        }
        return connectionFactory;
    }

    protected Context createContext() throws NamingException {
        return (Context) new InitialContext();
    }

    protected Destination createDestination(String destinationName) throws ConfigurationException {
        try {
            return (Destination) getContext().lookup(destinationName);
        } catch (NamingException e) {
            throw new ConfigurationException("Problem looking up JMS " + (jmsListener.isUseTopicFunctions() ? "Topic" : "Queue") + "Destination with name \'" + destinationName + "\'", e);
        }
    }

    public Context getContext() throws NamingException {
        if (context == null) {
            context = createContext();
        }
        return context;
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
                throw (NamingException) t;
            } else if (t instanceof JmsException) {
                throw (JmsException) t;
            } else {
                throw new JmsException(ex.getMessage(), t);
            }
        }
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public JmsListener getJmsListener() {
        return jmsListener;
    }

    public void setJmsListener(JmsListener jmsListener) {
        this.jmsListener = jmsListener;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

}
