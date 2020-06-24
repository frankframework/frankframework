/*
   Copyright 2018 Nationale-Nederlanden

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
import javax.jms.MessageConsumer;
import javax.jms.Session;

import org.apache.logging.log4j.Logger;
import org.springframework.jms.connection.JmsResourceHolder;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.transaction.TransactionStatus;

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
		log.trace("createConnection() - connection["+(conn==null?"null":conn.toString())+"]");
		return conn;
	}

	@Override
	protected Session createSession(Connection conn) throws JMSException {
		Session session = super.createSession(conn);
		log.trace("createSession() - ackMode["+getSessionAcknowledgeMode()+"] connection["+(conn==null?"null":conn.toString())+"] session["+(session==null?"null":session.toString())+"]");
		return session;
	}

	@Override
	protected Connection getConnection(JmsResourceHolder holder) {
		Connection conn = super.getConnection(holder);
		log.trace("getConnection() - jmsResourceHolder[" + holder.toString() + "] connection["+(conn==null?"null":conn.toString())+"]");
		return conn;
	}

	@Override
	protected Session getSession(JmsResourceHolder holder) {
		Session session = super.getSession(holder);
		log.trace("getSession() - ackMode["+getSessionAcknowledgeMode()+"] jmsResourceHolder[" + holder.toString() + "] session["+(session==null?"null":session.toString())+"]");
		return session;
	}

	@Override
	protected Connection createSharedConnection() throws JMSException {
		Connection conn = super.createSharedConnection();
		log.trace("createSharedConnection() - ackMode["+getSessionAcknowledgeMode()+"] connection["+(conn==null?"null":conn.toString())+"]");
		return conn;
	}

	@Override
	protected boolean receiveAndExecute(Object asyncMessageListenerInvoker, Session session, MessageConsumer consumer) throws JMSException {
		log.trace("receiveAndExecute() - destination["+getDestinationName()+"] clientId["+getClientId()+"] session["+session+"]");
		return super.receiveAndExecute(asyncMessageListenerInvoker, session, consumer);
	}

	@Override
	protected boolean doReceiveAndExecute(Object invoker, Session session, MessageConsumer consumer, TransactionStatus txStatus) throws JMSException {
		log.trace("doReceiveAndExecute() - destination["+getDestinationName()+"] clientId["+getClientId()+"] session["+session+"]");
		boolean messageReceived = super.doReceiveAndExecute(invoker, session, consumer, txStatus);
		if (getMessageListener() instanceof SpringJmsConnector) {
			SpringJmsConnector springJmsConnector = (SpringJmsConnector)getMessageListener();
			springJmsConnector.setLastPollFinishedTime(System.currentTimeMillis());
		}
		return messageReceived;
	}

	public void setCredentialFactory(CredentialFactory credentialFactory) {
		this.credentialFactory = credentialFactory;
	}
	public CredentialFactory getCredentialFactory() {
		return credentialFactory;
	}

}
