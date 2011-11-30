/*
 * $Log: TransactionAwareClientFactory.java,v $
 * Revision 1.3  2011-11-30 13:51:53  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2008/01/29 15:49:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */

package nl.nn.adapterframework.extensions.sap.tx;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import nl.nn.adapterframework.extensions.sap.SapException;
import nl.nn.adapterframework.extensions.sap.SapSystem;

import org.springframework.util.Assert;

import com.sap.mw.jco.JCO;

/**
 * Proxy for a target JMS {@link javax.jms.ConnectionFactory}, adding awareness of
 * Spring-managed transactions. Similar to a transactional JNDI ConnectionFactory
 * as provided by a J2EE server.
 *
 * <p>Messaging code that should remain unaware of Spring's JMS support can work
 * with this proxy to seamlessly participate in Spring-managed transactions.
 * Note that the transaction manager, for example {@link JmsTransactionManager},
 * still needs to work with the underlying ConnectionFactory, <i>not</i> with
 * this proxy.
 *
 * <p><b>Make sure that TransactionAwareConnectionFactoryProxy is the outermost
 * ConnectionFactory of a chain of ConnectionFactory proxies/adapters.</b>
 * TransactionAwareConnectionFactoryProxy can delegate either directly to the
 * target factory or to some intermediary adapter like
 * {@link UserCredentialsConnectionFactoryAdapter}.
 *
 * <p>Delegates to {@link ConnectionFactoryUtils} for automatically participating
 * in thread-bound transactions, for example managed by {@link JmsTransactionManager}.
 * <code>createSession</code> calls and <code>close</code> calls on returned Sessions
 * will behave properly within a transaction, that is, always work on the transactional
 * Session. If not within a transaction, normal ConnectionFactory behavior applies.
 *
 * <p>Note that transactional JMS Sessions will be registered on a per-Connection
 * basis. To share the same JMS Session across a transaction, make sure that you
 * operate on the same JMS Connection handle - either through reusing the handle
 * or through configuring a {@link SingleConnectionFactory} underneath.
 *
 * <p>Returned transactional Session proxies will implement the {@link SessionProxy}
 * interface to allow for access to the underlying target Session. This is only
 * intended for accessing vendor-specific Session API or for testing purposes
 * (e.g. to perform manual transaction control). For typical application purposes,
 * simply use the standard JMS Session interface.
 * 
 * <p>based on {@link org.springframework.jms.connection.TransactionAwareConnectionFactoryProxy}
 *
 * @author  Gerrit van Brakel
 * @since   4.8
 * @version Id
 */
public class TransactionAwareClientFactory {

	private boolean synchedLocalTransactionAllowed = false;

	private SapSystem targetSapSystem;


	/**
	 * Create a new TransactionAwareClientFactory.
	 */
	public TransactionAwareClientFactory() {
	}

	/**
	 * Create a new TransactionAwareClientFactory.
	 * @param targetSapSystem the target SapSystem
	 */
	public TransactionAwareClientFactory(SapSystem targetSapSystem) {
		setTargetSapSystem(targetSapSystem);
	}


	/**
	 * Set the target SapSystem that this ClientFactory should delegate to.
	 */
	public final void setTargetSapSystem(SapSystem targetSapSystem) {
		Assert.notNull(targetSapSystem, "targetSapSystem must not be nul");
		this.targetSapSystem = targetSapSystem;
	}

	/**
	 * Return the target SapSystem that this ClientFactory should delegate to.
	 */
	protected SapSystem getTargetSapSystem() {
		return this.targetSapSystem;
	}

	/**
	 * Set whether to allow for a local JMS transaction that is synchronized with a
	 * Spring-managed transaction (where the main transaction might be a JDBC-based
	 * one for a specific DataSource, for example), with the JMS transaction committing
	 * right after the main transaction. If not allowed, the given SapSystem
	 * needs to handle transaction enlistment underneath the covers.
	 * <p>Default is "false": If not within a managed transaction that encompasses
	 * the underlying JMS SapSystem, standard Sessions will be returned.
	 * Turn this flag on to allow participation in any Spring-managed transaction,
	 * with a local JMS transaction synchronized with the main transaction.
	 */
	public void setSynchedLocalTransactionAllowed(boolean synchedLocalTransactionAllowed) {
		this.synchedLocalTransactionAllowed = synchedLocalTransactionAllowed;
	}

	/**
	 * Return whether to allow for a local JMS transaction that is synchronized
	 * with a Spring-managed transaction.
	 */
	protected boolean isSynchedLocalTransactionAllowed() {
		return this.synchedLocalTransactionAllowed;
	}


	public JCO.Client getClient() throws SapException {
		JCO.Client targetClient = ClientFactoryUtils.getTransactionalClient(this.targetSapSystem, isSynchedLocalTransactionAllowed());
		return getTransactionAwareClientProxy(targetClient);
	}

//	public JCO.Client createConnection(String username, String password) throws JMSException {
//		JCO.Client targetConnection = this.targetSapSystem.getClient(username, password);
//		return getTransactionAwareConnectionProxy(targetConnection);
//	}



	/**
	 * Wrap the given JCO.Client with a proxy that delegates every method call to it
	 * but handles TID creation in a transaction-aware fashion.
	 * @param target the original JCO.Client to wrap
	 * @return the wrapped JCO.Client
	 */
	private JCO.Client getTransactionAwareClientProxy(JCO.Client target) {
		List classes = new ArrayList(1);
		classes.add(JCO.Client.class);
//		if (target instanceof QueueClient) {
//			classes.add(QueueClient.class);
//		}
//		if (target instanceof TopicClient) {
//			classes.add(TopicClient.class);
//		}
		return (JCO.Client) Proxy.newProxyInstance(
				getClass().getClassLoader(),
				(Class[]) classes.toArray(new Class[classes.size()]),
				new TransactionAwareClientInvocationHandler(target));
	}


	/**
	 * Invocation handler that exposes transactional TID for the underlying JCO.Client.
	 */
	private class TransactionAwareClientInvocationHandler implements InvocationHandler {

		private final JCO.Client target;

		public TransactionAwareClientInvocationHandler(JCO.Client target) {
			this.target = target;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			// Invocation on ClientProxy interface coming in...

			if (method.getName().equals("createTID")) {
				String tid = ClientFactoryUtils.getTransactionalTid(
						getTargetSapSystem(), this.target, isSynchedLocalTransactionAllowed());
				if (tid != null) {
					return tid;
				}
			}
			else if (method.getName().equals("equals")) {
				// Only consider equal when proxies are identical.
				return (proxy == args[0] ? Boolean.TRUE : Boolean.FALSE);
			}
			else if (method.getName().equals("hashCode")) {
				// Use hashCode of JCO.Client proxy.
				return new Integer(hashCode());
			}

			// Invoke method on target JCO.Client.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}


}
