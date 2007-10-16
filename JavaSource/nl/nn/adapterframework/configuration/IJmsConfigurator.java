/*
 * $Log: IJmsConfigurator.java,v $
 * Revision 1.3  2007-10-16 09:52:35  europe\M00035F
 * Change over JmsListener to a 'switch-class' to facilitate smoother switchover from older version to spring version
 *
 * Revision 1.2  2007/10/09 15:29:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.configuration;

import javax.jms.Destination;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jms.PushingJmsListener;

/**
 * Interface specifying method to configure a JMS receiver or some sort
 * from a provided {@link nl.nn.adapterframework.jms.PushingJmsListener} instance.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public interface IJmsConfigurator {

    Destination getDestination();
    void configureJmsReceiver(PushingJmsListener jmsListener) throws ConfigurationException;
    void openJmsReceiver() throws ListenerException;
    void closeJmsReceiver() throws ListenerException;
}
