package nl.nn.adapterframework.jdbc;

import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.jdbc.dbms.H2DbmsSupport;
import nl.nn.adapterframework.senders.SenderTestBase;
import nl.nn.adapterframework.util.AppConstants;

public class DirectQuerySenderTest extends SenderTestBase<DirectQuerySender> {
	private static final String H2_CONNECTION_STRING = "jdbc:h2:mem:test";

	private Connection connection;

	@Before
	public void setUp() throws Exception {
		AppConstants.getInstance().setProperty("jdbc.sqlDialect", "Oracle");
		startDatabase();
		super.setUp();
	}

	public void startDatabase() throws SQLException {
		connection = DriverManager.getConnection(H2_CONNECTION_STRING);
		connection.createStatement().execute("CREATE TABLE TEMP(TKEY INT PRIMARY KEY, TVARCHAR VARCHAR(100), TVARCHAR2 VARCHAR(100), TINT INT, TDATETIME DATETIME)");
	}

	@Override
	public DirectQuerySender createSender() throws Exception {
		DirectQuerySender dqs = new DirectQuerySender();
		dqs.setDbmsSupport(new H2DbmsSupport());
		return dqs;
	}

	@Test
	public void testConvertQuery() throws JdbcException, SQLException {
		String query1 = "Set define off;";
		String query2 = "select count(*) from temp;";
		String query3 = "delete from temp where tvarchar='tst';";
		String query4 = "update temp set tvarchar='new' where tvarchar2='old';";
		String query5 = "ooo BEGIN BEGIN ooo; IF (ooo) THEN ooo; END IF; ooo; IF (ooo) THEN ooo; END IF; END;END;";
		String query6 = "alter trigger ooo BEGIN BEGIN ooo; IF (ooo) THEN ooo; END IF; ooo; IF (ooo) THEN ooo; END IF; END;END;";
		String convertedQuery = sender.convertQuery(connection, query1 + query2 + query3 + query4 + query5 + query6 + query2);
		assertEquals(query2 + query3 + query4 + query5 + query2, convertedQuery);
	}
}
