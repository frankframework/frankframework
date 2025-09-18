package org.frankframework.jdbc.datasource;

import static org.mockito.Mockito.mock;

import java.io.PrintWriter;
import java.sql.Connection;
import java.util.logging.Logger;

import javax.sql.DataSource;

import lombok.Getter;
import lombok.Setter;

public class MockDataSource implements DataSource {

	@Getter
	@Setter
	private String user;

	@Getter
	@Setter
	private String password;

	@Override
	public Connection getConnection() {
		return mock(Connection.class);
	}

	@Override
	public Connection getConnection(String username, String password) {
		throw new IllegalStateException("expect method call without user / pass");
	}

	@Override
	public Logger getParentLogger() {
		return null;
	}

	@Override
	public <T> T unwrap(Class<T> iface) {
		return null;
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) {
		return false;
	}

	@Override
	public PrintWriter getLogWriter() {
		return null;
	}

	@Override
	public void setLogWriter(PrintWriter out) {
		// NO OP
	}

	@Override
	public void setLoginTimeout(int seconds) {
		// NO OP
	}

	@Override
	public int getLoginTimeout() {
		return 0;
	}

}
