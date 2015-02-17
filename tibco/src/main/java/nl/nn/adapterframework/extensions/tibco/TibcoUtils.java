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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.log4j.Logger;

import com.tibco.tibjms.admin.TibjmsAdminException;

/**
 * Some utilities for working with TIBCO.
 * 
 * @author Peter Leeuwenburgh
 */
public class TibcoUtils {
	static Logger log = LogUtil.getLogger(TibcoUtils.class);

	public static long getQueueFirstMessageAge(String provUrl,
			String authAlias, String userName, String password, String queueName)
			throws JMSException, TibjmsAdminException {
		return getQueueFirstMessageAge(provUrl, authAlias, userName, password,
				queueName, null);
	}

	/**
	 * return -1: no message found
	 * return -2: message found, but not of type Message.
	 */
	public static long getQueueFirstMessageAge(String provUrl,
			String authAlias, String userName, String password,
			String queueName, String messageSelector) throws JMSException,
			TibjmsAdminException {
		String url = StringUtils.replace(provUrl, "tibjmsnaming:", "tcp:");
		CredentialFactory cf = new CredentialFactory(authAlias, userName,
				password);
		Connection connection = null;
		Session jSession = null;
		try {
			ConnectionFactory factory = new com.tibco.tibjms.TibjmsConnectionFactory(
					url);
			connection = factory.createConnection(cf.getUsername(),
					cf.getPassword());
			jSession = connection.createSession(false,
					javax.jms.Session.AUTO_ACKNOWLEDGE);
			return getQueueFirstMessageAge(jSession, queueName, messageSelector);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (JMSException e) {
					log.warn("Exception on closing connection", e);
				}
			}
		}
	}

	protected static long getQueueFirstMessageAge(Session jSession,
			String queueName) throws JMSException {
		return getQueueFirstMessageAge(jSession, queueName, null);
	}

	protected static long getQueueFirstMessageAge(Session jSession,
			String queueName, String messageSelector) throws JMSException {
		return getQueueFirstMessageAge(jSession, queueName, messageSelector,
				System.currentTimeMillis());
	}

	protected static long getQueueFirstMessageAge(Session jSession,
			String queueName, long currentTime) throws JMSException {
		return getQueueFirstMessageAge(jSession, queueName, null, currentTime);
	}

	protected static long getQueueFirstMessageAge(Session jSession,
			String queueName, String messageSelector, long currentTime)
			throws JMSException {
		return getQueueFirstMessageAge(jSession, queueName, messageSelector, currentTime, true);
	}

	protected static long getQueueFirstMessageAge(Session jSession,
			String queueName, String messageSelector, long currentTime, boolean warn)
			throws JMSException {
		Queue queue = jSession.createQueue(queueName);
		QueueBrowser queueBrowser;
		if (messageSelector == null) {
			queueBrowser = jSession.createBrowser(queue);
		} else {
			queueBrowser = jSession.createBrowser(queue, messageSelector);
		}
		Enumeration enm = queueBrowser.getEnumeration();
		if (enm.hasMoreElements()) {
			Object o = enm.nextElement();
			if (o instanceof Message) {
				Message msg = (Message) o;
				long jmsTimestamp = msg.getJMSTimestamp();
				return currentTime - jmsTimestamp;
			} else {
				if (warn) {
					log.warn("message was not of type Message, but ["
							+ o.getClass().getName() + "]");
				}
				return -2;
			}
		} else {
			return -1;
		}
	}

	protected static String getQueueFirstMessageAgeAsString(Session jSession,
			String queueName, long currentTime) {
		try {
			long age = getQueueFirstMessageAge(jSession, queueName, null,
					currentTime, false);
			if (age == -2) {
				return "??";
			} else if (age == -1) {
				return null;
			} else {
				return DurationFormatUtils.formatDuration(age, "ddd-HH:mm:ss");
			}
		} catch (JMSException e) {
			return "?";
		}
	}
}
