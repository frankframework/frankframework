package nl.nn.adapterframework.jdbc;

import static nl.nn.adapterframework.parameters.Parameter.ParameterType;
import static nl.nn.adapterframework.testutil.MatchUtils.assertJsonEquals;
import static nl.nn.adapterframework.testutil.MatchUtils.assertXmlEquals;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.io.StringReader;

import org.junit.jupiter.api.BeforeEach;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.functional.ThrowingConsumer;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.document.DocumentFormat;
import nl.nn.adapterframework.testutil.ParameterBuilder;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.testutil.TransactionManagerType;
import nl.nn.adapterframework.testutil.junit.DatabaseTest;
import nl.nn.adapterframework.testutil.junit.DatabaseTestEnvironment;
import nl.nn.adapterframework.testutil.junit.WithLiquibase;

@WithLiquibase(tableName = FixedQuerySenderTest.TABLE_NAME, file = "Migrator/JdbcTestBaseQuery.xml")
public class FixedQuerySenderTest {

	private FixedQuerySender fixedQuerySender;
	protected static final String TABLE_NAME = "FQS_TABLE";

	private JdbcTransactionalStorage<Serializable> storage;

	private PipeLineSession session = new PipeLineSession();

	@DatabaseTest.Parameter(0)
	private TransactionManagerType transactionManagerType;

	@DatabaseTest.Parameter(1)
	private String dataSourceName;

	private TestConfiguration getConfiguration() {
		return transactionManagerType.getConfigurationContext(dataSourceName);
	}

	@BeforeEach
	public void setup(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {

		fixedQuerySender = new FixedQuerySender();
		fixedQuerySender.setDatasourceName(dataSourceName);
		fixedQuerySender.setName("FQS_TABLE");
		getConfiguration().autowireByName(fixedQuerySender);

		getConfiguration().getIbisManager();
		getConfiguration().autowireByName(fixedQuerySender);
	}

	private void assertSenderException(String dataSourceName, SenderException ex) {
		switch (dataSourceName) {
			case "H2":
				assertThat(ex.getMessage(), containsString("Syntax error in SQL statement"));
				break;
			case "DB2":
				assertThat(ex.getMessage(), containsString("SQLSTATE=42601"));
				break;
			case "PostgreSQL":
				assertThat(ex.getMessage(), containsString("No value specified for parameter 1"));
				break;
			case "Oracle":
				assertThat(ex.getMessage(), containsString("errorCode [17041]"));
				break;
			case "MS_SQL":
				assertThat(ex.getMessage(), containsString("The value is not set for the parameter number 1"));
				break;
			case "MariaDB":
				assertThat(ex.getMessage(), containsString(" escape sequence "));
				break;
			default:
				assertThat(ex.getMessage(), containsString("parameter"));
				break;
		}
	}

	private void assertColumnsReturned(Message response) throws Exception {
		String result = response.asString();

		String resultColumnsReturned = "<result><fielddefinition><field name=\"TKEY\" type=\"BIGINT\" columnDisplaySize=\"20\" precision=\"64\" scale=\"0\" isCurrency=\"false\" columnTypeName=\"BIGINT\" columnClassName=\"java.lang.Long\"/><field name=\"TVARCHAR\" type=\"VARCHAR\" columnDisplaySize=\"100\" precision=\"100\" scale=\"0\" isCurrency=\"false\" columnTypeName=\"CHARACTER VARYING\" columnClassName=\"java.lang.String\"/></fielddefinition><rowset><row number=\"0\"><field name=\"TKEY\">1</field><field name=\"TVARCHAR\">value</field></row></rowset></result>";
		assertEquals(resultColumnsReturned, result);
	}

	@DatabaseTest
	public void testNamedParametersTrue() throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('1', ?{namedParam1})");
		fixedQuerySender.addParameter(new Parameter("namedParam1", "value"));
		fixedQuerySender.setUseNamedParams(true);

		fixedQuerySender.configure();
		fixedQuerySender.open();

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}

	@DatabaseTest
	public void testNamedParameters() throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('1', ?{param})");
		fixedQuerySender.addParameter(new Parameter("param", "value"));

