/*
 * $Log: AbstractJmsConfigurator.java,v $
 * Revision 1.8  2011-11-30 13:52:01  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.6  2008/02/28 16:25:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved configuration checking
 *
 * Revision 1.5  2008/01/03 15:57:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework port connected listener interfaces
 *
 * Revision 1.4  2007/11/05 13:06:55  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Rename and redefine methods in interface IListenerConnector to remove 'jms' from names
 *
 * Revision 1.3  2007/10/16 09:52:35  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPortConnectedListener;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * Base class for JMS Configurator implementations.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
abstract public class AbstractJmsConfigurator {
	protected Logger log=LogUtil.getLogger(this);
   
    private IPortConnectedListener listener;
    private ConnectionFactory connectionFactory;
    private Destination destination;
	private ReceiverBase receiver;
	private IbisExceptionListener exceptionListener;        

    public void configureEndpointConnection(IPortConnectedListener listener, ConnectionFactory connectionFactory, Destination destination, IbisExceptionListener exceptionListener) throws ConfigurationException {
    	if (connectionFactory==null) {
    		throw new ConfigurationException("ConnectionFactory must be specified");
    	}
		if (destination==null) {
			throw new ConfigurationException("Destination must be specified");
		}
		setListener(listener);
		setConnectionFactory(connectionFactory);
        setDestination(destination);
		this.receiver = (ReceiverBase)getListener().getReceiver();
		this.exceptionListener = exceptionListener;
    }


    public void setListener(IPortConnectedListener listener) {
        this.listener = listener;
    }
	public IPortConnectedListener getListener() {
		return listener;
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}
	public ConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}

    public void setDestination(Destination destination) {
        this.destination = destination;
    }
	public Destination getDestination() {
		return destination;
	}

	public ReceiverBase getReceiver() {
		return receiver;
	}

	public IbisExceptionListener getExceptionListener() {
		return exceptionListener;
	}
}
