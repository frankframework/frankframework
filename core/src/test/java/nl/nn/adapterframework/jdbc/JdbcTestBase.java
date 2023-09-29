package nl.nn.adapterframework.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.Getter;
import nl.nn.adapterframework.jdbc.JdbcQuerySenderBase.QueryType;
import nl.nn.adapterframework.dbms.IDbmsSupport;
import nl.nn.adapterframework.dbms.IDbmsSupportFactory;
import nl.nn.adapterframework.dbms.JdbcException;
import nl.nn.adapterframework.jndi.TransactionalDbmsSupportAwareDataSourceProxy;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TransactionManagerType;
import nl.nn.adapterframework.testutil.URLDataSourceFactory;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.LogUtil;

@RunWith(Parameterized.class)
public abstract class JdbcTestBase {

	private boolean failed = false;
	@Rule
	public TestWatcher watchman = new TestWatcher() {
		@Override
		protected void failed(Throwable e, Description description) {
			failed = true;
		}
	};

	protected static final String TEST_CHANGESET_PATH = "Migrator/Ibisstore_4_unittests_changeset.xml";
	protected static final String DEFAULT_CHANGESET_PATH = "IAF_Util/IAF_DatabaseChangelog.xml";
	protected static Logger log = LogUtil.getLogger(JdbcTestBase.class);
	private @Getter TestConfiguration configuration;

	public static final String TEST_TABLE = "Temp"; // use mixed case tablename for testing

	protected static String singleDatasource = null;  //null; // "MariaDB";  // set to a specific datasource name, to speed up testing

	private boolean dropAllAfterEachTest = true;
	protected Liquibase liquibase;
	protected boolean testPeekShouldSkipRecordsAlreadyLocked = false;
	protected Properties dataSourceInfo;

	/** NON-Transactional global Connection. Only to be used for set-up and tear-down like actions! */
	protected Connection connection;

	@Parameterized.Parameter(0)
	public @Getter TransactionManagerType transactionManagerType;
	@Parameterized.Parameter(1)
	public String productKey;
	private DataSource dataSource;

	private @Getter IDbmsSupportFactory dbmsSupportFactory;
	protected @Getter IDbmsSupport dbmsSupport;

	@Parameters(name= "{0}: {1}")
	public static Collection data() throws NamingException {
		TransactionManagerType type = TransactionManagerType.DATASOURCE;
		List<String> datasourceNames; //See URLDataSourceFactory.TEST_DATASOURCES
		if (StringUtils.isNotEmpty(singleDatasource)) {
			datasourceNames = new ArrayList<>();
			datasourceNames.add(singleDatasource);
		} else {
			datasourceNames = type.getAvailableDataSources();
		}
		List<Object[]> matrix = new ArrayList<>();

		for(String name : datasourceNames) {
			matrix.add(new Object[] {type, name});
		}

		return matrix;
	}

	@Before
	public void setup() throws Exception {
		dataSource = transactionManagerType.getDataSource(productKey);

		String dsInfo; //We can assume a connection has already been made by the URLDataSourceFactory to validate the DataSource/connectivity
		if(dataSource instanceof TransactionalDbmsSupportAwareDataSourceProxy) {
			dsInfo = ((TransactionalDbmsSupportAwareDataSourceProxy) dataSource).getTargetDataSource().toString();
		} else {
			dsInfo = dataSource.toString();
		}
		dataSourceInfo = parseDataSourceInfo(dsInfo);

		//The datasourceName must be equal to the ProductKey to ensure we're testing the correct datasource
		assertEquals("DataSourceName does not match ProductKey", productKey, dataSourceInfo.getProperty(URLDataSourceFactory.PRODUCT_KEY));

		testPeekShouldSkipRecordsAlreadyLocked = Boolean.parseBoolean(dataSourceInfo.getProperty(URLDataSourceFactory.TEST_PEEK_KEY));
		configuration = transactionManagerType.getConfigurationContext(productKey);
		dbmsSupportFactory = configuration.getBean(IDbmsSupportFactory.class, "dbmsSupportFactory");

		connection = createNonTransactionalConnection();

		prepareDatabase();
	}

	private Properties parseDataSourceInfo(String dsInfo) {
		Properties props = new Properties();
		String[] parts = dsInfo.split("\\] ");
		for (String part : parts) {
			String[] kvPair = part.split(" \\[");
			String key = kvPair[0];
			String value = (kvPair.length == 1) ? "" : kvPair[1];
			if(!props.containsKey(key)) {
				props.put(key, value);
			}
		}
		return props;
	}

	@After
	public void teardown() throws Exception {
		if(liquibase != null) {
			if (dropAllAfterEachTest) {
				try {
					liquibase.dropAll();
				} catch(Exception e) {
					log.warn("Liquibase failed to drop all objects. Trying to rollback the changesets", e);
					liquibase.rollback(liquibase.getChangeSetStatuses(null).size(), null);
				}
			}
			liquibase.close();
			liquibase = null;
		}

		if (connection != null && !connection.isClosed()) {
			try {
				dropTableIfPresent(connection, TEST_TABLE);
			} finally {
				try {
					connection.rollback();
				} catch (Exception e) {
					log.debug("Could not rollback: "+e.getMessage());
				}
				connection.close();
				connection = null;
			}
		}

		if (failed) {
			transactionManagerType.closeConfigurationContext();
			configuration = null;
		}
	}

