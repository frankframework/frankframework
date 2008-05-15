/*
 * $Log: TibcoListener.java,v $
 * Revision 1.1  2008-05-15 14:32:58  europe\L190409
 * first version
 *
 */
package nl.nn.adapterframework.extensions.tibco;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.jms.JmsConnection;
import nl.nn.adapterframework.jms.JmsException;
import nl.nn.adapterframework.jms.JmsListener;

import org.apache.commons.lang.StringUtils;

/**
 * Dedicated Listener on Tibco JMS Destinations.
 * 
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
 */
public class TibcoListener extends JmsListener {

	private String serverUrl;

	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getServerUrl())) {
			throw new ConfigurationException("serverUrl must be specified");
		}
		super.configure();
	}


	protected JmsConnection getConnection() throws JmsException {
		if (connection == null) {
			synchronized (this) {
				if (connection == null) {
					log.debug("instantiating JmsConnectionFactory");
					TibcoConnectionFactory tibcoConnectionFactory = new TibcoConnectionFactory(isUseTopicFunctions());
					try {
						String serverUrl = getServerUrl();
						log.debug("creating JmsConnection");
						connection = (TibcoConnection)tibcoConnectionFactory.getConnection(serverUrl);
					} catch (IbisException e) {
						if (e instanceof JmsException) {
							throw (JmsException)e;
						}
						throw new JmsException(e);
					}
				}
			}
		}
		return connection;
	}


	public void setServerUrl(String string) {
		serverUrl = string;
	}
	public String getServerUrl() {
		return serverUrl;
	}

}
