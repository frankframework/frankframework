package nl.nn.adapterframework.jdbc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;

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
		} else if(getDataSourceName().equals("PostgreSQL")) {
			assertThat(ex.getMessage(), CoreMatchers.containsString("No value specified for parameter 1"));
		} else if(getDataSourceName().equals("Oracle")) {
			assertThat(ex.getMessage(), CoreMatchers.containsString("Missing IN or OUT parameter at index:: 1"));
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
	public void testColumnsReturnedWithSpaceBetween() throws Exception {
		sender.setQuery("INSERT INTO "+JdbcTestBase.TEST_TABLE+" (tKEY, tVARCHAR) VALUES ('1', ?)");
		sender.addParameter(new Parameter("param1", "value"));

		sender.setColumnsReturned("tKEY, tVARCHAR");

		sender.configure();
		sender.open();

		Message result=sendMessage("dummy");
		assertColumnsReturned(result);
	}

	@Test
	public void testColumnsReturnedWithDoubleSpace() throws Exception {
		sender.setQuery("INSERT INTO "+JdbcTestBase.TEST_TABLE+" (tKEY, tVARCHAR) VALUES ('1', ?)");
		sender.addParameter(new Parameter("param1", "value"));

		sender.setColumnsReturned("  tKEY,  tVARCHAR  ");

		sender.configure();
		sender.open();

		Message result=sendMessage("dummy");
		assertColumnsReturned(result);
	}

	@Test
	public void testColumnsReturned() throws Exception {
		sender.setQuery("INSERT INTO "+JdbcTestBase.TEST_TABLE+" (tKEY, tVARCHAR) VALUES ('1', ?)");
		sender.addParameter(new Parameter("param1", "value"));

		sender.setColumnsReturned("tKEY,tVARCHAR");

		sender.configure();
		sender.open();

		Message result=sendMessage("dummy");
		assertColumnsReturned(result);
	}

}
