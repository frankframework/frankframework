package nl.nn.adapterframework.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.senders.SenderTestBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.JdbcUtil;
import oracle.jdbc.pool.OracleDataSource;

public class FixedQuerySenderTest extends SenderTestBase<FixedQuerySender> {

	private final String resultColumnsReturned = "<result><fielddefinition><field name=\"TKEY\" type=\"INTEGER\" columnDisplaySize=\"11\" precision=\"10\" scale=\"0\" isCurrency=\"false\""
			+ " columnTypeName=\"INTEGER\" columnClassName=\"java.lang.Integer\"/><field name=\"TVARCHAR\" type=\"VARCHAR\" columnDisplaySize=\"100\" precision=\"100\" scale=\"0\" isCurrency=\"false\" "
			+ "columnTypeName=\"VARCHAR\" columnClassName=\"java.lang.String\"/></fielddefinition><rowset><row number=\"0\"><field name=\"TKEY\">1</field><field name=\"TVARCHAR\">value</field></row></rowset></result>";
	@Override
	public FixedQuerySender createSender() throws Exception {
		return new FixedQuerySender() {
			
			@Override
			public Connection getConnection() throws JdbcException {
				try {
					return getDbConnection();
				} catch (SQLException e) {
					throw new JdbcException(e);
				}
			}

			@Override
			protected DataSource getDatasource() throws JdbcException {
				try {
					return new OracleDataSource(); // just return one, to have one.
				} catch (SQLException e) {
					throw new JdbcException(e);
				} 
			}
		};
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		sender.setDatasourceName("H2");
		sender.setQuery("SELECT * FROM TEMP");

		boolean databaseIsPresent = false;
		try {
			sender.configure();
			sender.open();
			databaseIsPresent = true;
		} catch (Exception e) {
			System.out.println("Unable to connect to database ["+sender.getDatasourceName()+"], skipping test!");
		}
		assumeTrue(databaseIsPresent);

		IDbmsSupport dbmsSupport = sender.getDbmsSupport();
		Connection connection = sender.getConnection();

		try {
			if (dbmsSupport.isTablePresent(connection, "TEMP")) {
				JdbcUtil.executeStatement(connection, "DROP TABLE TEMP");
				log.warn(JdbcUtil.warningsToString(connection.getWarnings()));
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		try {
			JdbcUtil.executeStatement(connection, 
					"CREATE TABLE TEMP(TKEY "+dbmsSupport.getNumericKeyFieldType()+ " PRIMARY KEY, TVARCHAR "+dbmsSupport.getTextFieldType()+"(100), TINT INT, TNUMBER NUMERIC(10,5), " +
					"TDATE DATE, TDATETIME "+dbmsSupport.getTimestampFieldType()+", TBOOLEAN "+dbmsSupport.getBooleanFieldType()+", "+ 
					"TCLOB "+dbmsSupport.getClobFieldType()+", TBLOB "+dbmsSupport.getBlobFieldType()+")");
			log.warn(JdbcUtil.warningsToString(connection.getWarnings()));
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testNamedParameters() throws Exception {
		sender.setQuery("INSERT INTO TEMP (TKEY, TVARCHAR) VALUES (\'1\', ?{namedParam1})");
		Parameter param = new Parameter();
		param.setName("namedParam1");
		param.setValue("value");
		sender.addParameter(param);
		sender.setUseNamedParams(true);
		sender.setQueryType("insert");

		sender.configure();
		sender.open();
		Message prr = sender.sendMessage(new Message(""), session);
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", prr.asString());
		
	}
	
	@Test
	public void testColumnsReturnedWithSpaceBetween() throws Exception {
		sender.setQuery("INSERT INTO TEMP (TKEY, TVARCHAR) VALUES (\'1\', ?)");
		Parameter param = new Parameter();
		param.setName("param1");
		param.setValue("value");
		sender.addParameter(param);

		sender.setColumnsReturned("TKEY, TVARCHAR");
		sender.setQueryType("insert");

		sender.configure();
		sender.open();
		Message prr = sender.sendMessage(new Message(""), session);
		assertEquals(resultColumnsReturned, prr.asString());
	}
	
	@Test
	public void testColumnsReturnedWithDoubleSpace() throws Exception {
		sender.setQuery("INSERT INTO TEMP (  TKEY,  TVARCHAR  ) VALUES (\'1\', ?)");
		Parameter param = new Parameter();
		param.setName("param1");
		param.setValue("value");
		sender.addParameter(param);

		sender.setColumnsReturned("TKEY, TVARCHAR");
		sender.setQueryType("insert");

		sender.configure();
		sender.open();
		Message prr = sender.sendMessage(new Message(""), session);
		assertEquals(resultColumnsReturned, prr.asString());
	}
	
	@Test
	public void testColumnsReturned() throws Exception {
		sender.setQuery("INSERT INTO TEMP (TKEY, TVARCHAR) VALUES (\'1\', ?)");
		Parameter param = new Parameter();
		param.setName("param1");
		param.setValue("value");
		sender.addParameter(param);

		sender.setColumnsReturned("TKEY,TVARCHAR");
		sender.setQueryType("insert");

		sender.configure();
		sender.open();
		Message prr = sender.sendMessage(new Message(""), session);
		assertEquals(resultColumnsReturned, prr.asString());
	}
	
	public Connection getDbConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:h2:mem:test;LOCK_TIMEOUT=1000");
		
	}
	
}