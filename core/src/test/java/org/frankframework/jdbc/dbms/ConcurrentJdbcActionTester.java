package org.frankframework.jdbc.dbms;

import java.sql.Connection;
import java.sql.SQLException;

import org.frankframework.dbms.DbmsException;
import org.frankframework.functional.ThrowingSupplier;
import org.frankframework.testutil.ConcurrentActionTester;

public class ConcurrentJdbcActionTester extends ConcurrentActionTester {

	private final ThrowingSupplier<Connection, SQLException> connectionSupplier;

	private Connection connection;

	public ConcurrentJdbcActionTester(ThrowingSupplier<Connection,SQLException> connectionSupplier) {
		super();
		this.connectionSupplier=connectionSupplier;
	}

	@Override
	public final void initAction() throws SQLException, DbmsException {
		connection = connectionSupplier.get();
		initAction(connection);
	}

	public void initAction(Connection conn) throws SQLException, DbmsException {
	}

	@Override
	public final void action() throws SQLException {
		action(connection);
	}

	public void action(Connection conn) throws SQLException {
	}


	@Override
	public final void finalizeAction() throws SQLException {
		try {
			finalizeAction(connection);
		} finally {
			if (connection != null) {
				connection.close();
			}
		}
	}

	public void finalizeAction(Connection conn) throws SQLException {
	}
}
