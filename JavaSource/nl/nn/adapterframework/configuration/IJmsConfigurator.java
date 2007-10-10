/*
 * $Log: IJmsConfigurator.java,v $
 * Revision 1.1.2.4  2007-10-10 14:30:41  europe\L190409
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.2  2007/10/09 15:29:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
