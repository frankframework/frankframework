package org.frankframework.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;

import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLine;
import org.frankframework.dbms.Dbms;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.dbms.JdbcException;
import org.frankframework.jdbc.JdbcTransactionalStorage;
import org.frankframework.pipes.MessageSendingPipe;
import org.frankframework.receivers.Receiver;
import org.frankframework.scheduler.job.CleanupDatabaseJob;
import org.frankframework.scheduler.job.IJob;
import org.frankframework.testutil.JdbcTestUtil;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;
import org.frankframework.util.AppConstants;
import org.frankframework.util.Locker;

@WithLiquibase(tableName = "IBISLOCK") //Lock table must exist
@WithLiquibase(tableName = CleanupDatabaseJobTest.txStorageTableName) //Actual JdbcTXStorage table
public class CleanupDatabaseJobTest {

	private CleanupDatabaseJob jobDef;
	private JdbcTransactionalStorage<Serializable> storage;
	private final String cleanupJobName = "CleanupDB";
	protected static final String txStorageTableName = "NOT_IBISLOCK_TABLE";

	@BeforeEach
	@SuppressWarnings("unchecked")
	public void setUp(DatabaseTestEnvironment database) throws Exception {
		TestConfiguration configuration = database.getConfiguration();
		storage = configuration.createBean(JdbcTransactionalStorage.class);
		storage.setName("test-cleanupDB");
		storage.setType("A");
		storage.setSlotId("dummySlotId");
		storage.setTableName(txStorageTableName);
		storage.setSequenceName("SEQ_"+txStorageTableName);
		storage.setDatasourceName(database.getDataSourceName());

		if (configuration.getScheduledJob("MockJob") == null) {
			IJob mockJob = mock(IJob.class);
			Locker mockLocker = mock(Locker.class);
			when(mockLocker.getDatasourceName()).thenAnswer(invocation -> database.getDataSourceName());
			when(mockJob.getLocker()).thenReturn(mockLocker);
			when(mockJob.getName()).thenReturn("MockJob");

			configuration.getScheduleManager().addScheduledJob(mockJob);
		}

		if (configuration.getRegisteredAdapter("MockAdapter") == null) {
			Adapter mockAdapter = mock(Adapter.class);
			when(mockAdapter.getName()).thenReturn("MockAdapter");

			PipeLine pipeLine = new PipeLine();
			MessageSendingPipe mockPipe = mock(MessageSendingPipe.class);
			Locker mockLocker = mock(Locker.class);
			when(mockLocker.getDatasourceName()).thenAnswer(invocation -> database.getDataSourceName());
			when(mockPipe.getLocker()).thenReturn(mockLocker);
			when(mockPipe.getName()).thenReturn("MockPipe");
			when(mockPipe.getMessageLog()).thenReturn(storage);
			pipeLine.addPipe(mockPipe);
			when(mockAdapter.getPipeLine()).thenReturn(pipeLine);

			Receiver<?> mockReceiver = mock(Receiver.class);
			when(mockReceiver.getMessageLog()).thenReturn(storage);
			when(mockReceiver.getName()).thenReturn("MockReceiver");
			when(mockAdapter.getReceivers()).thenReturn(Collections.singletonList(mockReceiver));
			configuration.addAdapter(mockAdapter);
		}

		// Ensure we have an IbisManager via side effects of method
		//noinspection ResultOfMethodCallIgnored
		configuration.getIbisManager();
		jobDef = configuration.createBean(CleanupDatabaseJob.class);
	}

	@DatabaseTest
	public void testCleanupDatabaseJobMaxRowsZero(DatabaseTestEnvironment database) throws Exception {
		jobDef.setName(cleanupJobName);
		jobDef.configure();
		prepareInsertQuery(database, 1);

		// set max rows to 0
		AppConstants.getInstance().setProperty("cleanup.database.maxrows", "0");

		assertTrue(jobDef.beforeExecuteJob());
		jobDef.execute();

		assertEquals(0, getCount(database));
	}

	@DatabaseTest
	public void testCleanupDatabaseJob(DatabaseTestEnvironment database) throws Exception {
		jobDef.setName(cleanupJobName);
		jobDef.configure();

		prepareInsertQuery(database, 5);

		assertTrue(jobDef.beforeExecuteJob());
		jobDef.execute();

		assertEquals(0, getCount(database));
	}

	private void prepareInsertQuery(DatabaseTestEnvironment database, int numRows) throws Exception {
		IDbmsSupport dbmsSupport = database.getDbmsSupport();
		Date date = new Date();
		Date expiryDate = new Date(date.getTime() - 3600 * 1000 * 24);
		StringBuilder sb = new StringBuilder();
		for(int i = 1; i <= numRows; i++) {
			if(dbmsSupport.getDbms() == Dbms.ORACLE) {
				sb.append("SELECT ")
						.append(i)
						.append(", 'A', 'test', 'localhost', 'messageId', 'correlationId', ")
						.append(dbmsSupport.getDatetimeLiteral(date))
						.append(", 'comments', ")
						.append(dbmsSupport.getDatetimeLiteral(expiryDate))
						.append(", 'label' FROM DUAL");
			} else {
				sb.append("(");
				if(dbmsSupport.autoIncrementKeyMustBeInserted()) {
					sb.append(i).append(",");
				}
				sb.append("'A', 'test', 'localhost', 'messageId', 'correlationId', ").append(dbmsSupport.getDatetimeLiteral(date)).append(", 'comments', ").append(dbmsSupport.getDatetimeLiteral(expiryDate)).append(", 'label')");
			}
			if(i != numRows) {
				if(dbmsSupport.getDbms() == Dbms.ORACLE) {
					sb.append(" UNION ALL \n");
				} else {
					sb.append(",");
				}
			}
		}
		if(dbmsSupport.getDbms() == Dbms.ORACLE) {
			sb.append(") SELECT * FROM valuesTable");
		}

		String query ="INSERT INTO "+txStorageTableName+" (" +
				(dbmsSupport.autoIncrementKeyMustBeInserted() ? storage.getKeyField()+"," : "")
				+ storage.getTypeField() + ","
				+ storage.getSlotIdField() + ","
				+ storage.getHostField() + ","
				+ storage.getIdField() + ","
				+ storage.getCorrelationIdField() + ","
				+ storage.getDateField() + ","
				+ storage.getCommentField() + ","
				+ storage.getExpiryDateField()  +","
				+ storage.getLabelField() + ")" + (dbmsSupport.getDbms() == Dbms.ORACLE ? " WITH valuesTable AS (" : " VALUES ")
				+ sb.toString();

		try(Connection connection = database.getConnection()) {
			JdbcTestUtil.executeStatement(connection, query);
		}

		// check insertion
		assertEquals(numRows, getCount(database));
	}

	private int getCount(DatabaseTestEnvironment database) throws SQLException, JdbcException {
		try(Connection connection = database.getConnection()) {
			return JdbcTestUtil.executeIntQuery(connection, "SELECT count(*) from "+txStorageTableName);
		}
	}

	@DatabaseTest
	public void testCleanupDatabaseJobMaxRowsOne(DatabaseTestEnvironment database) throws Exception {
		jobDef.setName(cleanupJobName);
		jobDef.configure();

		prepareInsertQuery(database, 5);

		// to clean up 1 by 1
		AppConstants.getInstance().setProperty("cleanup.database.maxrows", "1");

		assertTrue(jobDef.beforeExecuteJob());
		jobDef.execute();

		assertEquals(0, getCount(database));
	}
}
