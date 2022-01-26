package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import nl.nn.adapterframework.jdbc.dbms.DbmsSupportFactory;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.testutil.URLDataSourceFactory;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.LogUtil;

@RunWith(Parameterized.class)
public abstract class JdbcTestBase {
	protected static Logger log = LogUtil.getLogger(JdbcTestBase.class);

	protected static URLDataSourceFactory dataSourceFactory = new URLDataSourceFactory();
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

	@Before
	public void setup() throws Exception {
		if(dataSource instanceof DriverManagerDataSource) {
			Properties dataSourceProperties = ((DriverManagerDataSource)dataSource).getConnectionProperties();
			productKey = dataSourceProperties.getProperty(URLDataSourceFactory.PRODUCT_KEY);
			testPeekShouldSkipRecordsAlreadyLocked = Boolean.parseBoolean(dataSourceProperties.getProperty(URLDataSourceFactory.TEST_PEEK_KEY));
		}

		connection = dataSource.getConnection();

		DbmsSupportFactory factory = new DbmsSupportFactory();
		dbmsSupport = factory.getDbmsSupport(connection);
		try {
			if (dbmsSupport.isTablePresent(connection, "TEMP")) {
				JdbcUtil.executeStatement(connection, "DROP TABLE TEMP");
				SQLWarning warnings = connection.getWarnings();
				if(warnings != null) {
					log.warn(JdbcUtil.warningsToString(warnings));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			JdbcUtil.executeStatement(connection, 
					"CREATE TABLE TEMP(TKEY "+dbmsSupport.getNumericKeyFieldType()+ " PRIMARY KEY, TVARCHAR "+dbmsSupport.getTextFieldType()+"(100), TINT INT, TNUMBER NUMERIC(10,5), " +
					"TDATE DATE, TDATETIME "+dbmsSupport.getTimestampFieldType()+", TBOOLEAN "+dbmsSupport.getBooleanFieldType()+", "+ 
					"TCLOB "+dbmsSupport.getClobFieldType()+", TBLOB "+dbmsSupport.getBlobFieldType()+")");
			SQLWarning warnings = connection.getWarnings();
			if(warnings != null) {
				log.warn(JdbcUtil.warningsToString(warnings));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/** Populates all database related fields that are normally wired through Spring */
	protected void autowire(JdbcFacade jdbcFacade) {
		jdbcFacade.setDatasourceName(getDataSourceName());
		jdbcFacade.setDataSourceFactory(dataSourceFactory);
		jdbcFacade.setDbmsSupportFactory(new DbmsSupportFactory());
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
	public static void stopDatabase() throws SQLException {
		try {
			connection.createStatement().execute("DROP TABLE TEMP");
		} finally {
			connection.close();
		}
	}
	
	protected PreparedStatement executeTranslatedQuery(Connection connection, String query, String queryType) throws JdbcException, SQLException {
		QueryExecutionContext context = new QueryExecutionContext(query, queryType, null);
		dbmsSupport.convertQuery(context, "Oracle");
		log.debug("executing translated query ["+context.getQuery()+"]");
		if (queryType.equals("select")) {
			return  connection.prepareStatement(context.getQuery());
		}
		if (queryType.equals("select for update")) {
			return connection.prepareStatement(context.getQuery(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		}
		JdbcUtil.executeStatement(connection, context.getQuery());
		return null;
	}
	

}
