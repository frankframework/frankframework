package nl.nn.adapterframework.jdbc;

import static org.junit.Assert.assertFalse;

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
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.Getter;
import nl.nn.adapterframework.jdbc.JdbcQuerySenderBase.QueryType;
import nl.nn.adapterframework.jdbc.dbms.DbmsSupportFactory;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.testutil.TransactionManagerType;
import nl.nn.adapterframework.testutil.URLDataSourceFactory;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.LogUtil;


@RunWith(Parameterized.class)
public abstract class JdbcTestBase {
	protected final static String TEST_CHANGESET_PATH = "Migrator/Ibisstore_4_unittests_changeset.xml";
	protected final static String DEFAULT_CHANGESET_PATH = "IAF_Util/IAF_DatabaseChangelog.xml";
	protected static Logger log = LogUtil.getLogger(JdbcTestBase.class);

	protected static String singleDatasource = null; // "MariaDB";  // set to a specific datasource name, to speed up testing

	protected Liquibase liquibase;
	protected boolean testPeekShouldSkipRecordsAlreadyLocked = false;
	protected String productKey = "unknown";
	protected Properties dataSourceInfo;

	/** Only to be used for setup and teardown like actions */
	protected Connection connection;

	@Parameterized.Parameter(0)
	public @Getter TransactionManagerType transactionManagerType;
	@Parameterized.Parameter(1)
	public DataSource dataSource;

	private @Getter DbmsSupportFactory dbmsSupportFactory = new DbmsSupportFactory();
	protected @Getter IDbmsSupport dbmsSupport;

	@Parameters(name= "{0}: {1}")
	public static Collection data() throws NamingException {
		TransactionManagerType type = TransactionManagerType.DATASOURCE;
		List<DataSource> datasources;
		if (StringUtils.isNotEmpty(singleDatasource)) {
			datasources = new ArrayList<>();
			datasources.add(type.getDataSource(singleDatasource));
		} else {
			datasources = type.getAvailableDataSources();
		}
		List<Object[]> matrix = new ArrayList<>();

		for(DataSource ds : datasources) {
			matrix.add(new Object[] {type, ds});
		}

		return matrix;
	}

	@Before
	public void setup() throws Exception {
		String dsInfo = dataSource.toString(); //We can assume a connection has already been made by the URLDataSourceFactory to validate the DataSource/connectivity
		dataSourceInfo = parseDataSourceInfo(dsInfo);
		productKey = dataSourceInfo.getProperty(URLDataSourceFactory.PRODUCT_KEY);
		testPeekShouldSkipRecordsAlreadyLocked = Boolean.parseBoolean(dataSourceInfo.getProperty(URLDataSourceFactory.TEST_PEEK_KEY));

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
				log.warn("Liquibase failed to drop all objects. Trying to rollback the changesets");
				liquibase.rollback(liquibase.getChangeSetStatuses(null).size(), null); 
			}
			liquibase.close();
		}

		if (connection != null && !connection.isClosed()) {
			try {
				IDbmsSupport dbmsSupport = new DbmsSupportFactory().getDbmsSupport(connection);
				if (dbmsSupport.isTablePresent(connection, "TEMP")) {
					connection.createStatement().execute("DROP TABLE TEMP");
				}
			} finally {
				connection.close();
			}
		}
	}

	//IBISSTORE_CHANGESET_PATH
	protected void runMigrator(String changeLogFile) throws Exception {
		Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));
		liquibase = new Liquibase(changeLogFile, new ClassLoaderResourceAccessor(), db);
		liquibase.dropAll();
		liquibase.update(new Contexts());
	}

	public boolean isTablePresent(String tableName) throws Exception {
		try(Connection connection = getConnection()) {
			return dbmsSupport.isTablePresent(connection, tableName);
		}
	}

	public void dropTable(String tableName) throws Exception {
		try(Connection connection = getConnection()) {
			if (dbmsSupport.isTablePresent(connection, tableName)) {
				JdbcUtil.executeStatement(connection, "DROP TABLE "+tableName);
			}
			assertFalse("table ["+tableName+"] should not exist", dbmsSupport.isTablePresent(connection, tableName));
		}
	}

	protected void prepareDatabase() throws Exception {
		connection = getConnection();
		dbmsSupport = dbmsSupportFactory.getDbmsSupport(dataSource);

		try(Connection connection = getConnection()) {
			if (dbmsSupport.isTablePresent(connection, "TEMP")) {
				JdbcUtil.executeStatement(connection, "DROP TABLE TEMP");
				SQLWarning warnings = connection.getWarnings();
				if(warnings != null) {
					log.warn(JdbcUtil.warningsToString(warnings));
				}
			}
			JdbcUtil.executeStatement(connection, 
					"CREATE TABLE TEMP(TKEY "+dbmsSupport.getNumericKeyFieldType()+ " PRIMARY KEY, TVARCHAR "+dbmsSupport.getTextFieldType()+"(100), TINT INT, TNUMBER NUMERIC(10,5), " +
					"TDATE DATE, TDATETIME "+dbmsSupport.getTimestampFieldType()+", TBOOLEAN "+dbmsSupport.getBooleanFieldType()+", "+ 
					"TCLOB "+dbmsSupport.getClobFieldType()+", TBLOB "+dbmsSupport.getBlobFieldType()+")");
			SQLWarning warnings = connection.getWarnings();
			if(warnings != null) {
				log.warn(JdbcUtil.warningsToString(warnings));
			}
		}
	}

	/** Populates all database related fields that are normally wired through Spring */
	protected void autowire(JdbcFacade jdbcFacade) {
		jdbcFacade.setDatasourceName(getDataSourceName());
//		jdbcFacade.setDataSourceFactory(transactionManagerType.getDataSourceFactory());
		jdbcFacade.setDbmsSupportFactory(dbmsSupportFactory);
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
