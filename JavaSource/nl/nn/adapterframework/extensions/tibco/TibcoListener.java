/*
 * $Log: TibcoListener.java,v $
 * Revision 1.2  2008-07-24 12:30:05  europe\L190409
 * added support for authenticated JMS
 *
 * Revision 1.1  2008/05/15 14:32:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.jms.JmsListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setServerUrl(String) serverUrl}</td><td>URL (hostname and port, separated by ':') of Tibco-Server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationName(String) destinationName}</td><td>name of the JMS destination (queue or topic) to use</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationType(String) destinationType}</td><td>either <code>QUEUE</code> or <code>TOPIC</code></td><td><code>QUEUE</code></td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>when true, the processing joins a transaction set up by the Pipeline or Receiver</td><td>false</td></tr>
 * <tr><td>{@link #setJmsTransacted(boolean) jmsTransacted}</td><td>when true, sessions are explicitly committed (exit-state equals commitOnState) or rolled-back (other exit-states) </td><td>false</td></tr>
 * <tr><td>{@link #setCommitOnState(String) commitOnState}</td><td>&nbsp;</td><td>"success"</td></tr>
 * <tr><td>{@link #setAcknowledgeMode(String) acknowledgeMode}</td><td>"auto", "dups" or "client"</td><td>"auto"</td></tr>
 * <tr><td>{@link #setPersistent(boolean) persistent}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTimeOut(long) timeOut}</td><td>receiver timeout, in milliseconds</td><td>3000 [ms]</td></tr>
 * <tr><td>{@link #setUseReplyTo(boolean) useReplyTo}</td><td>&nbsp;</td><td>true</td></tr>
 * <tr><td>{@link #setReplyMessageTimeToLive(long) replyMessageTimeToLive}</td><td>time that replymessage will live</td><td>0 [ms]</td></tr>
 * <tr><td>{@link #setReplyMessageType(String) replyMessageType}</td><td>value of the JMSType field of the reply message</td><td>not set by application</td></tr>
 * <tr><td>{@link #setReplyDeliveryMode(String) replyDeliveryMode}</td><td>controls mode that reply messages are sent with: either 'persistent' or 'non_persistent'</td><td>not set by application</td></tr>
 * <tr><td>{@link #setReplyPriority(int) replyPriority}</td><td>sets the priority that is used to deliver the reply message. ranges from 0 to 9. Defaults to -1, meaning not set. Effectively the default priority is set by Jms to 4</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setForceMQCompliancy(String) forceMQCompliancy}</td><td>Possible values: 'MQ' or 'JMS'. Setting to 'MQ' informs the MQ-server that the replyto queue is not JMS compliant.</td><td>JMS</td></tr>
 * <tr><td>{@link #setForceMessageIdAsCorrelationId(boolean) forceMessageIdAsCorrelationId}</td><td>
 * forces that the CorrelationId that is received is ignored and replaced by the messageId that is received. Use this to create a new, globally unique correlationId to be used downstream. It also
 * forces that not the Correlation ID of the received message is used in a reply as CorrelationId, but the MessageId.</td><td>false</td></tr>
 * </table>
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
						connection = (TibcoConnection)tibcoConnectionFactory.getConnection(serverUrl, getAuthAlias());
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
