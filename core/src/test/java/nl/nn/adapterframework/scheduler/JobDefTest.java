package nl.nn.adapterframework.scheduler;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.FileSystemResourceAccessor;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.jdbc.JdbcTestBase;
import nl.nn.adapterframework.jdbc.JdbcTransactionalStorage;
import nl.nn.adapterframework.jdbc.dbms.Dbms;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.JdbcUtil;

public class JobDefTest extends JdbcTestBase {

	private Liquibase liquibase;
	private JobDef jobDef;
	private JdbcTransactionalStorage storage;
	private TestConfiguration configuration;
	private final String cleanupJobName="CleanupDB";
	private final String cleanupJobTableName="Ibisstore_4_JobDefTest";

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		createJobDefTestTable();

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

		storage = configuration.createBean(JdbcTransactionalStorage.class);
		storage.setName("test-cleanupDB");
		storage.setType("A");
		storage.setSlotId("dummySlotId");
		storage.setTableName(cleanupJobTableName);
		storage.setSequenceName("SEQ_Ibisstore_4_JobDefTest");
		storage.setDatasourceName(getDataSourceName());

		MessageSendingPipe pipe = new MessageSendingPipe();
		pipe.setName("dummyPipe");
		pipe.setMessageLog(storage);
		pipeline.addPipe(pipe);

		adapter.setPipeLine(pipeline);
		return adapter;
	}

	private void createJobDefTestTable() throws Exception {
		FileSystemResourceAccessor resourceAccessor = new FileSystemResourceAccessor(TestFileUtils.getTestFileURL("/").getPath());
		String changesetFilePath = TestFileUtils.getTestFileURL("/Migrator/Ibisstore_4_JobDefTest_changeset.xml").getPath();
		liquibase = new Liquibase(changesetFilePath, resourceAccessor, new JdbcConnection(getConnection()));
		liquibase.update(new Contexts());
	}

	@After
	public void teardown() throws Exception {
		liquibase.dropAll();
	}

	@Test
	public void testCleanupDatabaseJobMaxRowsZero() throws Exception {
		jobDef.setName(cleanupJobName);
		jobDef.setFunction(JobDefFunctions.CLEANUPDB.getLabel());
		jobDef.configure();
		configuration.registerScheduledJob(jobDef);
		prepareInsertQuery(1);
		
		// set max rows to 0 
		AppConstants.getInstance().setProperty("cleanup.database.maxrows", "0");
		jobDef.runJob(configuration.getIbisManager());
		
		int numRows = JdbcUtil.executeIntQuery(getConnection(), "SELECT count(*) from "+cleanupJobTableName);
		assertEquals(0, numRows);
	}
	
	@Test
	public void testCleanupDatabaseJob() throws Exception {
		jobDef.setName(cleanupJobName);
		jobDef.setFunction(JobDefFunctions.CLEANUPDB.getLabel());
		jobDef.configure();
		configuration.registerScheduledJob(jobDef);

		prepareInsertQuery(5);

		int rowCount = JdbcUtil.executeIntQuery(getConnection(), "SELECT count(*) from "+cleanupJobTableName);
		// check insertion
		assertEquals(5, rowCount);

		jobDef.runJob(configuration.getIbisManager());
		int numRows = JdbcUtil.executeIntQuery(getConnection(), "SELECT count(*) from "+cleanupJobTableName);
		assertEquals(0, numRows);
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
		
		String query ="INSERT INTO "+cleanupJobTableName+" (" +
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

		JdbcUtil.executeStatement(getConnection(), query);
		
	}

	@Test
	public void testCleanupDatabaseJobMaxRowsOne() throws Exception {
		jobDef.setName(cleanupJobName);
		jobDef.setFunction(JobDefFunctions.CLEANUPDB.getLabel());
		jobDef.configure();
		configuration.registerScheduledJob(jobDef);

		prepareInsertQuery(5);

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

