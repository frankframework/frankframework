package org.frankframework.jms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.XAConnection;
import jakarta.jms.XAConnectionFactory;
import jakarta.jms.XAJMSContext;

import org.jboss.narayana.jta.jms.ConnectionFactoryProxy;
import org.junit.jupiter.api.Test;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;
import org.springframework.jms.connection.DelegatingConnectionFactory;

public class JmsPoolUtilTest {

	@Test
	public void reflectionToStringNull() {
		assertEquals("<null>", JmsPoolUtil.reflectionToString(null));
	}

	@Test
	public void reflectionToString() {
		ConnectionFactory qcf = new TestConnectionFactory();
		String expected = "JmsPoolUtilTest.TestConnectionFactory[password=**********,privateField=123]";
		assertEquals(expected, JmsPoolUtil.reflectionToString(qcf));
	}

	@Test
	public void reflectionToStringDelegated() {
		ConnectionFactory qcf = new TestConnectionFactory();

		JmsPoolConnectionFactory delegate1 = new JmsPoolConnectionFactory();
		delegate1.setConnectionFactory(qcf);

		DelegatingConnectionFactory delegate2 = new DelegatingConnectionFactory();
		delegate2.setTargetConnectionFactory(delegate1);
		DelegatingConnectionFactory delegate3 = new DelegatingConnectionFactory();
		delegate3.setTargetConnectionFactory(delegate2);

		String expected = "JmsPoolUtilTest.TestConnectionFactory[password=**********,privateField=123]";
		assertEquals(expected, JmsPoolUtil.reflectionToString(delegate3));

		TransactionalMetadataAwareConnectionFactoryProxy outerProxy = new TransactionalMetadataAwareConnectionFactoryProxy(delegate3);
		assertEquals(expected, outerProxy.toString());
	}

	@Test
	public void reflectionToStringNarayanaXaProxy() {
		TestConnectionFactory qcf = new TestConnectionFactory();

		ConnectionFactoryProxy delegate1 = new ConnectionFactoryProxy(qcf, null);

		DelegatingConnectionFactory delegate2 = new DelegatingConnectionFactory();
		delegate2.setTargetConnectionFactory(delegate1);
		DelegatingConnectionFactory delegate3 = new DelegatingConnectionFactory();
		delegate3.setTargetConnectionFactory(delegate2);

		String expected = "JmsPoolUtilTest.TestConnectionFactory[password=**********,privateField=123]";
		assertEquals(expected, JmsPoolUtil.reflectionToString(delegate3));

		TransactionalMetadataAwareConnectionFactoryProxy outerProxy = new TransactionalMetadataAwareConnectionFactoryProxy(delegate3);
		assertEquals(expected, outerProxy.toString());
	}

	@Test
	public void getConnectionPoolInfo() {
		ConnectionFactory qcf = new TestConnectionFactory();
		JmsPoolConnectionFactory pool = new JmsPoolConnectionFactory();
		pool.setConnectionFactory(qcf);

		DelegatingConnectionFactory delegate = new DelegatingConnectionFactory();
		delegate.setTargetConnectionFactory(pool);

		assertEquals("JmsPoolConnectionFactory Pool Info: current pool size [0], max pool size [1], "
				+ "max sessions per connection [500], block if session pool is full [true], "
				+ "block if session pool is full timeout [-1], connection check interval (ms) [-1], "
				+ "connection idle timeout (s) [30]", JmsPoolUtil.getConnectionPoolInfo(delegate));
	}

	@Test
	public void getConnectionPoolInfoNull() {
		ConnectionFactory qcf = new TestConnectionFactory();
		JmsPoolConnectionFactory pool = new JmsPoolConnectionFactory();
		pool.setConnectionFactory(qcf);

		assertNull(JmsPoolUtil.getConnectionPoolInfo(null));
	}

	@SuppressWarnings("unused") // Used in 'reflectionToString'.
	private static class TestConnectionFactory implements ConnectionFactory, XAConnectionFactory {
		private String privateField = "123";
		private String password = "top-secret";

		@Override
		public Connection createConnection() throws JMSException {
			return createConnection(null, null);
		}

		@Override
		public Connection createConnection(String userName, String password) throws JMSException {
			throw new IllegalAccessError(); // Should be called when using 'reflectionToString'.
		}

		@Override
		public JMSContext createContext() {
			return createContext(null, null);
		}

		@Override
		public JMSContext createContext(String userName, String password) {
			return createContext(userName, password, 0);
		}

		@Override
		public JMSContext createContext(String userName, String password, int sessionMode) {
			throw new IllegalAccessError(); // Should be called when using 'reflectionToString'.
		}

		@Override
		public JMSContext createContext(int sessionMode) {
			return createContext(null, null, sessionMode);
		}

		@Override
		public XAConnection createXAConnection() throws JMSException {
			return createXAConnection(null, null);
		}

		@Override
		public XAConnection createXAConnection(String userName, String password) throws JMSException {
			throw new IllegalAccessError(); // Should be called when using 'reflectionToString'.
		}

		@Override
		public XAJMSContext createXAContext() {
			return createXAContext(null, null);
		}

		@Override
		public XAJMSContext createXAContext(String userName, String password) {
			throw new IllegalAccessError(); // Should be called when using 'reflectionToString'.
		}
	}
}
