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
package nl.nn.adapterframework.extensions.sap.jco2.tx;

import nl.nn.adapterframework.extensions.sap.jco2.SapException;
import nl.nn.adapterframework.extensions.sap.jco2.SapSystem;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import com.sap.mw.jco.JCO;

/**
 * Helper class for managing a JCo clients, in particular
 * for obtaining transactional resources.
 *
 * <p>based on {@link org.springframework.jms.connection.ConnectionFactoryUtils}
 * 
 * @author  Gerrit van Brakel
 * @since   4.8
 */
public abstract class ClientFactoryUtils {
	private static final Logger logger = LogUtil.getLogger(ClientFactoryUtils.class);


	/**
	 * Release the given Client.
	 * @param client the Client to release
	 * (if this is <code>null</code>, the call will be ignored)
	 * @param sapSystem the SapSystem that the Client was obtained from
	 * (may be <code>null</code>)
	 */
	public static void releaseClient(JCO.Client client, SapSystem sapSystem) {
		if (client == null) {
			return;
		}
		try {
			sapSystem.releaseClient(client);
		}
		catch (Throwable ex) {
			logger.debug("Could not release JCO.Client", ex);
		}
	}

	/**
	 * Determine whether the given JCO.Client is transactional, that is,
	 * bound to the current thread by Spring's transaction facilities.
	 * @param client the JCO.Client to check
	 * @param sapSystem the SapSystem that the Session originated from
	 * @return whether the Client is transactional
	 */
	public static boolean isClientTransactional(JCO.Client client, SapSystem sapSystem) {
		if (client == null || sapSystem == null) {
			return false;
		}
		JcoResourceHolder resourceHolder = (JcoResourceHolder) TransactionSynchronizationManager.getResource(sapSystem);
		return (resourceHolder != null && resourceHolder.containsClient(client));
	}


	/**
	 * Obtain a TID String that is synchronized with the current transaction, if any.
	 * @param sapSystem the SapSystem to obtain a TID for
	 * @param synchedLocalTransactionAllowed whether to allow for a local JMS transaction
	 * that is synchronized with a Spring-managed transaction (where the main transaction
	 * might be a JDBC-based one for a specific DataSource, for example), with the JMS
	 * transaction committing right after the main transaction. If not allowed, the given
	 * SapSystem needs to handle transaction enlistment underneath the covers.
	 * @return the TID, or <code>null</code> if none found
	 * @throws SapException in case of JCo failure
	 */
	public static String getTransactionalTid(
			final SapSystem sapSystem, final JCO.Client existingClient, final boolean synchedLocalTransactionAllowed)
			throws SapException {

		return doGetTransactionalTid(sapSystem, new ResourceFactory() {
			public String getTid(JcoResourceHolder holder) {
				return holder.getTid(existingClient);
			}
			public JCO.Client getClient(JcoResourceHolder holder) {
				return (existingClient != null ? existingClient : holder.getClient());
			}
			public JCO.Client createClient() throws SapException {
				return sapSystem.getClient();
			}
			public String createTid(JCO.Client client) throws SapException {
				return client.createTID();
			}
			public boolean isSynchedLocalTransactionAllowed() {
				return synchedLocalTransactionAllowed;
			}
		});
	}

	public static JCO.Client getTransactionalClient(
			final SapSystem sapSystem, final boolean synchedLocalTransactionAllowed) 
			throws SapException {

		return doGetTransactionalClient(sapSystem, new ResourceFactory() {
			public String getTid(JcoResourceHolder holder) {
				return holder.getTid(holder.getClient());
			}
			public JCO.Client getClient(JcoResourceHolder holder) {
				return holder.getClient();
			}
			public JCO.Client createClient() throws SapException {
				return sapSystem.getClient();
			}
			public String createTid(JCO.Client client) throws SapException {
				return client.createTID();
			}
			public boolean isSynchedLocalTransactionAllowed() {
				return synchedLocalTransactionAllowed;
			}
		});
	}

