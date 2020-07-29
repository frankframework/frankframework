package nl.nn.adapterframework.jdbc.dbms;

import java.sql.SQLException;

import org.junit.Ignore;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.QueryExecutionContext;

public class SqlTranslatorTestOracleToH2 extends OracleToH2TranslatorTestBase {

	@Override
	protected String convertQuery(QueryExecutionContext queryExecutionContext, boolean canModifyQueryExecutionContext) throws JdbcException, SQLException {
		SqlTranslator translator = new SqlTranslator("Oracle", "H2");
		return translator.translate(queryExecutionContext.getQuery());
	}

	@Ignore("too hard for SqlTranslator to create identity column")
	@Override
	public void testIgnoreAlterTableIbisStore() throws JdbcException, SQLException {
		//super.testIgnoreAlterTableIbisStore();
	}
	@Ignore("too hard for SqlTranslator to create identity column")
	@Override
	public void testConvertQueryCreateTableIbisStore() throws JdbcException, SQLException {
		//super.testConvertQueryCreateTableIbisStore();
	}
}
