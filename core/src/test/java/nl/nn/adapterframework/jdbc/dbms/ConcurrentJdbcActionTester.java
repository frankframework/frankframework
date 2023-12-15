package nl.nn.adapterframework.jdbc.dbms;

import java.sql.Connection;
import java.sql.SQLException;

import nl.nn.adapterframework.dbms.DbmsException;
import nl.nn.adapterframework.functional.ThrowingSupplier;
import nl.nn.adapterframework.testutil.ConcurrentActionTester;
import nl.nn.adapterframework.testutil.junit.DatabaseTestEnvironment;

public abstract class ConcurrentJdbcActionTester extends ConcurrentActionTester {

	private final ThrowingSupplier<Connection, SQLException> connectionSupplier;

	private DatabaseTestEnvironment databaseTestEnvironment;

	public ConcurrentJdbcActionTester(ThrowingSupplier<Connection,SQLException> connectionSupplier) {
		super();
		this.connectionSupplier=connectionSupplier;
	}

	@Override
	public final void initAction() throws SQLException, DbmsException {
		initAction(databaseTestEnvironment);
	}

	public void initAction(DatabaseTestEnvironment databaseTestEnvironment) throws SQLException, DbmsException {
	}


	@Override
	public final void action() throws SQLException {
		action(databaseTestEnvironment);
	}

	public void action(DatabaseTestEnvironment databaseTestEnvironment) throws SQLException {
	}


	@Override
	public final void finalizeAction() throws SQLException {
		try {
			finalizeAction(databaseTestEnvironment);
		} finally {
			if (databaseTestEnvironment.getConnection() != null) {
				databaseTestEnvironment.getConnection().close();
			}
		}
	}

	public void finalizeAction(DatabaseTestEnvironment databaseTestEnvironment) throws SQLException {
	}

}
