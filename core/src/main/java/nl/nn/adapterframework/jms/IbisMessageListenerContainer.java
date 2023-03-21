/*
   Copyright 2018 Nationale-Nederlanden, 2022, 2023 WeAreFrank!

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
package nl.nn.adapterframework.jms;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.apache.logging.log4j.Logger;
import org.springframework.jms.connection.JmsResourceHolder;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionStatus;

import nl.nn.adapterframework.jms.JMSFacade.AcknowledgeMode;
import nl.nn.adapterframework.unmanaged.SpringJmsConnector;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;

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
	protected Connection createConnection() throws JMSException {
		Connection conn;
		if (credentialFactory!=null) {
			conn = getConnectionFactory().createConnection(credentialFactory.getUsername(), credentialFactory.getPassword());
		} else {
			conn = super.createConnection();
		}
		if (log.isTraceEnabled()) log.trace("createConnection() - connection["+(conn==null?"null":conn.toString())+"]");
		return conn;
	}

	@Override
	protected Session createSession(Connection conn) throws JMSException {
		Session session = super.createSession(conn);
		if (log.isTraceEnabled()) log.trace("createSession() - ackMode["+getSessionAcknowledgeMode()+"] connection["+(conn==null?"null":conn.toString())+"] session["+(session==null?"null":session.toString())+"]");
		return session;
	}

	@Override
	protected Connection getConnection(JmsResourceHolder holder) {
		Connection conn = super.getConnection(holder);
		if (log.isTraceEnabled()) log.trace("getConnection() - jmsResourceHolder[" + holder.toString() + "] connection["+(conn==null?"null":conn.toString())+"]");
		return conn;
	}

	@Override
	protected Session getSession(JmsResourceHolder holder) {
		Session session = super.getSession(holder);
		if (log.isTraceEnabled()) log.trace("getSession() - ackMode["+getSessionAcknowledgeMode()+"] jmsResourceHolder[" + holder.toString() + "] session["+(session==null?"null":session.toString())+"]");
		return session;
	}

	@Override
	protected Connection createSharedConnection() throws JMSException {
		Connection conn = super.createSharedConnection();
		if (log.isTraceEnabled()) log.trace("createSharedConnection() - ackMode["+getSessionAcknowledgeMode()+"] connection["+(conn==null?"null":conn.toString())+"]");
		return conn;
	}

	@Override
	protected boolean receiveAndExecute(Object asyncMessageListenerInvoker, Session session, MessageConsumer consumer) throws JMSException {
		if (log.isTraceEnabled()) log.trace("receiveAndExecute() - destination["+getDestinationName()+"] clientId["+getClientId()+"] session["+session+"]");
		return super.receiveAndExecute(asyncMessageListenerInvoker, session, consumer);
	}

	@Override
	protected boolean doReceiveAndExecute(Object invoker, Session session, MessageConsumer consumer, TransactionStatus txStatus) throws JMSException {
		if (log.isTraceEnabled()) log.trace("doReceiveAndExecute() - destination["+getDestinationName()+"] clientId["+getClientId()+"] session["+session+"]");
		try {
			return super.doReceiveAndExecute(invoker, session, consumer, txStatus);
		} finally {
			if (getMessageListener() instanceof SpringJmsConnector) {
				SpringJmsConnector springJmsConnector = (SpringJmsConnector)getMessageListener();
				springJmsConnector.setLastPollFinishedTime(System.currentTimeMillis());
			}
		}
	}

	@Override
	protected void commitIfNecessary(Session session, @Nullable Message message) throws JMSException {
		if (message!=null && !session.getTransacted() && getMessageListener() instanceof SpringJmsConnector) {
			SpringJmsConnector springJmsConnector = (SpringJmsConnector)getMessageListener();
			JmsListener listener = (JmsListener)springJmsConnector.getListener();
			if (listener.getAcknowledgeModeEnum()==AcknowledgeMode.CLIENT_ACKNOWLEDGE) {
				// Avoid message.acknowledge() in super.commitIfNecessray if AcknowledgeMode=CLIENT_ACKNOWLEDGE
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
