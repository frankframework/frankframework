package nl.nn.adapterframework.jdbc.dbms;

import java.sql.Connection;
import java.sql.SQLException;

import nl.nn.adapterframework.dbms.DbmsException;
import nl.nn.adapterframework.functional.ThrowingSupplier;
import nl.nn.adapterframework.testutil.ConcurrentActionTester;

public abstract class ConcurrentJdbcActionTester extends ConcurrentActionTester {

	private ThrowingSupplier<Connection,SQLException> connectionSupplier;

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
			if (connection!=null) {
				connection.close();
			}
		}
	}

	public void finalizeAction(Connection conn) throws SQLException {
	}

}
