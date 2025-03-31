package org.frankframework.jdbc;

import static org.frankframework.testutil.MatchUtils.assertJsonEquals;
import static org.frankframework.testutil.MatchUtils.assertXmlEquals;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.frankframework.core.ConfiguredTestBase;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.dbms.Dbms;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.functional.ThrowingConsumer;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterType;
import org.frankframework.stream.Message;
import org.frankframework.testutil.NumberParameterBuilder;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;

@WithLiquibase(tableName = FixedQuerySenderTest.TABLE_NAME, file = "Migrator/ChangelogBlobTests.xml")
public class FixedQuerySenderTest {

	private FixedQuerySender fixedQuerySender;

	private final String resultColumnsReturned = "<result><rowset><row number=\"0\"><field name=\"TKEY\">1</field><field name=\"TVARCHAR\">value</field></row></rowset></result>";
	protected static final String TABLE_NAME = "FQS_TABLE";

	private PipeLineSession session;

	private Dbms databaseUnderTest;

	@BeforeEach
	public void setup(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		this.databaseUnderTest = databaseTestEnvironment.getDbmsSupport().getDbms();
		TestConfiguration configuration = databaseTestEnvironment.getConfiguration();
		session = new PipeLineSession();
		session.put(PipeLineSession.MESSAGE_ID_KEY, ConfiguredTestBase.testMessageId);
		session.put(PipeLineSession.CORRELATION_ID_KEY, ConfiguredTestBase.testCorrelationId);

		fixedQuerySender = new FixedQuerySender();
		fixedQuerySender.setDatasourceName(databaseTestEnvironment.getDataSourceName());
		fixedQuerySender.setName("FQS_TABLE");
		fixedQuerySender.setIncludeFieldDefinition(false);
		configuration.autowireByName(fixedQuerySender);

		configuration.getIbisManager();
		configuration.autowireByName(fixedQuerySender);
	}

	@AfterEach
	public void teardown() {
		session.close();
	}

	private void assertSenderException(Dbms database, SenderException ex) {
		switch (database) {
			case H2 -> assertThat(ex.getMessage(), containsString("Syntax error in SQL statement"));
			case DB2 -> assertThat(ex.getMessage(), containsString("SQLSTATE=42601"));
			case POSTGRESQL -> assertThat(ex.getMessage(), containsString("No value specified for parameter 1"));
			case ORACLE -> assertThat(ex.getMessage(), containsString("errorCode [17041]"));
			case MSSQL -> assertThat(ex.getMessage(), containsString("The value is not set for the parameter number 1"));
			case MARIADB -> assertThat(ex.getMessage(), containsString(" escape sequence "));
			default -> assertThat(ex.getMessage(), containsString("parameter"));
		}
	}

	private void assertColumnsReturned(Message response) throws Exception {
		String result = response.asString();
		assertEquals(resultColumnsReturned, result);
	}

	@DatabaseTest
	public void testNamedParametersTrue() throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('1', ?{namedParam1})");
		fixedQuerySender.addParameter(new Parameter("namedParam1", "value"));
		fixedQuerySender.setUseNamedParams(true);