	/**
	 * Obtain a JMS String that is synchronized with the current transaction, if any.
	 * @param sapSystem the JMS SapSystem to bind for
	 * (used as TransactionSynchronizationManager key)
	 * @param resourceFactory the ResourceFactory to use for extracting or creating
	 * JMS resources
	 * @return the transactional String, or <code>null</code> if none found
	 */
	public static String doGetTransactionalTid(SapSystem sapSystem, ResourceFactory resourceFactory)
			throws SapException {

		Assert.notNull(sapSystem, "SapSystem must not be null");
		Assert.notNull(resourceFactory, "ResourceFactory must not be null");

		JcoResourceHolder resourceHolder =
				(JcoResourceHolder) TransactionSynchronizationManager.getResource(sapSystem);
		if (resourceHolder != null) {
			String tid = resourceFactory.getTid(resourceHolder);
			if (tid != null) {
				return tid;
			}
			if (resourceHolder.isFrozen()) {
				return null;
			}
		}
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return null;
		}
		JcoResourceHolder resourceHolderToUse = resourceHolder;
		if (resourceHolderToUse == null) {
			resourceHolderToUse = new JcoResourceHolder(sapSystem);
		}
		JCO.Client client = resourceFactory.getClient(resourceHolderToUse);
		String tid = null;
		try {
			boolean isExistingClient = (client != null);
			if (!isExistingClient) {
				client = resourceFactory.createClient();
				resourceHolderToUse.addClient(client);
			}
			tid = resourceFactory.createTid(client);
			resourceHolderToUse.addTid(tid, client);
		}
		catch (SapException ex) {
			if (client != null) {
				try {
					sapSystem.releaseClient(client);
				}
				catch (Throwable ex2) {
					// ignore
				}
			}
			throw ex;
		}
		if (resourceHolderToUse != resourceHolder) {
			TransactionSynchronizationManager.registerSynchronization(
					new JcoResourceSynchronization(
							sapSystem, resourceHolderToUse, resourceFactory.isSynchedLocalTransactionAllowed()));
			resourceHolderToUse.setSynchronizedWithTransaction(true);
			TransactionSynchronizationManager.bindResource(sapSystem, resourceHolderToUse);
		}
		return tid;
	}

	/**
	 * Obtain a JCO.Client that is synchronized with the current transaction, if any.
	 * @param sapSystem the JMS SapSystem to bind for
	 * (used as TransactionSynchronizationManager key)
	 * @param resourceFactory the ResourceFactory to use for extracting or creating
	 * JMS resources
	 * @return the transactional JCO.Client, or <code>null</code> if none found
	 */
	public static JCO.Client doGetTransactionalClient(SapSystem sapSystem, ResourceFactory resourceFactory) throws SapException
			{

		Assert.notNull(sapSystem, "SapSystem must not be null");
		Assert.notNull(resourceFactory, "ResourceFactory must not be null");

		JcoResourceHolder resourceHolder =
				(JcoResourceHolder) TransactionSynchronizationManager.getResource(sapSystem);
		if (resourceHolder != null) {
			JCO.Client client = resourceFactory.getClient(resourceHolder);
			if (client != null) {
				return client;
			}
			if (resourceHolder.isFrozen()) {
				return null;
			}
		}
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return null;
		}
		JcoResourceHolder resourceHolderToUse = resourceHolder;
		if (resourceHolderToUse == null) {
			resourceHolderToUse = new JcoResourceHolder(sapSystem);
		}
		JCO.Client client=null;
		try {
			client = resourceFactory.createClient();
		} catch (SapException ex) {
			if (client != null) {
				try {
					sapSystem.releaseClient(client);
				}
				catch (Throwable ex2) {
					// ignore
				}
			}
			throw ex;
		}
		resourceHolderToUse.addClient(client);
		if (resourceHolderToUse != resourceHolder) {
			TransactionSynchronizationManager.registerSynchronization(
					new JcoResourceSynchronization(
							sapSystem, resourceHolderToUse, resourceFactory.isSynchedLocalTransactionAllowed()));
			resourceHolderToUse.setSynchronizedWithTransaction(true);
			TransactionSynchronizationManager.bindResource(sapSystem, resourceHolderToUse);
		}
		return client;
	}


	/**
	 * Callback interface for resource creation.
	 * Serving as argument for the <code>doGetTransactionalTid</code> method.
	 */
	public interface ResourceFactory {

		/**
		 * Fetch an appropriate Tid from the given JcoResourceHolder.
		 * @param holder the JcoResourceHolder
		 * @return an appropriate Tid fetched from the holder,
		 * or <code>null</code> if none found
		 */
		String getTid(JcoResourceHolder holder);

		/**
		 * Fetch an appropriate JCO.Client from the given JcoResourceHolder.
		 * @param holder the JcoResourceHolder
		 * @return an appropriate JCO.Client fetched from the holder,
		 * or <code>null</code> if none found
		 */
		JCO.Client getClient(JcoResourceHolder holder);

		/**
		 * Create a new JCO.Client for registration with a JcoResourceHolder.
		 * @return the new JCO.Client
		 */
		JCO.Client createClient() throws SapException;

		/**
		 * Create a new Tid for registration with a JcoResourceHolder.
		 * @param client the JCO.Client to create a String for
		 * @return the new Tid
		 */
		String createTid(JCO.Client client) throws SapException;

		/**
		 * Return whether to allow for a local JMS transaction that is synchronized with
		 * a Spring-managed transaction (where the main transaction might be a JDBC-based
		 * one for a specific DataSource, for example), with the JMS transaction
		 * committing right after the main transaction.
		 * @return whether to allow for synchronizing a local JMS transaction
		 */
		boolean isSynchedLocalTransactionAllowed();
	}


	/**
	 * Callback for resource cleanup at the end of a non-native JMS transaction
	 * (e.g. when participating in a JtaTransactionManager transaction).
	 * @see org.springframework.transaction.jta.JtaTransactionManager
	 */
	private static class JcoResourceSynchronization extends TransactionSynchronizationAdapter {

		private final Object resourceKey;

		private final JcoResourceHolder resourceHolder;

		private final boolean synchedLocalTransacted; // will always be true...

		private boolean holderActive = true;

		public JcoResourceSynchronization(Object resourceKey, JcoResourceHolder resourceHolder, boolean synchedLocalTransacted) {
			this.resourceKey = resourceKey;
			this.resourceHolder = resourceHolder;
			this.synchedLocalTransacted = synchedLocalTransacted;
		}

		public void suspend() {
			if (this.holderActive) {
				TransactionSynchronizationManager.unbindResource(this.resourceKey);
			}
		}

		public void resume() {
			if (this.holderActive) {
				TransactionSynchronizationManager.bindResource(this.resourceKey, this.resourceHolder);
			}
		}

		public void beforeCompletion() {
			TransactionSynchronizationManager.unbindResource(this.resourceKey);
			this.holderActive = false;
			if (!this.synchedLocalTransacted) {
				this.resourceHolder.closeAll();
			}
		}

		public void afterCommit() {
			if (this.synchedLocalTransacted) {
				try {
					this.resourceHolder.commitAll();
				} catch (SapException ex) {
					throw new SynchedLocalTransactionFailedException("Local JMS transaction failed to commit", ex);
				}
			}
		}

		public void afterCompletion(int status) {
			if (this.synchedLocalTransacted) {
				this.resourceHolder.closeAll();
			}
		}
	}

}
