/*
 * $Log: IListenerConnector.java,v $
 * Revision 1.1  2007-11-05 12:18:49  europe\M00035F
 * Rename interface IJmsConfigurator to IListenerConnector to make it more generic and make the name better match what the implementations do.
 *
 * Revision 1.1  2007/11/05 10:33:16  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Move interface 'IListenerConnector' from package 'configuration' to package 'core' in preparation of renaming it
 *
 * Revision 1.3  2007/10/16 09:52:35  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Change over JmsListener to a 'switch-class' to facilitate smoother switchover from older version to spring version
 *
 * Revision 1.2  2007/10/09 15:29:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.core;

import javax.jms.Destination;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.jms.PushingJmsListener;

/**
 * Interface specifying method to configure a JMS receiver or some sort
 * from a provided {@link nl.nn.adapterframework.jms.PushingJmsListener} instance.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public interface IListenerConnector {

    Destination getDestination();
    void configureJmsReceiver(PushingJmsListener jmsListener) throws ConfigurationException;
    void openJmsReceiver() throws ListenerException;
    void closeJmsReceiver() throws ListenerException;
}
