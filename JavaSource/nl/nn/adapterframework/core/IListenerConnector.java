/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
/*
 * $Log: IListenerConnector.java,v $
 * Revision 1.7  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.5  2010/12/13 13:16:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made acknowledgemode configurable
 *
 * Revision 1.4  2008/09/01 15:08:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added session key to store session
 *
 * Revision 1.3  2008/01/03 15:41:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
 * @version $Id$
 */
public interface IListenerConnector {

	public final static String THREAD_CONTEXT_SESSION_KEY="JmsSession";

   
    void configureEndpointConnection(IPortConnectedListener listener, ConnectionFactory connectionFactory, Destination destination, IbisExceptionListener exceptionListener, String cacheMode, int acknowledgeMode, boolean sessionTransacted, String selector) throws ConfigurationException;

	/**
	 * Start Listener-port to which the Listener is connected.
	 */
    void start() throws ListenerException;
 
	/**
	 * Stop Listener-port to which the Listener is connected.
	 */
    void stop() throws ListenerException;
}
