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
package org.frankframework.extensions.tibco;

import java.util.Map;

import javax.naming.Context;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Session;
import jakarta.jms.TopicSession;

import org.apache.commons.lang3.StringUtils;

import com.tibco.tibjms.TibjmsConnectionFactory;

import org.frankframework.jms.JmsException;
import org.frankframework.jms.JmsMessagingSource;
import org.frankframework.jms.MessagingSource;

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
				messagingSourceMap, authAlias, false, null);
		this.connectionFactory=(TibjmsConnectionFactory)connectionFactory;
	}

	@Override
	protected Connection createConnection() throws JMSException {
		if (StringUtils.isNotEmpty(getAuthAlias())) {
			return super.createConnection();
		}
		return connectionFactory.createConnection(null, null);
	}


	@Override
	public Destination lookupDestination(String destinationName) throws JmsException {
		Session session=null;
		try {
			session = createSession(false,Session.AUTO_ACKNOWLEDGE);
			log.debug("Session class [{}]", session.getClass().getName());
			Destination destination;

			/* create the destination */
			if (session instanceof TopicSession) {
				destination = session.createTopic(destinationName);
			} else {
				destination = session.createQueue(destinationName);
			}

			return destination;
		} catch (Exception e) {
			throw new JmsException("cannot create destination", e);
		} finally {
			releaseSession(session);
		}
	}

}
