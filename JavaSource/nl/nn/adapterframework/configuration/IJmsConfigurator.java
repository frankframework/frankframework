/*
 * $Log: IJmsConfigurator.java,v $
 * Revision 1.2  2007-10-09 15:29:43  europe\L190409
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.configuration;

import javax.jms.Destination;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jms.JmsListener;

/**
 * Interface specifying method to configure a JMS receiver or some sort
 * from a provided {@link nl.nn.adapterframework.jms.JmsListener} instance.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public interface IJmsConfigurator {

    Destination getDestination();
    void configureJmsReceiver(JmsListener jmsListener) throws ConfigurationException;
    void openJmsReceiver() throws ListenerException;
    void closeJmsReceiver() throws ListenerException;
}
