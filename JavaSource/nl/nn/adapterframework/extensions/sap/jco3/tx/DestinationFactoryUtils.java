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
/*
 * $Log: DestinationFactoryUtils.java,v $
 * Revision 1.1  2012-02-06 14:33:05  m00f069
 * Implemented JCo 3 based on the JCo 2 code. JCo2 code has been moved to another package, original package now contains classes to detect the JCo version available and use the corresponding implementation.
 *
 * Revision 1.4  2011/11/30 13:51:53  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2008/01/30 14:41:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified javadoc
 *
 * Revision 1.1  2008/01/29 15:49:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */

package nl.nn.adapterframework.extensions.sap.jco3.tx;

import nl.nn.adapterframework.extensions.sap.jco3.SapException;
import nl.nn.adapterframework.extensions.sap.jco3.SapSystem;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;

/**
 * Helper class for managing a JCo destinations, in particular
 * for obtaining transactional resources.
 *
 * <p>based on {@link org.springframework.jms.connection.ConnectionFactoryUtils}
 * 
 * @author  Gerrit van Brakel
 * @author  Jaco de Groot
 * @since   5.0
 * @version $Id$
 */
public abstract class DestinationFactoryUtils {
	private static final Logger logger = LogUtil.getLogger(DestinationFactoryUtils.class);

	/**
	 * Determine whether the given JCoDestination is transactional, that is,
	 * bound to the current thread by Spring's transaction facilities.
	 * @param destination the JCoDestination to check
	 * @param sapSystem the SapSystem that the Session originated from
	 * @return whether the Destination is transactional
	 */
	public static boolean isDestinationTransactional(JCoDestination destination, SapSystem sapSystem) {
		if (destination == null || sapSystem == null) {
			return false;
		}
		JcoResourceHolder resourceHolder = (JcoResourceHolder) TransactionSynchronizationManager.getResource(sapSystem);
		return (resourceHolder != null && resourceHolder.containsDestination(destination));
	}


	/**
	 * Obtain a TID String that is synchronized with the current transaction, if any.
	 * @param sapSystem the SapSystem to obtain a TID for
	 * @param existingCon the existing JCoDestination to obtain a String for
	 * (may be <code>null</code>)
	 * @param synchedLocalTransactionAllowed whether to allow for a local JMS transaction
	 * that is synchronized with a Spring-managed transaction (where the main transaction
	 * might be a JDBC-based one for a specific DataSource, for example), with the JMS
	 * transaction committing right after the main transaction. If not allowed, the given
	 * SapSystem needs to handle transaction enlistment underneath the covers.
	 * @return the TID, or <code>null</code> if none found
	 * @throws SapException in case of JCo failure
	 * @throws JCoException 
	 */
	public static String getTransactionalTid(
			final SapSystem sapSystem, final JCoDestination existingDestination, final boolean synchedLocalTransactionAllowed)
			throws SapException, JCoException {

		return doGetTransactionalTid(sapSystem, new ResourceFactory() {
			public String getTid(JcoResourceHolder holder) {
				return holder.getTid(existingDestination);
			}
			public JCoDestination getDestination(JcoResourceHolder holder) {
				return (existingDestination != null ? existingDestination : holder.getDestination());
			}
			public JCoDestination createDestination() throws JCoException {
				return sapSystem.getDestination();
			}
			public String createTid(JCoDestination destination) throws JCoException {
				return destination.createTID();
			}
			public boolean isSynchedLocalTransactionAllowed() {
				return synchedLocalTransactionAllowed;
			}
		});
	}

	public static JCoDestination getTransactionalDestination(
			final SapSystem sapSystem, final boolean synchedLocalTransactionAllowed) 
			throws SapException, JCoException {

		return doGetTransactionalDestination(sapSystem, new ResourceFactory() {
			public String getTid(JcoResourceHolder holder) {
				return holder.getTid(holder.getDestination());
			}
			public JCoDestination getDestination(JcoResourceHolder holder) {
				return holder.getDestination();
			}
			public JCoDestination createDestination() throws JCoException {
				return sapSystem.getDestination();
			}
			public String createTid(JCoDestination destination) throws JCoException {
				return destination.createTID();
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
	 * @param startDestination whether the underlying JMS JCoDestination approach should be
	 * started in order to allow for receiving messages. Note that a reused JCoDestination
	 * may already have been started before, even if this flag is <code>false</code>.
	 * @return the transactional String, or <code>null</code> if none found
	 * @throws JCoException in case of failure
	 */
	public static String doGetTransactionalTid(SapSystem sapSystem, ResourceFactory resourceFactory)
			throws JCoException {

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
		JCoDestination destination = resourceFactory.getDestination(resourceHolderToUse);
		String tid = null;
		boolean isExistingDestination = (destination != null);
		if (!isExistingDestination) {
			destination = resourceFactory.createDestination();
			resourceHolderToUse.addDestination(destination);
		}
		tid = resourceFactory.createTid(destination);
		resourceHolderToUse.addTid(tid, destination);
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
	 * Obtain a JCoDestination that is synchronized with the current transaction, if any.
	 * @param sapSystem the JMS SapSystem to bind for
	 * (used as TransactionSynchronizationManager key)
	 * @param resourceFactory the ResourceFactory to use for extracting or creating
	 * JMS resources
	 * @return the transactional JCoDestination, or <code>null</code> if none found
	 * @throws JCoException in case of failure
	 */
	public static JCoDestination doGetTransactionalDestination(SapSystem sapSystem, ResourceFactory resourceFactory) throws JCoException
			{

		Assert.notNull(sapSystem, "SapSystem must not be null");
		Assert.notNull(resourceFactory, "ResourceFactory must not be null");

		JcoResourceHolder resourceHolder =
				(JcoResourceHolder) TransactionSynchronizationManager.getResource(sapSystem);
		if (resourceHolder != null) {
			JCoDestination destination = resourceFactory.getDestination(resourceHolder);
			if (destination != null) {
				return destination;
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
		JCoDestination destination=null;
		destination = resourceFactory.createDestination();
		resourceHolderToUse.addDestination(destination);
		if (resourceHolderToUse != resourceHolder) {
			TransactionSynchronizationManager.registerSynchronization(
					new JcoResourceSynchronization(
							sapSystem, resourceHolderToUse, resourceFactory.isSynchedLocalTransactionAllowed()));
			resourceHolderToUse.setSynchronizedWithTransaction(true);
			TransactionSynchronizationManager.bindResource(sapSystem, resourceHolderToUse);
		}
		return destination;
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
		 * Fetch an appropriate JCoDestination from the given JcoResourceHolder.
		 * @param holder the JcoResourceHolder
		 * @return an appropriate JCoDestination fetched from the holder,
		 * or <code>null</code> if none found
		 */
		JCoDestination getDestination(JcoResourceHolder holder);

		/**
		 * Create a new JCoDestination for registration with a JcoResourceHolder.
		 * @return the new JCoDestination
		 */
		JCoDestination createDestination() throws JCoException;

		/**
		 * Create a new Tid for registration with a JcoResourceHolder.
		 * @param destination the JCoDestination to create a String for
		 * @return the new Tid
		 * @throws JCoException if thrown by API methods
		 */
		String createTid(JCoDestination destination) throws JCoException;

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
	}

}
