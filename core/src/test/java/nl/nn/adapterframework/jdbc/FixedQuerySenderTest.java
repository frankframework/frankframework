package nl.nn.adapterframework.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.sql.Connection;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.senders.SenderTestBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.JdbcUtil;

public class FixedQuerySenderTest extends SenderTestBase<FixedQuerySender> {

	@Override
	public FixedQuerySender createSender() throws Exception {
		return new FixedQuerySender();
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();

		sender.setDatasourceName("Oracle");
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

		Message result = sendMessage("dummy");
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}
}
