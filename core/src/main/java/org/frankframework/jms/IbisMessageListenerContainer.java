/*
   Copyright 2018 Nationale-Nederlanden, 2022-2024 WeAreFrank!

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
package org.frankframework.jms;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;

import org.apache.logging.log4j.Logger;
import org.springframework.jms.connection.JmsResourceHolder;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.transaction.TransactionStatus;

import org.frankframework.unmanaged.SpringJmsConnector;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.LogUtil;

/**
 * Extend the DefaultMessageListenerContainer from Spring to add trace logging and make it possible to monitor the last
 * poll finished time.
 *
 * @author Niels Meijer
 * @author Jaco de Groot
 */
public class IbisMessageListenerContainer extends DefaultMessageListenerContainer {
	protected Logger log = LogUtil.getLogger(this);

	private CredentialFactory credentialFactory;

	@Override
	@Nonnull
	protected Connection createConnection() throws JMSException {
		Connection conn;
		if (credentialFactory!=null) {
			conn = obtainConnectionFactory().createConnection(credentialFactory.getUsername(), credentialFactory.getPassword());
		} else {
			conn = super.createConnection();
		}
		if (log.isTraceEnabled()) log.trace("createConnection() - connection[{}]", conn);
		return conn;
	}

	@Override
	@Nonnull
	protected Session createSession(@Nonnull Connection conn) throws JMSException {
		Session session = super.createSession(conn);
		if (log.isTraceEnabled())
			log.trace("createSession() - ackMode[{}] connection[{}] session[{}]", getSessionAcknowledgeMode(), conn, session);
		return session;
	}

	@Override
	protected Connection getConnection(@Nonnull JmsResourceHolder holder) {
		Connection conn = super.getConnection(holder);
		if (log.isTraceEnabled())
			log.trace("getConnection() - jmsResourceHolder[{}] connection[{}]", holder, conn == null ? "null" : conn.toString());
		return conn;
	}

	@Override
	protected Session getSession(@Nonnull JmsResourceHolder holder) {
		Session session = super.getSession(holder);
		if (log.isTraceEnabled())
			log.trace("getSession() - ackMode[{}] jmsResourceHolder[{}] session[{}]", getSessionAcknowledgeMode(), holder, session == null ? "null" : session.toString());
		return session;
	}

	@Override
	@Nonnull
	protected Connection createSharedConnection() throws JMSException {
		Connection conn = super.createSharedConnection();
		if (log.isTraceEnabled())
			log.trace("createSharedConnection() - ackMode[{}] connection[{}]", getSessionAcknowledgeMode(), conn.toString());
		return conn;
	}

	@Override
	protected boolean receiveAndExecute(@Nonnull Object asyncMessageListenerInvoker, @Nullable Session session, @Nullable MessageConsumer consumer) throws JMSException {
		if (log.isTraceEnabled())
			log.trace("receiveAndExecute() - destination[{}] clientId[{}] session[{}]", getDestinationName(), getClientId(), session);
		return super.receiveAndExecute(asyncMessageListenerInvoker, session, consumer);
	}

	@Override
	protected boolean doReceiveAndExecute(@Nonnull Object invoker, @Nullable Session session, @Nullable MessageConsumer consumer, @Nullable TransactionStatus txStatus) throws JMSException {
		if (log.isTraceEnabled())
			log.trace("doReceiveAndExecute() - destination[{}] clientId[{}] session[{}]", getDestinationName(), getClientId(), session);
		try {
			return super.doReceiveAndExecute(invoker, session, consumer, txStatus);
		} finally {
			if (getMessageListener() instanceof SpringJmsConnector springJmsConnector) {
				springJmsConnector.setLastPollFinishedTime(System.currentTimeMillis());
			}
		}
	}

	@Override
	protected void commitIfNecessary(@Nonnull Session session, @Nullable Message message) throws JMSException {
		if (message!=null && !session.getTransacted() && getMessageListener() instanceof SpringJmsConnector springJmsConnector) {
			JmsListener listener = (JmsListener)springJmsConnector.getListener();
			if (listener.getAcknowledgeMode() == JMSFacade.AcknowledgeMode.CLIENT_ACKNOWLEDGE) {
				// Avoid message.acknowledge() in super.commitIfNecessary if AcknowledgeMode=CLIENT_ACKNOWLEDGE
				// Acknowledgement for CLIENT_ACKNOWLEDGE is done in afterMessageProcessed
				log.debug("Skip client acknowledge in commitIfNecessary()");
				return;
			}
		}
		super.commitIfNecessary(session, message);
	}

	public void setCredentialFactory(CredentialFactory credentialFactory) {
		this.credentialFactory = credentialFactory;
	}
	public CredentialFactory getCredentialFactory() {
		return credentialFactory;
	}

}
