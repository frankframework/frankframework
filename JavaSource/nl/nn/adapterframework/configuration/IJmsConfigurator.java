/*
 * Created on 17-sep-07
 *
 */
package nl.nn.adapterframework.configuration;

import javax.jms.Destination;
import javax.naming.NamingException;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jms.JmsException;
import nl.nn.adapterframework.jms.JmsListener;

/**
 * Interface specifying method to configure a JMS receiver or some sort
 * from a provided {@link nl.nn.adapterframework.jms.JmsListener} instance.
 * 
 * 
 * @author m00035f
 *
 */
public interface IJmsConfigurator {

    Destination getDestination(String destinationName) throws JmsException, NamingException;
    Destination getDestination();
    void configureJmsReceiver(JmsListener jmsListener) throws ConfigurationException;
    void openJmsReceiver() throws ListenerException;
    void closeJmsReceiver() throws ListenerException;
}
