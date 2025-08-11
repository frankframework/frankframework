/*
   Copyright 2013, 2016, 2017 Nationale-Nederlanden

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

import java.util.Enumeration;
import java.util.Map;

import jakarta.annotation.Nullable;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.logging.log4j.Logger;

import com.tibco.tibjms.admin.ServerInfo;
import com.tibco.tibjms.admin.TibjmsAdmin;
import com.tibco.tibjms.admin.TibjmsAdminException;

import org.frankframework.util.CredentialFactory;
import org.frankframework.util.LogUtil;

/**
 * Some utilities for working with TIBCO.
 *
 * @author Peter Leeuwenburgh
 * @author Jaco de Groot
 */
public class TibcoUtils {
	static Logger log = LogUtil.getLogger(TibcoUtils.class);

	private TibcoUtils() {
		// Private constructor to prevent creating instance of utility class
	}

	public static long getQueueFirstMessageAge(String provUrl,
			String authAlias, String userName, String password, String queueName)
			throws JMSException {
		return getQueueFirstMessageAge(provUrl, authAlias, userName, password,
				queueName, null);
	}

	/**
	 * return -1: no message found
	 * return -2: message found, but not of type Message.
	 */
	public static long getQueueFirstMessageAge(String provUrl,
			String authAlias, String userName, String password,
			String queueName, String messageSelector) throws JMSException {
		try (Connection connection = getConnection(provUrl, authAlias, userName, password);
			 Session jSession = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
		) {
			return getQueueFirstMessageAge(jSession, queueName, messageSelector);
		}
	}

	public static Connection getConnection(String provUrl, String authAlias,
			String userName, String password) throws JMSException {
		String url = StringUtils.replace(provUrl, "tibjmsnaming:", "tcp:");
		CredentialFactory cf = new CredentialFactory(authAlias, userName,
				password);
		ConnectionFactory factory = new com.tibco.tibjms.TibjmsConnectionFactory(
				url);
		return factory.createConnection(cf.getUsername(), cf.getPassword());
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
		return getQueueFirstMessageAge(jSession, queueName, messageSelector,
				currentTime, true);
	}

	protected static long getQueueFirstMessageAge(Session jSession,
			String queueName, String messageSelector, long currentTime,
			boolean warn) throws JMSException {
		QueueBrowser queueBrowser = null;
		try {
			Queue queue = jSession.createQueue(queueName);
			if (messageSelector == null) {
				queueBrowser = jSession.createBrowser(queue);
			} else {
				queueBrowser = jSession.createBrowser(queue, messageSelector);
			}
			Enumeration enm = queueBrowser.getEnumeration();
			if (enm.hasMoreElements()) {
				Object o = enm.nextElement();
				if (o instanceof Message msg) {
					long jmsTimestamp = msg.getJMSTimestamp();
					return currentTime - jmsTimestamp;
				} else {
					if (warn) {
						log.warn("message was not of type Message, but [{}]", o.getClass().getName());
					}
					return -2;
				}
			} else {
				return -1;
			}
		} finally {
			if (queueBrowser != null) {
				try {
					queueBrowser.close();
				} catch (JMSException e) {
					log.warn("Exception on closing queueBrowser", e);
				}
			}
		}
	}

	protected static String getQueueFirstMessageAgeAsString(Session jSession, String queueName, long currentTime) {
		try {
			long age = getQueueFirstMessageAge(jSession, queueName, null, currentTime, false);
			if (age == -1) {
				return null;
			} else if (age < -1) {
				return "??";
			} else {
				return DurationFormatUtils.formatDuration(age, "ddd-HH:mm:ss");
			}
		} catch (JMSException e) {
			return "?";
		}
	}

	public static long getQueueMessageCount(String provUrl, String authAlias,
			String userName, String password, String queueName,
			String messageSelector) throws JMSException {
		Connection connection = null;
		Session jSession = null;
		try {
			connection = getConnection(provUrl, authAlias, userName, password);
			jSession = connection.createSession(false,
					jakarta.jms.Session.AUTO_ACKNOWLEDGE);
			return getQueueMessageCount(jSession, queueName, messageSelector);
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

	protected static long getQueueMessageCount(Session jSession,
			String queueName, String messageSelector) throws JMSException {
		QueueBrowser queueBrowser = null;
		try {
			Queue queue = jSession.createQueue(queueName);
			if (messageSelector == null) {
				queueBrowser = jSession.createBrowser(queue);
			} else {
				queueBrowser = jSession.createBrowser(queue, messageSelector);
			}
			int count = 0;
			for (Enumeration enm = queueBrowser.getEnumeration(); enm
					.hasMoreElements(); enm.nextElement()) {
				count++;
			}
			return count;
		} finally {
			if (queueBrowser != null) {
				try {
					queueBrowser.close();
				} catch (JMSException e) {
					log.warn("Exception on closing queueBrowser", e);
				}
			}
		}
	}

	protected static @Nullable TibjmsAdmin getActiveServerAdmin(String url, CredentialFactory cf, Map<String, Object> connectionProperties) throws TibjmsAdminException {
		TibjmsAdminException lastException = null;
		TibjmsAdmin admin = null;
		String[] uws = url.split(",");
		String uw = null;
		boolean uwsOk = false;
		for (int i = 0; !uwsOk && i < uws.length; i++) {
			uw = uws[i].trim();
			int state = ServerInfo.SERVER_ACTIVE * -1;
			try {
				// The next line of code has been reported to throw the
				// following exception:
				//   com.tibco.tibjms.admin.TibjmsAdminException: Unable to connect to server. Root cause:
				//   jakarta.jms.ResourceAllocationException: too many open connections
				admin = new TibjmsAdmin(uw, cf.getUsername(), cf.getPassword(), connectionProperties);
				// The next line of code has been reported to throw the
				// following exception:
				//   com.tibco.tibjms.admin.TibjmsAdminSecurityException: Command unavailable on a server not in active state and using a JSON configuration file
				state = admin.getInfo().getState();
			} catch(TibjmsAdminException e) {
				// In case a passive or broken server is tried before an active
				// server this will result in an exception. Hence, ignore all
				// exceptions unless all servers fail in which case the latest
				// exception should be logged to give an indication of what is
				// going wrong.
				lastException = e;
			}
			if (admin != null) {
				if (state == ServerInfo.SERVER_ACTIVE) {
					uwsOk = true;
				} else {
					log.debug("Server [{}] is not active", uw);
					try {
						admin.close();
					} catch (TibjmsAdminException e) {
						log.warn("Exception on closing Tibjms Admin on server [{}]", uw, e);
					}
				}
			}
		}
		if (!uwsOk) {
			log.warn("Could not find an active server", lastException);
			return null;
		} else {
			log.debug("Found active server [{}]", uw);
			return admin;
		}
	}

	protected static void closeAdminClient(@Nullable TibjmsAdmin admin) {
		if (admin == null) {
			return;
		}

		// The Tibco JMS Admin client doesn't implement AutoCloseable unfortunately
		try {
			admin.close();
		} catch (TibjmsAdminException e) {
			log.warn("exception on closing Tibjms Admin", e);
		}
	}
}
