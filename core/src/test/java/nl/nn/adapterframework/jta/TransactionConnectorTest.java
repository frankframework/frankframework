package nl.nn.adapterframework.jta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionObject;

import bitronix.tm.BitronixTransaction;
import bitronix.tm.internal.XAResourceManager;
import bitronix.tm.utils.Uid;
import nl.nn.adapterframework.jdbc.TransactionManagerTestBase;
import nl.nn.adapterframework.task.TimeoutGuard;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

public class TransactionConnectorTest extends TransactionManagerTestBase {
	protected static Logger log = LogUtil.getLogger(TransactionConnectorTest.class);

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		runQuery("DELETE FROM TEMP WHERE TKEY=999");
		runQuery("INSERT INTO TEMP (TKEY,TINT) VALUES (999, 1)");
	}
	
	@Test
	public void testSimpleTransaction() throws Exception {
		
		TransactionStatus txStatus = startTransaction();

		try {
			runQuery("UPDATE TEMP SET TINT=2 WHERE TKEY=999");
				
		} finally {
			if (txStatus.isRollbackOnly()) {
				txManager.rollback(txStatus);
				assertEquals(1,runSelectQuery("SELECT TINT FROM TEMP WHERE TKEY=999"));
			} else {
				txManager.commit(txStatus);
				assertEquals(2,runSelectQuery("SELECT TINT FROM TEMP WHERE TKEY=999"));
			}
		}
	}

	@Test
	public void testNewTransactionMustLock() throws Exception {
		
		TransactionStatus txStatus = startTransaction();

		try {
			runQuery("UPDATE TEMP SET TINT=2 WHERE TKEY=999");

			TransactionStatus txStatus2 = startTransaction();
			try {
				runQuery("UPDATE TEMP SET TINT=3 WHERE TKEY=999 AND TINT=2");
			} catch (Exception e) {
				log.info("expected exception", e);
			} finally {
				if (txStatus2.isRollbackOnly()) {
					txManager.rollback(txStatus2);
				} else {
					txManager.commit(txStatus2);
				}
			}
		} catch (Exception e) {
			log.info("exception caught", e);
		} finally {
			txManager.commit(txStatus);
		}
		assertEquals(2,runSelectQuery("SELECT TINT FROM TEMP WHERE TKEY=999"));
	}

	
	@Test
	public void testBasicSameThread() throws Exception {
		displayTransaction();		
		TransactionStatus txStatus = startTransaction();
		displayTransaction();		

		try {
			runQuery("UPDATE TEMP SET TINT=2 WHERE TKEY=999");
			displayTransaction();		

			runQuery("UPDATE TEMP SET TINT=3 WHERE TKEY=999 AND TINT=2");
		} finally {
			txManager.commit(txStatus);
		}
		assertEquals(3,runSelectQuery("SELECT TINT FROM TEMP WHERE TKEY=999"));
	}

	@Test
	public void testBasic() throws Exception {
		
		TransactionStatus txStatus = startTransaction();

		// do some action in main thread
		try {
			runQuery("UPDATE TEMP SET TINT=2 WHERE TKEY=999");

			try {
				runInConnectedChildThread("UPDATE TEMP SET TINT=3 WHERE TKEY=999 AND TINT=2");
			} catch (Throwable t) {
				t.printStackTrace();
				fail();
			}
		} finally {
			txManager.commit(txStatus);
		}
		assertEquals(3,runSelectQuery("SELECT TINT FROM TEMP WHERE TKEY=999"));
	}
	
	@Test
	public void testNoOuterTransaction() throws Exception {
		
		// do some action in main thread
		runQuery("UPDATE TEMP SET TINT=2 WHERE TKEY=999");

		try {
			runInConnectedChildThread("UPDATE TEMP SET TINT=3 WHERE TKEY=999 AND TINT=2");
		} catch (Throwable t) {
			t.printStackTrace();
			fail();
		}
		assertEquals(3,runSelectQuery("SELECT TINT FROM TEMP WHERE TKEY=999"));
	}
	@Test
	public void testBasicRollbackInChildThread() throws Exception {
		
		TransactionStatus txStatus = startTransaction();
		// do some action in main thread
		try {
			runQuery("UPDATE TEMP SET TINT=2 WHERE TKEY=999");

			try {
				runInConnectedChildThread("UPDATE TEMP SET TINT=3 WHERE TKEY=999 AND TINT=2");
			} catch (Throwable t) {
				t.printStackTrace();
				fail();
			}
		} finally {
			txManager.commit(txStatus);
		}
		assertEquals(3,runSelectQuery("SELECT TINT FROM TEMP WHERE TKEY=999"));
	}

	private TransactionStatus startTransaction() {
		int txTimeout=5;
		TransactionDefinition txDef = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW,txTimeout);
		return txManager.getTransaction(txDef);
	}
	
	private void runQuery(String query) throws SQLException {
		try (Connection con = getConnection()) {
			try (PreparedStatement stmt = con.prepareStatement(query)) {
				TimeoutGuard guard = new TimeoutGuard(3, "run child thread"){

					@Override
					protected void abort() {
						try {
							log.warn("--> TIMEOUT executing ["+query+"]");
							stmt.cancel();
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}

				};
				try {
					log.debug("runQuery thread ["+Thread.currentThread().getId()+"] query ["+query+"] ");
					stmt.execute();
				} finally {
					if (guard.cancel()) {
						throw new SQLException("Interrupted ["+query+"");
					}
				}
			}
		}
	}
	
	private int runSelectQuery(String query) throws SQLException {
		try (Connection con = getConnection()) {
			try (PreparedStatement stmt = con.prepareStatement(query)) {
				try (ResultSet rs = stmt.executeQuery()) {
					rs.next();
					return rs.getInt(1);
				}
			}
		}
	}
	public void runInConnectedChildThread(String query) throws InterruptedException {
		try (TransactionConnector transactionConnector = TransactionConnector.getInstance(txManager)) {
			Thread thread = new Thread() {
	
				@Override
				public void run() {
					if (transactionConnector!=null) transactionConnector.beginChildThread();
					try {
						runQuery(query);
					} catch (Throwable e) {
						log.warn(ClassUtils.nameOf(e)+": "+e.getMessage());
					} finally {
						if (transactionConnector!=null) transactionConnector.endChildThread();
					}
				}
				
			};
			thread.start();
			thread.join();
		}
	}
	
	public void displayTransaction() throws SystemException, IllegalArgumentException, SecurityException, IllegalAccessException, NoSuchFieldException {
		if (txManager instanceof IThreadConnectableTransactionManager) {
			IThreadConnectableTransactionManager tctm = (IThreadConnectableTransactionManager)txManager;
			Object transaction = tctm.getCurrentTransaction();
			if (transaction instanceof JtaTransactionObject) {
				UserTransaction ut =((JtaTransactionObject)transaction).getUserTransaction();
				System.out.println("-> UserTransaction status: "+ut.getStatus());
			} else {
				return;
			}
			Object resources = tctm.suspendTransaction(transaction);
			tctm.resumeTransaction(transaction, resources);
			System.out.println("-> Transaction: "+ToStringBuilder.reflectionToString(transaction, ToStringStyle.MULTI_LINE_STYLE));
			System.out.println("-> Resources: "+ToStringBuilder.reflectionToString(resources, ToStringStyle.MULTI_LINE_STYLE));
			
			Object wasActive = ClassUtils.getDeclaredFieldValue(resources, "wasActive");
			System.out.println("-> wasActive: "+wasActive);

			Object suspendedResources = ClassUtils.getDeclaredFieldValue(resources, "suspendedResources");
			if (suspendedResources!=null) {
				System.out.println("-> suspendedResources: "+ToStringBuilder.reflectionToString(suspendedResources, ToStringStyle.MULTI_LINE_STYLE));
				
				if (suspendedResources instanceof BitronixTransaction) {
					BitronixTransaction bt = (BitronixTransaction)suspendedResources;
					XAResourceManager rm = bt.getResourceManager();
					System.out.println("-> XAResourceManager: "+ToStringBuilder.reflectionToString(rm, ToStringStyle.MULTI_LINE_STYLE));
					Uid gtrid = rm.getGtrid();
					System.out.println("-> gtrid: "+gtrid);
				}
				
			}
			
//			AbstractPlatformTransactionManager.SuspendedResourcesHolder holder = 
		}
	}
}
