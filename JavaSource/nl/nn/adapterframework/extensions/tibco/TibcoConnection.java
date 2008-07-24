/*
 * $Log: TibcoConnection.java,v $
 * Revision 1.3  2008-07-24 12:30:05  europe\L190409
 * added support for authenticated JMS
 *
 * Revision 1.2  2008/05/15 14:53:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * remove unnecessary overridden code
 *
 * Revision 1.1  2008/05/15 14:32:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.extensions.tibco;

import java.util.Map;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicSession;
import javax.naming.Context;

import nl.nn.adapterframework.jms.JmsConnection;
import nl.nn.adapterframework.jms.JmsException;

import org.apache.commons.lang.StringUtils;

import com.tibco.tibjms.TibjmsConnectionFactory;

/**
 * Wrapper around Tibco connection objects.
 * 
 * @author 	Gerrit van Brakel
 * @since   4.4
 * @version Id
 */
public class TibcoConnection extends JmsConnection {
	public static final String version="$RCSfile: TibcoConnection.java,v $ $Revision: 1.3 $ $Date: 2008-07-24 12:30:05 $";
	
	private TibjmsConnectionFactory connectionFactory;
	
	public TibcoConnection(String connectionFactoryName, Context context, ConnectionFactory connectionFactory, Map connectionMap, String authAlias) {
		super(connectionFactoryName, context, connectionFactory, connectionMap, authAlias);
		this.connectionFactory=(TibjmsConnectionFactory)connectionFactory;
	}

	protected Connection createConnection() throws JMSException {
		if (StringUtils.isNotEmpty(getAuthAlias())) {
			return super.createConnection();
		}
		String userName=null;
		String password=null;
		return connectionFactory.createConnection(userName,password);
	}


	public Destination lookupDestination(String destinationName) throws JmsException {
		Session session=null;		
		try {
			session = createSession(false,Session.AUTO_ACKNOWLEDGE);
			log.debug("Session class ["+session.getClass().getName()+"]");
			Destination destination;

			/* create the destination */
			if (session instanceof TopicSession) {
				destination = ((TopicSession)session).createTopic(destinationName);
			} else {
				destination = ((QueueSession)session).createQueue(destinationName);
			}

			return destination;
		} catch (Exception e) {
			throw new JmsException("cannot create destination", e);
		} finally {
			releaseSession(session);
		}
	}
	
}
