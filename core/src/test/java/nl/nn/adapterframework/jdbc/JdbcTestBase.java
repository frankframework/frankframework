package nl.nn.adapterframework.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
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
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupportFactory;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TransactionManagerType;
import nl.nn.adapterframework.testutil.URLDataSourceFactory;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.LogUtil;

@RunWith(Parameterized.class)
public abstract class JdbcTestBase {
	protected static final String TEST_CHANGESET_PATH = "Migrator/Ibisstore_4_unittests_changeset.xml";
	protected static final String DEFAULT_CHANGESET_PATH = "IAF_Util/IAF_DatabaseChangelog.xml";
	protected static Logger log = LogUtil.getLogger(JdbcTestBase.class);
	private @Getter TestConfiguration configuration;

	public static final String TEST_TABLE="Temp"; // use mixed case tablename for testing

	protected static String singleDatasource = null;  //null; // "MariaDB";  // set to a specific datasource name, to speed up testing

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

	private boolean runMigratorOnlyOncePerDatabaseAndChangelog = true;
	private Set<String> migratedDatabaseChangelogFiles = new HashSet<>();

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

		String dsInfo = dataSource.toString(); //We can assume a connection has already been made by the URLDataSourceFactory to validate the DataSource/connectivity
		dataSourceInfo = parseDataSourceInfo(dsInfo);

		//The datasourceName must be equal to the ProductKey to ensure we're testing the correct datasource
		assertEquals("DataSourceName does not match ProductKey", productKey,dataSourceInfo.getProperty(URLDataSourceFactory.PRODUCT_KEY));

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
			try {
				liquibase.dropAll();
			} catch(Exception e) {
				log.warn("Liquibase failed to drop all objects. Trying to rollback the changesets", e);
				liquibase.rollback(liquibase.getChangeSetStatuses(null).size(), null);
			}
			liquibase.close();
			liquibase = null;
		}

		if (connection != null && !connection.isClosed()) {
			try {
				dropTableIfPresent(connection, TEST_TABLE);
			} finally {
				connection.close();
			}
		}
	}

	protected Connection createNonTransactionalConnection() throws SQLException {
		Connection connection = new DelegatingDataSource(dataSource).getConnection();
		connection.setAutoCommit(true); //Ensure this connection is NOT transactional!
		return connection;
	}

	//IBISSTORE_CHANGESET_PATH
	protected void runMigrator(String changeLogFile) throws Exception {
		Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
		if (runMigratorOnlyOncePerDatabaseAndChangelog) {
			String key = getDataSourceName() +"/" + changeLogFile;
			if (migratedDatabaseChangelogFiles.contains(key)) {
				return;
			}
			migratedDatabaseChangelogFiles.add(key);
		}
		liquibase = new Liquibase(changeLogFile, new ClassLoaderResourceAccessor(), db);
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
		JdbcUtil.executeStatement(connection,
				"CREATE TABLE "+TEST_TABLE+"(tKEY "+dbmsSupport.getNumericKeyFieldType()+ " PRIMARY KEY, tVARCHAR "+dbmsSupport.getTextFieldType()+"(100), tINT INT, tNUMBER NUMERIC(10,5), " +
				"tDATE DATE, tDATETIME "+dbmsSupport.getTimestampFieldType()+", tBOOLEAN "+dbmsSupport.getBooleanFieldType()+", "+
				"tCLOB "+dbmsSupport.getClobFieldType()+", tBLOB "+dbmsSupport.getBlobFieldType()+")");
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
		QueryExecutionContext context = new QueryExecutionContext(query, queryType, null);
		dbmsSupport.convertQuery(context, "Oracle");
		log.debug("executing translated query ["+context.getQuery()+"]");
		if (queryType==QueryType.SELECT) {
			if(!selectForUpdate) {
				return  connection.prepareStatement(context.getQuery());
			}
			return connection.prepareStatement(context.getQuery(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		}
		JdbcUtil.executeStatement(connection, context.getQuery());
		return null;
	}

}
