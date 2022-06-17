package nl.nn.adapterframework.scheduler;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.jdbc.JdbcTestBase;
import nl.nn.adapterframework.jdbc.JdbcTransactionalStorage;
import nl.nn.adapterframework.jdbc.dbms.Dbms;
import nl.nn.adapterframework.scheduler.job.CleanupDatabaseJob;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.JdbcUtil;

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

		jobDef = new CleanupDatabaseJob() {

			@Override
			protected Set<String> getAllLockerDatasourceNames(IbisManager ibisManager) {
				return Collections.singleton(getDataSourceName());
			}

		};

		getConfiguration().getIbisManager(); // force ibisManager bean to be available in applicationContext

		getConfiguration().autowireByName(jobDef);
	}

	@Test
	public void testCleanupDatabaseJobMaxRowsZero() throws Exception {
		jobDef.setName(cleanupJobName);
		jobDef.configure();
		prepareInsertQuery(1);

		// set max rows to 0 
		AppConstants.getInstance().setProperty("cleanup.database.maxrows", "0");

		jobDef.beforeExecuteJob(getConfiguration().getIbisManager());
		jobDef.execute(getConfiguration().getIbisManager());

		assertEquals(0, getCount(tableName));
	}

	@Test
	public void testCleanupDatabaseJob() throws Exception {
		jobDef.setName(cleanupJobName);
		jobDef.configure();

		prepareInsertQuery(5);

		// check insertion
		assertEquals(5, getCount(tableName));

		jobDef.beforeExecuteJob(getConfiguration().getIbisManager());
		jobDef.execute(getConfiguration().getIbisManager());

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

	private int getCount(String tableName) throws Exception {
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

		jobDef.beforeExecuteJob(getConfiguration().getIbisManager());
		jobDef.execute(getConfiguration().getIbisManager());

		int numRows = JdbcUtil.executeIntQuery(getConnection(), "SELECT count(*) from "+tableName);
		
		assertEquals(0, numRows);
	}
}

