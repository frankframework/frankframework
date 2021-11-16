package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import bitronix.tm.resource.jdbc.PoolingDataSource;
import liquibase.Contexts;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import nl.nn.adapterframework.jdbc.JdbcQuerySenderBase.QueryType;
import nl.nn.adapterframework.jdbc.dbms.DbmsSupportFactory;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.testutil.BTMXADataSourceFactory;
import nl.nn.adapterframework.testutil.URLDataSourceFactory;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.LogUtil;

@RunWith(Parameterized.class)
public abstract class JdbcTestBase {
	private final static String IBISSTORE_CHANGESET_PATH = "Migrator/Ibisstore_4_unittests_changeset.xml";
	protected static Logger log = LogUtil.getLogger(JdbcTestBase.class);

	protected static TransactionManagerType transactionManagerType = TransactionManagerType.BTM;
	
	protected Liquibase liquibase;
	protected static URLDataSourceFactory dataSourceFactory = createDataSourceFactory(transactionManagerType);
	protected boolean testPeekShouldSkipRecordsAlreadyLocked = false;
	protected String productKey = "unknown";

	protected static Connection connection; // only to be used for setup and teardown like actions

	@Parameterized.Parameter(0)
	public DataSource dataSource;
	protected IDbmsSupport dbmsSupport;

	@Parameters(name= "{index}: {0}")
	public static Iterable<DataSource> data() {
		return dataSourceFactory.getAvailableDataSources();
	}

	public enum TransactionManagerType {
		DATASOURCE, BTM, NARAYANA
	}
	
	@Before
	public void setup() throws Exception {
		switch (transactionManagerType) {
			case DATASOURCE:
				Properties dataSourceProperties = ((DriverManagerDataSource)dataSource).getConnectionProperties();
				productKey = dataSourceProperties.getProperty(URLDataSourceFactory.PRODUCT_KEY);
				testPeekShouldSkipRecordsAlreadyLocked = Boolean.parseBoolean(dataSourceProperties.getProperty(URLDataSourceFactory.TEST_PEEK_KEY));
				break;
			case BTM:
				productKey = ((PoolingDataSource)dataSource).getUniqueName();
				break;
			case NARAYANA:
				//return new NarayanaXADataSourceFactory();
				throw new NotImplementedException("Narayana DataSource wrapper not yet implemented");
			default:
				throw new IllegalArgumentException("Don't know how to setup() for transactionManagerType ["+transactionManagerType+"]");
		}

		connection = dataSource.getConnection();

		DbmsSupportFactory factory = new DbmsSupportFactory();
		dbmsSupport = factory.getDbmsSupport(connection);

		prepareDatabase();
	}

	@After
	public void teardown() throws Exception {
		if(liquibase != null) {
			liquibase.dropAll();
		}
		dataSourceFactory.destroy();
	}
	
	public static URLDataSourceFactory createDataSourceFactory(TransactionManagerType transactionManagerType) {
		switch (transactionManagerType) {
		case DATASOURCE:
			return new URLDataSourceFactory();
		case BTM:
			return new BTMXADataSourceFactory();
		case NARAYANA:
			//return new NarayanaXADataSourceFactory();
			throw new NotImplementedException("NarayanaXADataSourceFactory not yet implemented");
		default:
			throw new IllegalArgumentException("Don't know how to create DataSourceFactory for transactionManagerType ["+transactionManagerType+"]");
		}
	}
	
	protected void createDbTable() throws Exception {
		Database db = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(getConnection()));
		liquibase = new Liquibase(IBISSTORE_CHANGESET_PATH, new ClassLoaderResourceAccessor(), db);
		liquibase.update(new Contexts());
	}
	
	protected void prepareDatabase() throws Exception {
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

	public String getDataSourceName() {
		return productKey;
	}

	/**
	 * @return a new Connection each time this method is called
	 */
	public Connection getConnection() throws SQLException {
		return dataSource.getConnection();
	}

	@AfterClass
	public static void stopDatabase() throws Exception {
		if (!connection.isClosed()) {
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
