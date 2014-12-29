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

import java.util.Enumeration;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;

import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

import com.tibco.tibjms.admin.QueueInfo;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;

/**
 * Some utilities for working with TIBCO.
 * 
 * @author Peter Leeuwenburgh
 */
public class TibcoUtils {
	static Logger log = LogUtil.getLogger(TibcoUtils.class);

	public static long getQueueFirstMessageAge(String url, String authAlias,
			String userName, String password, String queueName)
			throws JMSException, TibjmsAdminException {
		long result = -1;
		CredentialFactory cf = new CredentialFactory(authAlias, userName,
				password);
		Connection connection = null;
		Session jSession = null;
		TibjmsAdmin admin = null;
		try {
			admin = new TibjmsAdmin(url, cf.getUsername(), cf.getPassword());
			QueueInfo qInfo = admin.getQueue(queueName);
			if (qInfo.getPendingMessageCount() > 0) {
				ConnectionFactory factory = new com.tibco.tibjms.TibjmsConnectionFactory(
						url);
				connection = factory.createConnection(cf.getUsername(),
						cf.getPassword());
				jSession = connection.createSession(false,
						javax.jms.Session.AUTO_ACKNOWLEDGE);
				result = getQueueFirstMessageAge(jSession, queueName);
			}
		} finally {
			if (admin != null) {
				try {
					admin.close();
				} catch (TibjmsAdminException e) {
					log.warn("Exception on closing Tibjms Admin", e);
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (JMSException e) {
					log.warn("Exception on closing connection", e);
				}
			}
		}
		return result;
	}

	protected static long getQueueFirstMessageAge(Session jSession,
			String queueName) throws JMSException {
		return getQueueFirstMessageAge(jSession, queueName, System.currentTimeMillis());
	}

	protected static long getQueueFirstMessageAge(Session jSession,
			String queueName, long currentTime) throws JMSException {
		Queue queue = jSession.createQueue(queueName);
		QueueBrowser queueBrowser = jSession.createBrowser(queue);
		Enumeration enm = queueBrowser.getEnumeration();
		if (enm.hasMoreElements()) {
			Object o = enm.nextElement();
			if (o instanceof Message) {
				Message msg = (Message) o;
				long jmsTimestamp = msg.getJMSTimestamp();
				return currentTime - jmsTimestamp;
			} else {
				log.warn("message was not of type Message, but ["
						+ o.getClass().getName() + "]");
				return -1;
			}
		} else {
			return -1;
		}
	}
}
