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

import nl.nn.adapterframework.jms.JmsException;
import nl.nn.adapterframework.jms.JmsMessagingSource;
import nl.nn.adapterframework.jms.MessagingSource;

import org.apache.commons.lang3.StringUtils;

import com.tibco.tibjms.TibjmsConnectionFactory;

/**
 * {@link MessagingSource} for Tibco connections.
 * 
 * @author 	Gerrit van Brakel
 * @since   4.9
 */
public class TibcoMessagingSource extends JmsMessagingSource {

	private final TibjmsConnectionFactory connectionFactory;
	
	public TibcoMessagingSource(String connectionFactoryName, Context context,
			ConnectionFactory connectionFactory, Map<String, MessagingSource> messagingSourceMap,
			String authAlias) {
		super(connectionFactoryName, "", context, connectionFactory,
				messagingSourceMap, authAlias, false, null, true);
		this.connectionFactory=(TibjmsConnectionFactory)connectionFactory;
	}

	@Override
	protected Connection createConnection() throws JMSException {
		if (StringUtils.isNotEmpty(getAuthAlias())) {
			return super.createConnection();
		}
		String userName=null;
		String password=null;
		return connectionFactory.createConnection(userName,password);
	}


	@Override
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
