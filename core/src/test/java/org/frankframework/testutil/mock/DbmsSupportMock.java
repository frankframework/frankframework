package org.frankframework.testutil.mock;

import java.sql.Connection;

import org.mockito.Mockito;

import org.frankframework.dbms.Dbms;
import org.frankframework.dbms.DbmsException;
import org.frankframework.dbms.GenericDbmsSupport;
import org.frankframework.dbms.IDbmsSupport;

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
	public boolean isTablePresent(Connection conn, String schemaName, String tableName) throws DbmsException {
		return true; //all tables are present
	}
}
