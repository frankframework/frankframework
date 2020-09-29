package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.jdbc.dbms.DbmsSupportFactory;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.LogUtil;

@RunWith(Parameterized.class)
public abstract class JdbcTestBase {
	protected static Logger log = LogUtil.getLogger(JdbcTestBase.class);

	protected String productKey;
	protected String url;
	protected String userid;
	protected String password;
	protected boolean testPeekShouldSkipRecordsAlreadyLocked; // Avoid 'Peek should skip records already locked'-error. if it doesn't, it is not really a problem: Peeking is then only effective when the listener is idle
	
	
	protected static Connection connection;
	protected IDbmsSupport dbmsSupport;

	
	@Parameters(name= "{index}: {0}")
	public static Iterable<Object[]> data() {
		Object[][] datasources = {
			{ "H2",         "jdbc:h2:mem:test;LOCK_TIMEOUT=10", null, null, false },
			{ "Oracle",     "jdbc:oracle:thin:@localhost:1521:ORCLCDB", 			"testiaf_user", "testiaf_user00", false }, 
			{ "MS_SQL",     "jdbc:sqlserver://localhost:1433;database=testiaf", 	"testiaf_user", "testiaf_user00", false }, 
			{ "MySQL",      "jdbc:mysql://localhost:3307/testiaf?sslMode=DISABLED&disableMariaDbDriver", "testiaf_user", "testiaf_user00", true }, 
			{ "MariaDB",    "jdbc:mariadb://localhost:3306/testiaf", 				"testiaf_user", "testiaf_user00", false }, 
			{ "MariaDB",    "jdbc:mysql://localhost:3306/testiaf?sslMode=DISABLED&disableMariaDbDriver", "testiaf_user", "testiaf_user00", false }, 
			{ "PostgreSQL", "jdbc:postgresql://localhost:5432/testiaf", 			"testiaf_user", "testiaf_user00", true }
		};
		List<Object[]> availableDatasources = new ArrayList<>();
		for (Object[] datasource:datasources) {
			String product = (String)datasource[0];
			String url = (String)datasource[1];
			String userId = (String)datasource[2];
			String password = (String)datasource[3];
			try (Connection connection=getConnection(url, userId, password)) {
				availableDatasources.add(datasource);
			} catch (Exception e) {
				log.warn("Cannot connect to ["+url+"], skipping DbmsSupportTest for ["+product+"]:"+e.getMessage());
			}
		}
		return availableDatasources;
	}


	public JdbcTestBase(String productKey, String url, String userid, String password, boolean testPeekDoesntFindRecordsAlreadyLocked) throws SQLException {
		this.productKey = productKey;
		this.url = url;
		this.userid = userid;
		this.password = password;
		this.testPeekShouldSkipRecordsAlreadyLocked = testPeekDoesntFindRecordsAlreadyLocked;

		connection = getConnection();
		DbmsSupportFactory factory = new DbmsSupportFactory();
		dbmsSupport = factory.getDbmsSupport(connection);
		try {
			if (dbmsSupport.isTablePresent(connection, "TEMP")) {
				JdbcUtil.executeStatement(connection, "DROP TABLE TEMP");
				log.warn(JdbcUtil.warningsToString(connection.getWarnings()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			JdbcUtil.executeStatement(connection, 
					"CREATE TABLE TEMP(TKEY "+dbmsSupport.getNumericKeyFieldType()+ " PRIMARY KEY, TVARCHAR "+dbmsSupport.getTextFieldType()+"(100), TINT INT, TNUMBER NUMERIC(10,5), " +
					"TDATE DATE, TDATETIME "+dbmsSupport.getTimestampFieldType()+", TBOOLEAN "+dbmsSupport.getBooleanFieldType()+", "+ 
					"TCLOB "+dbmsSupport.getClobFieldType()+", TBLOB "+dbmsSupport.getBlobFieldType()+")");
			log.warn(JdbcUtil.warningsToString(connection.getWarnings()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static Connection getConnection(String url, String userId, String password) throws SQLException {
		DriverManager.setLoginTimeout(1);
		Connection connection;
		if (userId==null && password==null) {
			connection = DriverManager.getConnection(url);
		} else {
			connection = DriverManager.getConnection(url, userId, password);
		}
		return connection;
	}
	
	public Connection getConnection() throws SQLException {
		return getConnection(url, userid, password);
	}

	@AfterClass
	public static void stopDatabase() throws SQLException {
		try  {
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
