package nl.nn.adapterframework.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.sql.Connection;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.senders.SenderTestBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.JdbcUtil;

public class FixedQuerySenderTest extends SenderTestBase<FixedQuerySender> {

	private final String resultColumnsReturned = "<result><fielddefinition>"
			+ "<field name=\"TKEY\" type=\"INTEGER\" columnDisplaySize=\"11\" precision=\"32\" scale=\"0\" isCurrency=\"false\" columnTypeName=\"INTEGER\" columnClassName=\"java.lang.Integer\"/>"
			+ "<field name=\"TVARCHAR\" type=\"VARCHAR\" columnDisplaySize=\"100\" precision=\"100\" scale=\"0\" isCurrency=\"false\" columnTypeName=\"CHARACTER VARYING\" columnClassName=\"java.lang.String\"/></fielddefinition>"
			+ "<rowset><row number=\"0\"><"
			+ "field name=\"TKEY\">1</field>"
			+ "<field name=\"TVARCHAR\">value</field></row></rowset></result>";

	@Override
	public FixedQuerySender createSender() throws Exception {
		return new FixedQuerySender();
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
	public void testNamedParametersTrue() throws Exception {
		sender.setQuery("INSERT INTO TEMP (TKEY, TVARCHAR) VALUES ('1', ?{namedParam1})");
		Parameter param = new Parameter();
		param.setName("namedParam1");
		param.setValue("value");
		sender.addParameter(param);
		sender.setUseNamedParams(true);

		sender.configure();
		sender.open();

		Message result = sendMessage("dummy");
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}

	@Test
	public void testNamedParameters() throws Exception {
		sender.setQuery("INSERT INTO TEMP (TKEY, TVARCHAR) VALUES ('1', ?{param})");
		Parameter param = new Parameter();
		param.setName("param");
		param.setValue("value");
		sender.addParameter(param);

		sender.configure();
		sender.open();

		Message result = sendMessage("dummy");
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}

	@Test
	public void testUseNamedParametersStringValueContains_unp_start() throws Exception {
		sender.setQuery("INSERT INTO TEMP (TKEY, TVARCHAR) VALUES ('3', '?{param}')");

		sender.configure();
		sender.open();

		Message result = sendMessage("dummy");
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
		
		sender.setQuery("SELECT TVARCHAR FROM TEMP WHERE TKEY='3'");
		sender.setQueryType("select");
		sender.setScalar(true);

		result = sendMessage("dummy");

		assertEquals("?{param}", result.asString());
	}

	@Test
	public void testUseNamedParametersStringValueContains_unp_start_resolveParam() throws Exception {
		exception.expect(SenderException.class);

		sender.setQuery("INSERT INTO TEMP (TKEY, TVARCHAR) VALUES ('1', '?{param}')");

		Parameter param = new Parameter();
		param.setName("param");
		param.setValue("value");
		sender.addParameter(param);

		sender.configure();
		sender.open();

		sendMessage("dummy");
	}
	
	@Test
	public void testUseNamedParametersWithoutNamedParam() throws Exception {
		sender.setQuery("INSERT INTO TEMP (TKEY, TVARCHAR) VALUES ('1', 'text')");
		sender.setUseNamedParams(true);
		sender.configure();
		sender.open();

		Message result = sendMessage("dummy");
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}

	@Test
	public void testUseNamedParametersWithoutParam() throws Exception {
		exception.expect(SenderException.class);
		exception.expectMessage("Syntax error in SQL statement \"INSERT INTO TEMP (TKEY, TVARCHAR) VALUES ('1', ?{param})[*]\"");

		sender.setQuery("INSERT INTO TEMP (TKEY, TVARCHAR) VALUES ('1', ?{param})");
		sender.setUseNamedParams(true);
		sender.configure();
		sender.open();

		sendMessage("dummy");
	}
	
	@Test
	public void testNamedParamInQueryFlagFalse() throws Exception {
		exception.expect(SenderException.class);
		exception.expectMessage("Syntax error in SQL statement \"INSERT INTO TEMP (TKEY, TVARCHAR) VALUES ('1', ?{param})[*]\"");

		sender.setQuery("INSERT INTO TEMP (TKEY, TVARCHAR) VALUES ('1', ?{param})");
		sender.setUseNamedParams(false);
		sender.configure();
		sender.open();

		sendMessage("dummy");
	}

	@Test
	public void testInCompleteNamedParamInQuery() throws Exception {
		exception.expect(SenderException.class);
		exception.expectMessage("Syntax error in SQL statement \"INSERT INTO TEMP (TKEY, TVARCHAR) VALUES ('1', ?{param)[*]\"");

		sender.setQuery("INSERT INTO TEMP (TKEY, TVARCHAR) VALUES ('1', ?{param)");
		sender.configure();
		sender.open();

		sendMessage("dummy");
	}
	@Test
	public void testColumnsReturnedWithSpaceBetween() throws Exception {
		sender.setQuery("INSERT INTO TEMP (TKEY, TVARCHAR) VALUES ('1', ?)");
		Parameter param = new Parameter();
		param.setName("param1");
		param.setValue("value");
		sender.addParameter(param);

		sender.setColumnsReturned("TKEY, TVARCHAR");

		sender.configure();
		sender.open();
		Message result=sendMessage("dummy");
		assertEquals(resultColumnsReturned, result.asString());
	}
	
	@Test
	public void testColumnsReturnedWithDoubleSpace() throws Exception {
		sender.setQuery("INSERT INTO TEMP (TKEY, TVARCHAR) VALUES ('1', ?)");
		Parameter param = new Parameter();
		param.setName("param1");
		param.setValue("value");
		sender.addParameter(param);

		sender.setColumnsReturned("  TKEY,  TVARCHAR  ");

		sender.configure();
		sender.open();
		Message result=sendMessage("dummy");
		assertEquals(resultColumnsReturned, result.asString());
	}
	
	@Test
	public void testColumnsReturned() throws Exception {
		sender.setQuery("INSERT INTO TEMP (TKEY, TVARCHAR) VALUES ('1', ?)");
		Parameter param = new Parameter();
		param.setName("param1");
		param.setValue("value");
		sender.addParameter(param);

		sender.setColumnsReturned("TKEY,TVARCHAR");

		sender.configure();
		sender.open();
		Message result=sendMessage("dummy");
		assertEquals(resultColumnsReturned, result.asString());
	}

}
