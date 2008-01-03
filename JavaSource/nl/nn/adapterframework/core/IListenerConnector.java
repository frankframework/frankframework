/*
 * $Log: IListenerConnector.java,v $
 * Revision 1.3  2008-01-03 15:41:49  europe\L190409
 * rework port connected listener interfaces
 *
 * Revision 1.2  2007/11/05 13:06:55  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Rename and redefine methods in interface IListenerConnector to remove 'jms' from names
 *
 * Revision 1.1  2007/11/05 12:18:49  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
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

import javax.jms.ConnectionFactory;
import javax.jms.Destination;

import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 * Interface specifying method to configure a JMS receiver or some sort
 * from a provided {@link nl.nn.adapterframework.jms.ConnectionBase appConnection} instance.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public interface IListenerConnector {
    
    void configureEndpointConnection(IPortConnectedListener listener, ConnectionFactory connectionFactory, Destination destination, IbisExceptionListener exceptionListener, String cacheMode, boolean sessionTransacted, String selector) throws ConfigurationException;

	/**
	 * Start Listener-port to which the Listener is connected.
	 */
    void start() throws ListenerException;
 
	/**
	 * Stop Listener-port to which the Listener is connected.
	 */
    void stop() throws ListenerException;
}