		fixedQuerySender.configure();
		fixedQuerySender.start();

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}

	@DatabaseTest
	public void testNamedParameters() throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('1', ?{param})");
		fixedQuerySender.addParameter(new Parameter("param", "value"));

		fixedQuerySender.configure();
		assertTrue(fixedQuerySender.getUseNamedParams());
		fixedQuerySender.start();

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}

	@DatabaseTest
	public void testUseNamedParametersStringValueContains_unp_start() throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('3', '?{param}')");

		fixedQuerySender.configure();
		fixedQuerySender.start();

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());

		fixedQuerySender.setQuery("SELECT tVARCHAR FROM " + TABLE_NAME + " WHERE tKEY='3'");
		fixedQuerySender.setQueryType(AbstractJdbcQuerySender.QueryType.SELECT);
		fixedQuerySender.setScalar(true);

		result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();

		assertEquals("?{param}", result.asString());
	}

	@DatabaseTest
	public void testUseNamedParametersStringValueContains_unp_start_resolveParam() throws Exception {
		assumeFalse(databaseUnderTest == Dbms.MARIADB, "MariaDB JDBC driver appears to allow setting parameters with index higher than parameter count in the prepared statement so the test fails");

		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('1', '?{param}')");

		fixedQuerySender.addParameter(new Parameter("param", "value"));

		fixedQuerySender.configure();
		fixedQuerySender.start();

		SenderException senderException = assertThrows(SenderException.class, () -> fixedQuerySender.sendMessage(new Message("dummy"), session));
		assertThat(senderException.getMessage(), containsString("Could not set parameter [param] with type [STRING] at position 0, exception"));
	}

	@DatabaseTest
	public void testUseNamedParametersWithoutNamedParam() throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('1', 'text')");
		fixedQuerySender.setUseNamedParams(true);
		fixedQuerySender.configure();
		fixedQuerySender.start();

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}

	@DatabaseTest
	public void testUseNamedParametersWithoutParam(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('1', ?{param})");
		fixedQuerySender.setUseNamedParams(true);
		fixedQuerySender.configure();
		fixedQuerySender.start();

		SenderException ex = assertThrows(SenderException.class, () -> fixedQuerySender.sendMessage(new Message("dummy"), session));

		assertSenderException(databaseTestEnvironment.getDbmsSupport().getDbms(), ex);
	}

	@DatabaseTest
	public void testNamedParamInQueryFlagFalse(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('1', ?{param})");
		fixedQuerySender.setUseNamedParams(false);
		fixedQuerySender.configure();
		fixedQuerySender.start();

		SenderException ex = assertThrows(SenderException.class, () -> fixedQuerySender.sendMessage(new Message("dummy"), session));

		assertSenderException(databaseTestEnvironment.getDbmsSupport().getDbms(), ex);
	}

	@DatabaseTest
	public void testIncompleteNamedParamInQuery(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('1', ?{param)");
		fixedQuerySender.configure();
		fixedQuerySender.start();

		SenderException ex = assertThrows(SenderException.class, () -> fixedQuerySender.sendMessage(new Message("dummy"), session));

		assertSenderException(databaseTestEnvironment.getDbmsSupport().getDbms(), ex);
	}

	@DatabaseTest
	public void testMultipleColumnsReturnedWithSpaceBetween() throws Exception {
		assumeTrue(Dbms.H2 == databaseUnderTest || Dbms.ORACLE == databaseUnderTest);
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('1', ?)");
		fixedQuerySender.addParameter(new Parameter("param1", "value"));

		fixedQuerySender.setColumnsReturned("tKEY, tVARCHAR");

		fixedQuerySender.configure();
		fixedQuerySender.start();

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertColumnsReturned(result);
	}

	@DatabaseTest
	public void testMultipleColumnsReturnedWithDoubleSpace() throws Exception {
		assumeTrue(Dbms.H2 == databaseUnderTest || Dbms.ORACLE == databaseUnderTest);
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('1', ?)");
		fixedQuerySender.addParameter(new Parameter("param1", "value"));

		fixedQuerySender.setColumnsReturned("  tKEY,  tVARCHAR  ");

		fixedQuerySender.configure();
		fixedQuerySender.start();

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertColumnsReturned(result);
	}

	@DatabaseTest
	public void testMultipleColumnsReturned() throws Exception {
		assumeTrue(Dbms.H2 == databaseUnderTest || Dbms.ORACLE == databaseUnderTest);
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('1', ?)");
		fixedQuerySender.addParameter(new Parameter("param1", "value"));

		fixedQuerySender.setColumnsReturned("tKEY,tVARCHAR");
		fixedQuerySender.configure();
		fixedQuerySender.start();

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertColumnsReturned(result);
	}

	@DatabaseTest
	public void testAddMonth() throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tDATE) VALUES ('1', ADD_MONTHS(SYSTIMESTAMP,?))");
		fixedQuerySender.addParameter(NumberParameterBuilder.create("param", 7));
		fixedQuerySender.setSqlDialect("Oracle");
		fixedQuerySender.configure();
		fixedQuerySender.start();

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}


	public void testOutputFormat(DocumentFormat outputFormat, boolean includeFieldDefinition, ThrowingConsumer<String, Exception> asserter) throws Exception {
		assumeTrue(Dbms.H2 == databaseUnderTest);
		fixedQuerySender.setQuery("SELECT COUNT(*) as CNT, 'string' as STR, 5 as NUM, null as NULLCOL FROM " + TABLE_NAME + " WHERE 1=0");
		fixedQuerySender.setOutputFormat(outputFormat);
		fixedQuerySender.setIncludeFieldDefinition(includeFieldDefinition);
		fixedQuerySender.setQueryType(AbstractJdbcQuerySender.QueryType.SELECT);
		fixedQuerySender.configure();
		fixedQuerySender.start();

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		asserter.accept(result.asString());
	}

	@DatabaseTest
	public void testOutputFormatDefault() throws Exception {
		String expected =  TestFileUtils.getTestFile("/Jdbc/result-default.xml");
		testOutputFormat(null, true, r-> assertXmlEquals(expected, r));
	}

	@DatabaseTest
	public void testOutputFormatXml() throws Exception {
		String expected =  TestFileUtils.getTestFile("/Jdbc/result-xml.xml");
		testOutputFormat(DocumentFormat.XML, true, r-> assertXmlEquals(expected, r));
	}

	@DatabaseTest
	public void testOutputFormatJson() throws Exception {
		String expected =  TestFileUtils.getTestFile("/Jdbc/result-json.json");
		testOutputFormat(DocumentFormat.JSON, true, r-> assertJsonEquals(expected, r));
	}

	@DatabaseTest
	public void testOutputFormatDefaultNoFieldDefinitions() throws Exception {
		String expected =  TestFileUtils.getTestFile("/Jdbc/result-default-nofielddef.xml");
		testOutputFormat(null, false, r-> assertXmlEquals(expected, r));
	}

	@DatabaseTest
	public void testOutputFormatXmlNoFieldDefinitions() throws Exception {
		String expected =  TestFileUtils.getTestFile("/Jdbc/result-xml-nofielddef.xml");
		testOutputFormat(DocumentFormat.XML, false, r -> assertXmlEquals(expected, r));
	}

	@DatabaseTest
	public void testOutputFormatJsonNoFieldDefinitions() throws Exception {
		String expected =  TestFileUtils.getTestFile("/Jdbc/result-json-nofielddef.json");
		testOutputFormat(DocumentFormat.JSON, false, r-> assertJsonEquals(expected, r));
	}

	public String getLongString(int sizeInK) {
		String block="0123456789ABCDEF".repeat(16);
		return block.repeat(sizeInK);
	}

	@DatabaseTest
	public void testParameterTypeDefault() throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tCLOB) VALUES ('1', ?)");
		fixedQuerySender.addParameter(ParameterBuilder.create().withName("clob").withSessionKey("clob"));
		fixedQuerySender.setSqlDialect("Oracle");
		fixedQuerySender.configure();
		fixedQuerySender.start();

		String block = getLongString(10);

		session.put("clob", new Message(new StringReader(block)));

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}

	@DatabaseTest
	public void testParameterTypeLobStream() throws Exception {
		assumeFalse(Dbms.DB2 == databaseUnderTest, "This test does not work with DB2, same problems as with TestBlobs");

		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tCLOB, tBLOB) VALUES ('1', ?, ?)");
		fixedQuerySender.addParameter(ParameterBuilder.create().withName("clob").withSessionKey("clob").withType(ParameterType.CHARACTER));
		fixedQuerySender.addParameter(ParameterBuilder.create().withName("blob").withSessionKey("blob").withType(ParameterType.BINARY));
		fixedQuerySender.setSqlDialect("Oracle");
		fixedQuerySender.configure();
		fixedQuerySender.start();

		String block = getLongString(10000);

		session.put("clob", new Message(new StringReader(block)));
		session.put("blob", new Message(new ByteArrayInputStream(block.getBytes())));
		session.put("varchar", new Message(new ByteArrayInputStream(block.getBytes())));

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}

	@DatabaseTest
	public void testParameterTypeLobArray() throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tCLOB, tBLOB) VALUES ('1', ?, ?)");
		fixedQuerySender.addParameter(ParameterBuilder.create().withName("clob").withSessionKey("clob").withType(ParameterType.CHARACTER));
		fixedQuerySender.addParameter(ParameterBuilder.create().withName("blob").withSessionKey("blob").withType(ParameterType.BINARY));
		fixedQuerySender.setSqlDialect("Oracle");
		fixedQuerySender.configure();
		fixedQuerySender.start();

		String block = getLongString(1000);

		session.put("clob", new Message(block));
		session.put("blob", new Message(block.getBytes()));
		session.put("varchar", new Message(block.getBytes()));

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}
}
