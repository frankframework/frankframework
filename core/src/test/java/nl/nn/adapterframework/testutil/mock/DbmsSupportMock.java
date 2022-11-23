package nl.nn.adapterframework.testutil.mock;

import java.sql.Connection;

import org.mockito.Mockito;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.dbms.Dbms;
import nl.nn.adapterframework.jdbc.dbms.GenericDbmsSupport;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;

public abstract class DbmsSupportMock extends GenericDbmsSupport implements IDbmsSupport {

	@Override
	public Dbms getDbms() {
		return Dbms.NONE;
	}

	@Override
	public String getDbmsName() {
		return "MockDbmsSupport";
	}

	public static DbmsSupportMock newInstance() {
		DbmsSupportMock mock = Mockito.mock(DbmsSupportMock.class, Mockito.CALLS_REAL_METHODS);
		return mock;
	}

	@Override
	public boolean isTablePresent(Connection conn, String schemaName, String tableName) throws JdbcException {
		return true; //all tables are present
	}
}
