package nl.nn.adapterframework.jdbc;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import nl.nn.adapterframework.jdbc.dbms.JdbcSession;
import nl.nn.adapterframework.util.JdbcUtil;

public class TransactionManagerTest extends TransactionManagerTestBase {

	protected void checkNumberOfLines(int expected) throws JdbcException, SQLException {
		String query = dbmsSupport.prepareQueryTextForNonLockingRead("select count(*) from TEMP where TKEY = 1");
		try (JdbcSession session = dbmsSupport.prepareSessionForNonLockingRead(connection)) {
			int count = JdbcUtil.executeIntQuery(connection, query);
			assertEquals("number of lines in table", expected, count);
		}
	}
	
	@Test
	public void testCommit() throws Exception {
		JdbcUtil.executeStatement(connection, "DELETE FROM TEMP where TKEY=1");

		TransactionStatus txStatus = txManager.getTransaction(getTxDef(TransactionDefinition.PROPAGATION_REQUIRED));
		
		try (Connection txManagedConnection = txManagedDataSource.getConnection()) {
			checkNumberOfLines(0);
			JdbcUtil.executeStatement(txManagedConnection, "INSERT INTO TEMP (tkey) VALUES (1)");
//			checkNumberOfLines(0);			
		}
//		checkNumberOfLines(0);

		txManager.commit(txStatus);
		
		checkNumberOfLines(1);
	}

	@Test
	public void testRollback() throws Exception {
		JdbcUtil.executeStatement(connection, "DELETE FROM TEMP where TKEY=1");

		TransactionStatus txStatus = txManager.getTransaction(getTxDef(TransactionDefinition.PROPAGATION_REQUIRED));
		
		try (Connection txManagedConnection = txManagedDataSource.getConnection()) {
			checkNumberOfLines(0);
			JdbcUtil.executeStatement(txManagedConnection, "INSERT INTO TEMP (tkey) VALUES (1)");
//			checkNumberOfLines(0);			
		}
//		checkNumberOfLines(0);

		txManager.rollback(txStatus);
		
		checkNumberOfLines(0);
	}
}
