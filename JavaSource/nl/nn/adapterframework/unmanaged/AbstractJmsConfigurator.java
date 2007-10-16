/*
 * $Log: AbstractJmsConfigurator.java,v $
 * Revision 1.3  2007-10-16 09:52:35  europe\M00035F
 * Change over JmsListener to a 'switch-class' to facilitate smoother switchover from older version to spring version
 *
 * Revision 1.2  2007/10/10 07:49:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.unmanaged;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.jms.PushingJmsListener;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Base class for JMS Configurator implementations.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
abstract public class AbstractJmsConfigurator {
	protected Logger log=LogUtil.getLogger(this);
   
    private PushingJmsListener jmsListener;
    private Context context;
    private Destination destination;
    private String destinationName;

    public void configureJmsReceiver(final PushingJmsListener jmsListener) throws ConfigurationException {
        this.jmsListener = jmsListener;
        setDestinationName(jmsListener.getDestinationName());
        setDestination(createDestination(jmsListener.getDestinationName()));
    }

    protected ConnectionFactory createConnectionFactory(String cfName) throws ConfigurationException {
        ConnectionFactory connectionFactory;
        try {
            connectionFactory = (ConnectionFactory) getContext().lookup(cfName);
        } catch (NamingException e) {
            throw new ConfigurationException("Problem looking up JMS " + (jmsListener.isUseTopicFunctions() ? "Topic" : "Queue") + "Connection Factory with name [" + cfName + "]", e);
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
            throw new ConfigurationException("Problem looking up JMS " + (jmsListener.isUseTopicFunctions() ? "Topic" : "Queue") + "Destination with name [" + destinationName + "]", e);
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

    public void setContext(Context context) {
        this.context = context;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public PushingJmsListener getJmsListener() {
        return jmsListener;
    }

    public void setJmsListener(PushingJmsListener jmsListener) {
        this.jmsListener = jmsListener;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

}
