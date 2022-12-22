package nl.nn.adapterframework.jdbc;

import static nl.nn.adapterframework.testutil.MatchUtils.assertJsonEquals;
import static nl.nn.adapterframework.testutil.MatchUtils.assertXmlEquals;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeThat;
import static org.junit.Assume.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.StringReader;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.functional.ThrowingConsumer;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.Parameter.ParameterType;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.document.DocumentFormat;
import nl.nn.adapterframework.testutil.ParameterBuilder;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class FixedQuerySenderTest extends JdbcSenderTestBase<FixedQuerySender> {

	private final String resultColumnsReturned = "<result><rowset><row number=\"0\"><field name=\"TKEY\">1</field><field name=\"TVARCHAR\">value</field></row></rowset></result>";

	@Override
	public FixedQuerySender createSender() throws Exception {
		FixedQuerySender sender = new FixedQuerySender();
		sender.setIncludeFieldDefinition(false);
		return sender;
	}

	private void assertSenderException(SenderException ex) {
		if(getDataSourceName().equals("H2")) {
			assertThat(ex.getMessage(), CoreMatchers.containsString("Syntax error in SQL statement"));
		} else if(getDataSourceName().equals("DB2")) {
			assertThat(ex.getMessage(), CoreMatchers.containsString("(SqlSyntaxErrorException) SQLState [42601]"));
		} else if(getDataSourceName().equals("PostgreSQL")) {
			assertThat(ex.getMessage(), CoreMatchers.containsString("No value specified for parameter 1"));
		} else if(getDataSourceName().equals("Oracle")) {
			assertThat(ex.getMessage(), CoreMatchers.containsString("errorCode [17041]"));
		} else if(getDataSourceName().equals("MS_SQL")) {
			assertThat(ex.getMessage(), CoreMatchers.containsString("The value is not set for the parameter number 1"));
		} else {
			assertThat(ex.getMessage(), CoreMatchers.containsString("parameter"));
		}
	}

	private void assertColumnsReturned(Message response) throws Exception {
		String result = response.asString();

		assertEquals(resultColumnsReturned, result);
	}

	@Test
	public void testNamedParametersTrue() throws Exception {
		sender.setQuery("INSERT INTO "+JdbcTestBase.TEST_TABLE+" (tKEY, tVARCHAR) VALUES ('1', ?{namedParam1})");
		sender.addParameter(new Parameter("namedParam1", "value"));
		sender.setUseNamedParams(true);

		sender.configure();
		sender.open();

		Message result = sendMessage("dummy");
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}

	@Test
	public void testNamedParameters() throws Exception {
		sender.setQuery("INSERT INTO "+JdbcTestBase.TEST_TABLE+" (tKEY, tVARCHAR) VALUES ('1', ?{param})");
		sender.addParameter(new Parameter("param", "value"));

		sender.configure();
		sender.open();

		Message result = sendMessage("dummy");
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}

	@Test
	public void testUseNamedParametersStringValueContains_unp_start() throws Exception {
		sender.setQuery("INSERT INTO "+JdbcTestBase.TEST_TABLE+" (tKEY, tVARCHAR) VALUES ('3', '?{param}')");

		sender.configure();
		sender.open();

		Message result = sendMessage("dummy");
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());

		sender.setQuery("SELECT tVARCHAR FROM "+JdbcTestBase.TEST_TABLE+" WHERE tKEY='3'");
		sender.setQueryType("select");
		sender.setScalar(true);

		result = sendMessage("dummy");

		assertEquals("?{param}", result.asString());
	}

	@Test
	public void testUseNamedParametersStringValueContains_unp_start_resolveParam() throws Exception {
		sender.setQuery("INSERT INTO "+JdbcTestBase.TEST_TABLE+" (tKEY, tVARCHAR) VALUES ('1', '?{param}')");

		sender.addParameter(new Parameter("param", "value"));

		sender.configure();
		sender.open();

		assertThrows(SenderException.class, () -> sendMessage("dummy"));
	}

	@Test
	public void testUseNamedParametersWithoutNamedParam() throws Exception {
		sender.setQuery("INSERT INTO "+JdbcTestBase.TEST_TABLE+" (tKEY, tVARCHAR) VALUES ('1', 'text')");
		sender.setUseNamedParams(true);
		sender.configure();
		sender.open();

		Message result = sendMessage("dummy");
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}

	@Test
	public void testUseNamedParametersWithoutParam() throws Exception {
		sender.setQuery("INSERT INTO "+JdbcTestBase.TEST_TABLE+" (tKEY, tVARCHAR) VALUES ('1', ?{param})");
		sender.setUseNamedParams(true);
		sender.configure();
		sender.open();

		SenderException ex = assertThrows(SenderException.class, () -> sendMessage("dummy"));

		assertSenderException(ex);
	}

	@Test
	public void testNamedParamInQueryFlagFalse() throws Exception {
		sender.setQuery("INSERT INTO "+JdbcTestBase.TEST_TABLE+" (tKEY, tVARCHAR) VALUES ('1', ?{param})");
		sender.setUseNamedParams(false);
		sender.configure();
		sender.open();

		SenderException ex = assertThrows(SenderException.class, () -> sendMessage("dummy"));

		assertSenderException(ex);
	}

	@Test
	public void testIncompleteNamedParamInQuery() throws Exception {
		sender.setQuery("INSERT INTO "+JdbcTestBase.TEST_TABLE+" (tKEY, tVARCHAR) VALUES ('1', ?{param)");
		sender.configure();
		sender.open();

		SenderException ex = assertThrows(SenderException.class, () -> sendMessage("dummy") );

		assertSenderException(ex);
	}

	@Test
	public void testMultipleColumnsReturnedWithSpaceBetween() throws Exception {
		assumeThat(productKey, anyOf(is("H2"),is("Oracle")));
		sender.setQuery("INSERT INTO "+JdbcTestBase.TEST_TABLE+" (tKEY, tVARCHAR) VALUES ('1', ?)");
		sender.addParameter(new Parameter("param1", "value"));

		sender.setColumnsReturned("tKEY, tVARCHAR");

		sender.configure();
		sender.open();

		Message result=sendMessage("dummy");
		assertColumnsReturned(result);
	}

	@Test
	public void testMultipleColumnsReturnedWithDoubleSpace() throws Exception {
		assumeThat(productKey, anyOf(is("H2"),is("Oracle")));
		sender.setQuery("INSERT INTO "+JdbcTestBase.TEST_TABLE+" (tKEY, tVARCHAR) VALUES ('1', ?)");
		sender.addParameter(new Parameter("param1", "value"));

		sender.setColumnsReturned("  tKEY,  tVARCHAR  ");

		sender.configure();
		sender.open();

		Message result=sendMessage("dummy");
		assertColumnsReturned(result);
	}

	@Test
	public void testMultipleColumnsReturned() throws Exception {
		assumeThat(productKey, anyOf(is("H2"),is("Oracle")));
		sender.setQuery("INSERT INTO "+JdbcTestBase.TEST_TABLE+" (tKEY, tVARCHAR) VALUES ('1', ?)");
		sender.addParameter(new Parameter("param1", "value"));

		sender.setColumnsReturned("tKEY,tVARCHAR");
		sender.configure();
		sender.open();

		Message result=sendMessage("dummy");
		assertColumnsReturned(result);
	}

	@Test
	public void testAddMonth() throws Exception {
		sender.setQuery("INSERT INTO "+JdbcTestBase.TEST_TABLE+" (tKEY, tDATE) VALUES ('1', ADD_MONTHS(SYSTIMESTAMP,?))");
		sender.addParameter(ParameterBuilder.create("param", "7").withType(ParameterType.INTEGER));
		sender.setSqlDialect("Oracle");
		sender.configure();
		sender.open();

		Message result=sendMessage("dummy");
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}


	public void testOutputFormat(DocumentFormat outputFormat, boolean includeFieldDefinition, ThrowingConsumer<String, Exception> asserter) throws Exception {
		assumeTrue(getDataSourceName().equals("H2"));
		sender.setQuery("SELECT COUNT(*) as CNT, 'string' as STR, 5 as NUM, null as NULLCOL FROM "+JdbcTestBase.TEST_TABLE+" WHERE 1=0");
		sender.setOutputFormat(outputFormat);
		sender.setIncludeFieldDefinition(includeFieldDefinition);
		sender.setQueryType("select");
		sender.configure();
		sender.open();

		Message result = sendMessage("dummy");
		asserter.accept(result.asString());
	}

	@Test
	public void testOutputFormatDefault() throws Exception {
		String expected =  TestFileUtils.getTestFile("/Jdbc/result-default.xml");
		testOutputFormat(null, true, r-> assertXmlEquals(expected, r));
	}

	@Test
	public void testOutputFormatXml() throws Exception {
		String expected =  TestFileUtils.getTestFile("/Jdbc/result-xml.xml");
		testOutputFormat(DocumentFormat.XML, true, r-> assertXmlEquals(expected, r));
	}

	@Test
	public void testOutputFormatJson() throws Exception {
		String expected =  TestFileUtils.getTestFile("/Jdbc/result-json.json");
		testOutputFormat(DocumentFormat.JSON, true, r-> assertJsonEquals(expected, r));
	}

	@Test
	public void testOutputFormatDefaultNoFieldDefinitions() throws Exception {
		String expected =  TestFileUtils.getTestFile("/Jdbc/result-default-nofielddef.xml");
		testOutputFormat(null, false, r-> assertXmlEquals(expected, r));
	}

	@Test
	public void testOutputFormatXmlNoFieldDefinitions() throws Exception {
		String expected =  TestFileUtils.getTestFile("/Jdbc/result-xml-nofielddef.xml");
		testOutputFormat(DocumentFormat.XML, false, r-> assertXmlEquals(expected, r));
	}

	@Test
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

	@Test
	public void testParameterTypeDefault() throws Exception {
		sender.setQuery("INSERT INTO "+JdbcTestBase.TEST_TABLE+" (tKEY, tCLOB) VALUES ('1', ?)");
		sender.addParameter(ParameterBuilder.create().withName("clob").withSessionKey("clob"));
		sender.setSqlDialect("Oracle");
		sender.configure();
		sender.open();

		String block = getLongString(10);

		session.put("clob", new Message(new StringReader(block)));

		Message result=sendMessage("dummy");
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}

	@Test
	public void testParameterTypeLobStream() throws Exception {
		sender.setQuery("INSERT INTO "+JdbcTestBase.TEST_TABLE+" (tKEY, tCLOB, tBLOB) VALUES ('1', ?, ?)");
		sender.addParameter(ParameterBuilder.create().withName("clob").withSessionKey("clob").withType(ParameterType.CHARACTER));
		sender.addParameter(ParameterBuilder.create().withName("blob").withSessionKey("blob").withType(ParameterType.BINARY));
		sender.setSqlDialect("Oracle");
		sender.configure();
		sender.open();

		String block = getLongString(10000);

		session.put("clob", new Message(new StringReader(block)));
		session.put("blob", new Message(new ByteArrayInputStream(block.getBytes())));
		session.put("varchar", new Message(new ByteArrayInputStream(block.getBytes())));

		Message result=sendMessage("dummy");
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}

	@Test
	public void testParameterTypeLobArray() throws Exception {
		sender.setQuery("INSERT INTO "+JdbcTestBase.TEST_TABLE+" (tKEY, tCLOB, tBLOB) VALUES ('1', ?, ?)");
		sender.addParameter(ParameterBuilder.create().withName("clob").withSessionKey("clob").withType(ParameterType.CHARACTER));
		sender.addParameter(ParameterBuilder.create().withName("blob").withSessionKey("blob").withType(ParameterType.BINARY));
		sender.setSqlDialect("Oracle");
		sender.configure();
		sender.open();

		String block = getLongString(1000);

		session.put("clob", new Message(block));
		session.put("blob", new Message(block.getBytes()));
		session.put("varchar", new Message(block.getBytes()));

		Message result=sendMessage("dummy");
		assertEquals("<result><rowsupdated>1</rowsupdated></result>", result.asString());
	}



}
