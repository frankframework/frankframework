package org.frankframework.jta.xa;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

public class XaDatasourceObserver implements XADataSource{

	private final XADataSource target;
	private final Function<XAConnection,XAConnection> connectionObservationProvider;

	public XaDatasourceObserver(XADataSource target) {
		this(target, XaConnectionObserver::new);
	}

	public XaDatasourceObserver(XADataSource target, Function<XAConnection,XAConnection> connectionObservationProvider) {
		this.target = target;
		this.connectionObservationProvider = connectionObservationProvider;
	}

	@Override
	public PrintWriter getLogWriter() throws SQLException {
		return target.getLogWriter();
	}

	@Override
	public int getLoginTimeout() throws SQLException {
		return target.getLoginTimeout();
	}

	@Override
	public Logger getParentLogger() throws SQLFeatureNotSupportedException {
		return target.getParentLogger();
	}

	@Override
	public void setLogWriter(PrintWriter w) throws SQLException {
		target.setLogWriter(w);
	}

	@Override
	public void setLoginTimeout(int t) throws SQLException {
		target.setLoginTimeout(t);
	}

	@Override
	public XAConnection getXAConnection() throws SQLException {
		return connectionObservationProvider.apply(target.getXAConnection());
	}

	@Override
	public XAConnection getXAConnection(String username, String password) throws SQLException {
		return connectionObservationProvider.apply(target.getXAConnection(username, password));
	}

}
