/*
   Copyright 2012-2016 the original author or authors, 2021-2025 WeAreFrank!

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
package org.frankframework.jta.narayana;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import jakarta.jms.XAConnectionFactory;

import org.jboss.narayana.jta.jms.ConnectionManager;
import org.jboss.tm.XAResourceWrapper;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;

/**
 * XAResourceRecoveryHelper implementation which gets XIDs, which needs to be recovered.
 * See org.jboss.narayana.jta.jms.JmsXAResourceRecoveryHelper.
 * 
 * Required as we wrap the connection in a pooling-capable factory, and do not use the native Narayana connection factory.
 * 
 * Additionally this also implements `XAResourceWrapper`, which (AFAIK) only adds debug info.
 * See XAResourceRecord#getJndiName()
 * 
 * @author Gytis Trikleris
 * @author Niels Meijer
 */
public class JmsXAResourceRecoveryHelper implements XAResourceRecoveryHelper, XAResource, XAResourceWrapper {

	private final ConnectionManager connectionManager;
	private final String name;

	public JmsXAResourceRecoveryHelper(XAConnectionFactory xaConnectionFactory, String name) {
		this.connectionManager = new ConnectionManager(xaConnectionFactory, null, null);
		this.name = name;
	}

	@Override
	public boolean initialise(String properties) {
		return true;
	}

	/**
	 * If JMS connection was created successfully, returns an array with one instance of JmsXAResourceRecoveryHelper. Otherwise,
	 * returns an empty array.
	 * @return Array with one instance of JmsXAResourceRecoveryHelper or an empty array
	 */
	@Override
	public XAResource[] getXAResources() {
		if (!connectionManager.isConnected()) {
			try {
				connectionManager.connect();
			} catch (XAException ignored) {
				return new XAResource[0];
			}
		}

		return new XAResource[] { this };
	}

	/**
	 * Delegates XAResource#recover call to the connected JMS resource. If provided argument is XAResource.TMENDRSCAN, then JMS
	 * connection will be closed at the end of the call.
	 */
	@Override
	public Xid[] recover(int flag) throws XAException {
		try {
			return connectionManager.connectAndApply(delegate -> delegate.recover(flag));
		} finally {
			if (flag == XAResource.TMENDRSCAN) {
				connectionManager.disconnect();
			}
		}
	}

	/**
	 * Delegates XAResource#start call to the connected JMS resource.
	 */
	@Override
	public void start(Xid xid, int flag) throws XAException {
		connectionManager.connectAndAccept(delegate -> delegate.start(xid, flag));
	}

	/**
	 * Delegates XAResource#end call to the connected JMS resource.
	 */
	@Override
	public void end(Xid xid, int flag) throws XAException {
		connectionManager.connectAndAccept(delegate -> delegate.end(xid, flag));
	}

	/**
	 * Delegates XAResource#prepare call to the connected JMS resource.
	 * @return Prepare outcome
	 */
	@Override
	public int prepare(Xid xid) throws XAException {
		return connectionManager.connectAndApply(delegate -> delegate.prepare(xid));
	}

	/**
	 * Delegates XAResource#commit call to the connected JMS resource.
	 */
	@Override
	public void commit(Xid xid, boolean onePhase) throws XAException {
		connectionManager.connectAndAccept(delegate -> delegate.commit(xid, onePhase));
	}

	/**
	 * Delegates XAResource#rollback call to the connected JMS resource.
	 */
	@Override
	public void rollback(Xid xid) throws XAException {
		connectionManager.connectAndAccept(delegate -> delegate.rollback(xid));
	}

	/**
	 * Delegates XAResource#isSameRM call to the connected JMS resource.
	 * @return True if is same resource manager or false if not.
	 */
	@Override
	public boolean isSameRM(XAResource xaResource) throws XAException {
		return connectionManager.connectAndApply(delegate -> delegate.isSameRM(xaResource));
	}

	/**
	 * Delegates XAResource#forget call to the connected JMS resource.
	 */
	@Override
	public void forget(Xid xid) throws XAException {
		connectionManager.connectAndAccept(delegate -> delegate.forget(xid));
	}

	/**
	 * Delegates XAResource#getTransactionTimeout call to the connected JMS resource.
	 * @return Transaction timeout value.
	 */
	@Override
	public int getTransactionTimeout() throws XAException {
		return connectionManager.connectAndApply(XAResource::getTransactionTimeout);
	}

	/**
	 * Delegates XAResource#setTransactionTimeout call to the connected JMS resource.
	 * @return True if transaction timeout was set, or false if wasn't.
	 */
	@Override
	public boolean setTransactionTimeout(int seconds) throws XAException {
		return connectionManager.connectAndApply(delegate -> delegate.setTransactionTimeout(seconds));
	}

	@Override
	public XAResource getResource() {
		return this;
	}

	@Override
	public String getProductName() {
		return null;
	}

	@Override
	public String getProductVersion() {
		return null;
	}

	@Override
	public String getJndiName() {
		return name;
	}

}