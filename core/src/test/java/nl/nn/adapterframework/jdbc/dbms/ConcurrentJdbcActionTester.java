package nl.nn.adapterframework.jdbc.dbms;

import java.sql.Connection;
import java.sql.SQLException;

import nl.nn.adapterframework.testutil.ConcurrentActionTester;

public abstract class ConcurrentJdbcActionTester extends ConcurrentActionTester {

	public interface ConnectionSupplier {
		public Connection get() throws SQLException;
	}
	
	private ConnectionSupplier connectionSupplier;
	
	private Connection connection;
	
	public ConcurrentJdbcActionTester(ConnectionSupplier connectionSupplier) {
		super();
		this.connectionSupplier=connectionSupplier;
	}

	@Override
	public final void initAction() throws Exception {
		connection = connectionSupplier.get();
		initAction(connection);
	}

	public void initAction(Connection conn) throws Exception {
	}


	@Override
	public final void action() throws Exception {
		action(connection);
	}

	public void action(Connection conn) throws Exception {
	}


	@Override
	public final void finalizeAction() throws Exception {
		try {
			finalizeAction(connection);
		} finally {
			if (connection!=null) {
				connection.close();
			}
		}
	}

	public void finalizeAction(Connection conn) throws Exception {
	}
	
}