	protected final Connection createNonTransactionalConnection() throws SQLException {
		Connection connection = getTargetDataSource(dataSource).getConnection();
		connection.setAutoCommit(true); //Ensure this connection is NOT transactional!
		return connection;
	}

	private DataSource getTargetDataSource(DataSource dataSource) {
		if(dataSource instanceof DelegatingDataSource) {
			return getTargetDataSource(((DelegatingDataSource) dataSource).getTargetDataSource());
		}
		return dataSource;
	}

	//IBISSTORE_CHANGESET_PATH
	protected void runMigrator(String changeLogFile) throws Exception {
		Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(createNonTransactionalConnection()));
		liquibase = new Liquibase(changeLogFile, new ClassLoaderResourceAccessor(), db);
		liquibase.forceReleaseLocks();
		StringWriter out = new StringWriter(2048);
		liquibase.reportStatus(true, new Contexts(), out);
		log.info("Liquibase Database: {}, {}", liquibase.getDatabase().getDatabaseProductName(), liquibase.getDatabase().getDatabaseProductVersion());
		log.info("Liquibase Database connection: {}", liquibase.getDatabase());
		log.info("Liquibase changeset status:");
		log.info(out.toString());
		liquibase.update(new Contexts());
	}

	public boolean isTablePresent(String tableName) throws Exception {
		return dbmsSupport.isTablePresent(connection, tableName);
	}

	public void dropTableIfPresent(String tableName) throws Exception {
		try(Connection connection = getConnection()) {
			dropTableIfPresent(connection, tableName);
		}
	}

	public void dropTableIfPresent(Connection connection, String tableName) throws Exception {
		if (connection!=null && !connection.isClosed()) {
			dropTableIfPresent(dbmsSupport, connection, tableName);
		} else {
			log.warn("connection is null or closed, cannot drop table ["+tableName+"]");
		}
	}

	public static void dropTableIfPresent(IDbmsSupport dbmsSupport, Connection connection, String tableName) throws Exception {
		if (dbmsSupport.isTablePresent(connection, tableName)) {
			JdbcUtil.executeStatement(connection, "DROP TABLE "+tableName);
			SQLWarning warnings = connection.getWarnings();
			if(warnings != null) {
				log.warn(JdbcUtil.warningsToString(warnings));
			}
		}
		assertFalse("table ["+tableName+"] should not exist", dbmsSupport.isTablePresent(connection, tableName));
	}

	protected void prepareDatabase() throws Exception {
		dbmsSupport = dbmsSupportFactory.getDbmsSupport(dataSource);

		dropTableIfPresent(connection, TEST_TABLE);
		String query = "CREATE TABLE "+TEST_TABLE+"(tKEY "+dbmsSupport.getNumericKeyFieldType()+ " PRIMARY KEY, tVARCHAR "+dbmsSupport.getTextFieldType()+"(100), tINT INT, tNUMBER NUMERIC(10,5), " +
				"tDATE DATE, tDATETIME "+dbmsSupport.getTimestampFieldType()+", tBOOLEAN "+dbmsSupport.getBooleanFieldType()+", "+
				"tCLOB "+dbmsSupport.getClobFieldType()+", tBLOB "+dbmsSupport.getBlobFieldType()+")";
		if (productKey.equals("DB2")) {
			query = query.replace(" PRIMARY KEY", "");
		}
		JdbcUtil.executeStatement(connection, query);
		if (productKey.equals("DB2")) {
			JdbcUtil.executeStatement(connection,"CREATE INDEX idx1 ON "+TEST_TABLE+"(tKEY)");
		}
		JdbcUtil.executeStatement(connection,"CREATE INDEX idx2 ON "+TEST_TABLE+"(tINT,tDATE)");
		SQLWarning warnings = connection.getWarnings();
		if(warnings != null) {
			log.warn(JdbcUtil.warningsToString(warnings));
		}
	}

	/** Populates all database related fields that are normally wired through Spring */
	protected void autowire(JdbcFacade jdbcFacade) {
		configuration.autowireByName(jdbcFacade);
		jdbcFacade.setDatasourceName(getDataSourceName());
	}

	public String getDataSourceName() {
		return productKey;
	}

	/**
	 * <b>Make sure to close this!</b>
	 * @return a new Connection each time this method is called
	 */
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	protected PreparedStatement executeTranslatedQuery(Connection connection, String query, QueryType queryType) throws JdbcException, SQLException {
		return executeTranslatedQuery(connection, query, queryType, false);
	}

	protected PreparedStatement executeTranslatedQuery(Connection connection, String query, QueryType queryType, boolean selectForUpdate) throws JdbcException, SQLException {
		String translatedQuery = dbmsSupport.convertQuery(query, "Oracle");

		log.debug("executing translated query [{}]", translatedQuery);
		if (queryType==QueryType.SELECT) {
			if(!selectForUpdate) {
				return  connection.prepareStatement(translatedQuery);
			}
			return connection.prepareStatement(translatedQuery, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		}
		JdbcUtil.executeStatement(connection, translatedQuery);
		return null;
	}

}
