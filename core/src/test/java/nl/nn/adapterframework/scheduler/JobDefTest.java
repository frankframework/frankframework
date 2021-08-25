package nl.nn.adapterframework.scheduler;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.JdbcTestBase;
import nl.nn.adapterframework.jdbc.JdbcTransactionalStorage;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.JdbcUtil;

public class JobDefTest extends JdbcTestBase {

	private boolean tableCreated = false;
	private JobDef jobDef;
	private TestConfiguration configuration;
	private final String cleanupJobName="CleanupDB";
	private final String cleanupJobTableName="JOBDEFTEST";

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		if (!dbmsSupport.isTablePresent(connection, cleanupJobTableName)) {
			createJobDefTestTable();
			tableCreated = true;
		}

		configuration = new TestConfiguration();
		Adapter adapter = setupAdapter(); 
		configuration.registerAdapter(adapter);

		jobDef = new JobDef() {

			@Override
			protected List<String> getAllLockerDatasourceNames(IbisManager ibisManager) {
				return Arrays.asList(getDataSourceName());
			}

		};

		configuration.autowireByName(jobDef);
	}

	private Adapter setupAdapter() throws ConfigurationException {
		Adapter adapter = configuration.createBean(Adapter.class);
		adapter.setName("fakeAdapter");
		PipeLine pipeline = configuration.createBean(PipeLine.class);

		JdbcTransactionalStorage storage = configuration.createBean(JdbcTransactionalStorage.class);
		storage.setName("test-cleanupDB");
		storage.setType("A");
		storage.setSlotId("dummySlotId");
		storage.setTableName(cleanupJobTableName);
		storage.setDatasourceName(getDataSourceName());

		MessageSendingPipe pipe = new MessageSendingPipe();
		pipe.setName("dummyPipe");
		pipe.setMessageLog(storage);
		pipeline.addPipe(pipe);

		adapter.setPipeLine(pipeline);
		return adapter;
	}

	private void createJobDefTestTable() throws JdbcException {
		JdbcUtil.executeStatement(connection,
				"CREATE TABLE JOBDEFTEST("
				+ "MESSAGEKEY INT PRIMARY KEY , " 
				+ "TYPE " + dbmsSupport.getTextFieldType() + "(1) NULL, "
				+ "HOST " + dbmsSupport.getTextFieldType() + "(100) NULL, "
				+ "CREATIONDATE " + dbmsSupport.getTimestampFieldType() + " NULL, "
				+ "EXPIRYDATE " + dbmsSupport.getTimestampFieldType() + " NULL)");
	}

	@After
	public void teardown() throws JdbcException {
		if (tableCreated) {
			JdbcUtil.executeStatement(connection, "DROP TABLE "+cleanupJobTableName);
		}
	}

	@Test
	public void testCleanupDatabaseJobMaxRowsZero() throws ConfigurationException, JdbcException, SQLException {
		jobDef.setName(cleanupJobName);
		jobDef.setFunction(JobDefFunctions.CLEANUPDB.getLabel());
		jobDef.configure();
		configuration.registerScheduledJob(jobDef);

		JdbcUtil.executeStatement(connection, "INSERT INTO "+cleanupJobTableName+" (MESSAGEKEY, type, host, creationDate, expiryDate) VALUES (1, 'A', 'localhost', '2021-07-13 11:04:19.860', '2021-07-13 11:04:19.860')");
		// set max rows to 0 
		AppConstants.getInstance().setProperty("cleanup.database.maxrows", "0");
		jobDef.runJob(configuration.getIbisManager());

		int numRows = JdbcUtil.executeIntQuery(getConnection(), "SELECT count(*) from "+cleanupJobTableName);

		assertEquals(0, numRows);
	}
	
	@Test
	public void testCleanupDatabaseJob() throws ConfigurationException, JdbcException, SQLException {
		jobDef.setName(cleanupJobName);
		jobDef.setFunction(JobDefFunctions.CLEANUPDB.getLabel());
		jobDef.configure();
		configuration.registerScheduledJob(jobDef);

		insert5Rows();

		int rowCount = JdbcUtil.executeIntQuery(getConnection(), "SELECT count(*) from "+cleanupJobTableName);
		// check insertion
		assertEquals(5, rowCount);

		jobDef.runJob(configuration.getIbisManager());

		int numRows = JdbcUtil.executeIntQuery(getConnection(), "SELECT count(*) from "+cleanupJobTableName);

		assertEquals(0, numRows);
	}
	
	private void insert5Rows() throws JdbcException {
		JdbcUtil.executeStatement(connection, "INSERT INTO "+cleanupJobTableName+" (MESSAGEKEY, type, host, creationDate, expiryDate) VALUES "
				+ "(1, 'A', 'localhost', '2021-07-13 11:04:19.860', '2021-07-13 11:04:19.860'),"
				+ "(2, 'A', 'localhost', '2021-07-13 11:04:19.860', '2021-07-13 11:04:19.860'),"
				+ "(3, 'A', 'localhost', '2021-07-13 11:04:19.860', '2021-07-13 11:04:19.860'),"
				+ "(4, 'A', 'localhost', '2021-07-13 11:04:19.860', '2021-07-13 11:04:19.860'),"
				+ "(5, 'A', 'localhost', '2021-07-13 11:04:19.860', '2021-07-13 11:04:19.860')");
		
	}

	@Test
	public void testCleanupDatabaseJobMaxRowsOne() throws ConfigurationException, JdbcException, SQLException {
		jobDef.setName(cleanupJobName);
		jobDef.setFunction(JobDefFunctions.CLEANUPDB.getLabel());
		jobDef.configure();
		configuration.registerScheduledJob(jobDef);

		insert5Rows();

		int rowCount = JdbcUtil.executeIntQuery(getConnection(), "SELECT count(*) from "+cleanupJobTableName);
		// check insertion
		assertEquals(5, rowCount);
		// to clean up 1 by 1
		AppConstants.getInstance().setProperty("cleanup.database.maxrows", "1");
		jobDef.runJob(configuration.getIbisManager());

		int numRows = JdbcUtil.executeIntQuery(getConnection(), "SELECT count(*) from "+cleanupJobTableName);

		assertEquals(0, numRows);
	}
}

