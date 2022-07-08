package nl.nn.adapterframework.testutil;

import java.sql.Connection;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.dbms.GenericDbmsSupport;

public class DbmsSupportMock extends GenericDbmsSupport {

	private boolean allColumnsPresent;

	public DbmsSupportMock(boolean allColumnsPresent) {
		this.allColumnsPresent = allColumnsPresent;
	}

	@Override
	public boolean isColumnPresent(Connection conn, String tableName, String columnName) throws JdbcException {
		return allColumnsPresent;
	}

	@Override
	public boolean isColumnPresent(Connection conn, String schemaName, String tableName, String columnName) throws JdbcException {
		return allColumnsPresent;
	}

}