		fixedQuerySender.configure();
		assertTrue(fixedQuerySender.getUseNamedParams());
		fixedQuerySender.open();

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}

	@DatabaseTest
	public void testUseNamedParametersStringValueContains_unp_start() throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('3', '?{param}')");

		fixedQuerySender.configure();
		fixedQuerySender.open();

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());

		fixedQuerySender.setQuery("SELECT tVARCHAR FROM " + TABLE_NAME + " WHERE tKEY='3'");
		fixedQuerySender.setQueryType("select");
		fixedQuerySender.setScalar(true);

		result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();

		assertEquals("?{param}", result.asString());
	}

	@DatabaseTest
	public void testUseNamedParametersStringValueContains_unp_start_resolveParam() throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('1', '?{param}')");

		fixedQuerySender.addParameter(new Parameter("param", "value"));

		fixedQuerySender.configure();
		fixedQuerySender.open();

		assertThrows(SenderException.class, () -> fixedQuerySender.sendMessage(new Message("dummy"), session));
	}

	@DatabaseTest
	public void testUseNamedParametersWithoutNamedParam() throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('1', 'text')");
		fixedQuerySender.setUseNamedParams(true);
		fixedQuerySender.configure();
		fixedQuerySender.open();

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}

	@DatabaseTest
	public void testUseNamedParametersWithoutParam(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('1', ?{param})");
		fixedQuerySender.setUseNamedParams(true);
		fixedQuerySender.configure();
		fixedQuerySender.open();

		SenderException ex = assertThrows(SenderException.class, () -> fixedQuerySender.sendMessage(new Message("dummy"), session));

		assertSenderException(databaseTestEnvironment.getDataSourceName(), ex);
	}

	@DatabaseTest
	public void testNamedParamInQueryFlagFalse(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('1', ?{param})");
		fixedQuerySender.setUseNamedParams(false);
		fixedQuerySender.configure();
		fixedQuerySender.open();

		SenderException ex = assertThrows(SenderException.class, () -> fixedQuerySender.sendMessage(new Message("dummy"), session));

		assertSenderException(databaseTestEnvironment.getDataSourceName(), ex);
	}

	@DatabaseTest
	public void testIncompleteNamedParamInQuery(DatabaseTestEnvironment databaseTestEnvironment) throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('1', ?{param)");
		fixedQuerySender.configure();
		fixedQuerySender.open();

		SenderException ex = assertThrows(SenderException.class, () -> fixedQuerySender.sendMessage(new Message("dummy"), session));

		assertSenderException(databaseTestEnvironment.getDataSourceName(), ex);
	}

	@DatabaseTest
	public void testMultipleColumnsReturnedWithSpaceBetween() throws Exception {
		assertThat(dataSourceName, anyOf(is("H2"), is("Oracle")));
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('1', ?)");
		fixedQuerySender.addParameter(new Parameter("param1", "value"));

		fixedQuerySender.setColumnsReturned("tKEY, tVARCHAR");

		fixedQuerySender.configure();
		fixedQuerySender.open();

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertColumnsReturned(result);
	}

	@DatabaseTest
	public void testMultipleColumnsReturnedWithDoubleSpace() throws Exception {
		assertThat(dataSourceName, anyOf(is("H2"), is("Oracle")));
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('1', ?)");
		fixedQuerySender.addParameter(new Parameter("param1", "value"));

		fixedQuerySender.setColumnsReturned("  tKEY,  tVARCHAR  ");

		fixedQuerySender.configure();
		fixedQuerySender.open();

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertColumnsReturned(result);
	}

	@DatabaseTest
	public void testMultipleColumnsReturned() throws Exception {
		assertThat(dataSourceName, anyOf(is("H2"), is("Oracle")));
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tVARCHAR) VALUES ('1', ?)");
		fixedQuerySender.addParameter(new Parameter("param1", "value"));

		fixedQuerySender.setColumnsReturned("tKEY,tVARCHAR");
		fixedQuerySender.configure();
		fixedQuerySender.open();

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertColumnsReturned(result);
	}

	@DatabaseTest
	public void testAddMonth() throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tDATE) VALUES ('1', ADD_MONTHS(SYSTIMESTAMP,?))");
		fixedQuerySender.addParameter(ParameterBuilder.create("param", "7").withType(ParameterType.INTEGER));
		fixedQuerySender.setSqlDialect("Oracle");
		fixedQuerySender.configure();
		fixedQuerySender.open();

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}


	public void testOutputFormat(DocumentFormat outputFormat, boolean includeFieldDefinition, ThrowingConsumer<String, Exception> asserter) throws Exception {
		assumeTrue(dataSourceName.equals("H2"));
		fixedQuerySender.setQuery("SELECT COUNT(*) as CNT, 'string' as STR, 5 as NUM, null as NULLCOL FROM " + TABLE_NAME + " WHERE 1=0");
		fixedQuerySender.setOutputFormat(outputFormat);
		fixedQuerySender.setIncludeFieldDefinition(includeFieldDefinition);
		fixedQuerySender.setQueryType("select");
		fixedQuerySender.configure();
		fixedQuerySender.open();

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
		StringBuilder result=new StringBuilder();
		for(int i=0; i<16; i++) {
			result.append("0123456789ABCDEF");
		}
		String block=result.toString();
		for(int i=1; i<sizeInK; i++) {
			result.append(block);
		}
		return result.toString();
	}

	@DatabaseTest
	public void testParameterTypeDefault() throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tCLOB) VALUES ('1', ?)");
		fixedQuerySender.addParameter(ParameterBuilder.create().withName("clob").withSessionKey("clob"));
		fixedQuerySender.setSqlDialect("Oracle");
		fixedQuerySender.configure();
		fixedQuerySender.open();

		String block = getLongString(10);

		session.put("clob", new Message(new StringReader(block)));

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}

	@DatabaseTest
	public void testParameterTypeLobStream() throws Exception {
		fixedQuerySender.setQuery("INSERT INTO " + TABLE_NAME + " (tKEY, tCLOB, tBLOB) VALUES ('1', ?, ?)");
		fixedQuerySender.addParameter(ParameterBuilder.create().withName("clob").withSessionKey("clob").withType(ParameterType.CHARACTER));
		fixedQuerySender.addParameter(ParameterBuilder.create().withName("blob").withSessionKey("blob").withType(ParameterType.BINARY));
		fixedQuerySender.setSqlDialect("Oracle");
		fixedQuerySender.configure();
		fixedQuerySender.open();

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
		fixedQuerySender.open();

		String block = getLongString(1000);

		session.put("clob", new Message(block));
		session.put("blob", new Message(block.getBytes()));
		session.put("varchar", new Message(block.getBytes()));

		Message result = fixedQuerySender.sendMessage(new Message("dummy"), session).getResult();
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}
}
