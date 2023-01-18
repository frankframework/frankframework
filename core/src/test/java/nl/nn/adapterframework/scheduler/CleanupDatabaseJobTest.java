package nl.nn.adapterframework.scheduler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.scheduler.job.IJob;
import nl.nn.adapterframework.util.Locker;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.JdbcTestBase;
import nl.nn.adapterframework.jdbc.JdbcTransactionalStorage;
import nl.nn.adapterframework.jdbc.dbms.Dbms;
import nl.nn.adapterframework.scheduler.job.CleanupDatabaseJob;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.JdbcUtil;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class CleanupDatabaseJobTest extends JdbcTestBase {

	private CleanupDatabaseJob jobDef;
	private JdbcTransactionalStorage<?> storage;
	private final String cleanupJobName="CleanupDB";
	private final String tableName="IBISLOCK";

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		System.setProperty("tableName", tableName);
		runMigrator(TEST_CHANGESET_PATH);

		storage = getConfiguration().createBean(JdbcTransactionalStorage.class);
		storage.setName("test-cleanupDB");
		storage.setType("A");
		storage.setSlotId("dummySlotId");
		storage.setTableName(tableName);
		storage.setSequenceName("SEQ_"+tableName);
		storage.setDatasourceName(getDataSourceName());

		if (getConfiguration().getScheduledJob("MockJob") == null) {
			IJob mockJob = mock(IJob.class);
			Locker mockLocker = mock(Locker.class);
			when(mockLocker.getDatasourceName()).thenAnswer(invocation -> getDataSourceName());
			when(mockJob.getLocker()).thenReturn(mockLocker);
			when(mockJob.getName()).thenReturn("MockJob");

			getConfiguration().getScheduleManager().registerScheduledJob(mockJob);
		}

		if (getConfiguration().getRegisteredAdapter("MockAdapter") == null) {
			Adapter mockAdapter = mock(Adapter.class);
			when(mockAdapter.getName()).thenReturn("MockAdapter");

			PipeLine pipeLine = new PipeLine();
			IExtendedPipe mockPipe = mock(IExtendedPipe.class);
			Locker mockLocker = mock(Locker.class);
			when(mockLocker.getDatasourceName()).thenAnswer(invocation -> getDataSourceName());
			when(mockPipe.getLocker()).thenReturn(mockLocker);
			when(mockPipe.getName()).thenReturn("MockPipe");
			pipeLine.addPipe(mockPipe);

			when(mockAdapter.getPipeLine()).thenReturn(pipeLine);

			getConfiguration().registerAdapter(mockAdapter);
		}

		// Ensure we have an IbisManager via side effects of method
		//noinspection ResultOfMethodCallIgnored
		getConfiguration().getIbisManager();

		jobDef = new CleanupDatabaseJob() {

			@Override
			protected List<MessageLogObject> getAllMessageLogs() {
				List<MessageLogObject> mlo = new ArrayList<>();
				String datasourceName = storage.getDatasourceName();
				String expiryDateField = storage.getExpiryDateField();
				String tableName = storage.getTableName();
				String keyField = storage.getKeyField();
				String typeField = storage.getTypeField();
				mlo.add(new MessageLogObject(datasourceName, tableName, expiryDateField, keyField, typeField));
				return mlo;
			}

		};

		getConfiguration().autowireByName(jobDef);
	}

	@Test
	public void testCleanupDatabaseJobMaxRowsZero() throws Exception {
		jobDef.setName(cleanupJobName);
		jobDef.configure();
		prepareInsertQuery(1);

		// set max rows to 0
		AppConstants.getInstance().setProperty("cleanup.database.maxrows", "0");

		jobDef.beforeExecuteJob();
		jobDef.execute();

		assertEquals(0, getCount(tableName));
	}

	@Test
	public void testCleanupDatabaseJob() throws Exception {
		jobDef.setName(cleanupJobName);
		jobDef.configure();

		prepareInsertQuery(5);

		// check insertion
		assertEquals(5, getCount(tableName));

		jobDef.beforeExecuteJob();
		jobDef.execute();

		assertEquals(0, getCount(tableName));
	}

	private void prepareInsertQuery(int numRows) throws Exception {
		Date date = new Date();
		Date expiryDate = new Date(date.getTime() - 3600 * 1000 * 24);
		StringBuilder sb = new StringBuilder("");
		for(int i = 1; i <= numRows; i++) {
			if(dbmsSupport.getDbms() == Dbms.ORACLE) {
				sb.append("SELECT "+i+", 'A', 'test', 'localhost', 'messageId', 'correlationId', "+dbmsSupport.getDatetimeLiteral(date)+", 'comments', "+dbmsSupport.getDatetimeLiteral(expiryDate)+", 'label' FROM DUAL");
			} else {
				sb.append("(");
				if(dbmsSupport.autoIncrementKeyMustBeInserted()) {
					sb.append(i+",");
				}
				sb.append("'A', 'test', 'localhost', 'messageId', 'correlationId', "+dbmsSupport.getDatetimeLiteral(date)+", 'comments', "+dbmsSupport.getDatetimeLiteral(expiryDate)+", 'label')");
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

		String query ="INSERT INTO "+tableName+" (" +
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

		try(Connection connection = getConnection()) {
			JdbcUtil.executeStatement(connection, query);
		}
	}

	private int getCount(String tableName) throws SQLException, JdbcException {
		try(Connection connection = getConnection()) {
			return JdbcUtil.executeIntQuery(connection, "SELECT count(*) from "+tableName);
		}
	}

	@Test
	public void testCleanupDatabaseJobMaxRowsOne() throws Exception {
		jobDef.setName(cleanupJobName);
		jobDef.configure();

		prepareInsertQuery(5);

		int rowCount = getCount(tableName);

		// check insertion
		assertEquals(5, rowCount);
		// to clean up 1 by 1
		AppConstants.getInstance().setProperty("cleanup.database.maxrows", "1");

		jobDef.beforeExecuteJob();
		jobDef.execute();

		int numRows = getCount(tableName);

		assertEquals(0, numRows);
	}
}

