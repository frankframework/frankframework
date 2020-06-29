package nl.nn.adapterframework.jdbc.dbms;

import java.sql.SQLException;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.QueryExecutionContext;

public class OracleToH2TranslatorDedicatedTest extends OracleToH2TranslatorTest {

	@Override
	protected String convertQuery(QueryExecutionContext queryExecutionContext, boolean canModifyQueryExecutionContext) throws JdbcException, SQLException {
		return OracleToH2Translator.convertQuery(queryExecutionContext, canModifyQueryExecutionContext);
	}

}
