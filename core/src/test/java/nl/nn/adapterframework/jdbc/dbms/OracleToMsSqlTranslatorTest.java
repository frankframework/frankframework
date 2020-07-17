package nl.nn.adapterframework.jdbc.dbms;

import java.sql.SQLException;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.QueryExecutionContext;

public class OracleToMsSqlTranslatorTest extends OracleToMsSqlTranslatorTestBase {

	@Override
	protected String convertQuery(QueryExecutionContext queryExecutionContext, boolean canModifyQueryExecutionContext) throws JdbcException, SQLException {
		return OracleToMSSQLTranslator.convertQuery(queryExecutionContext, canModifyQueryExecutionContext);
	}

}
