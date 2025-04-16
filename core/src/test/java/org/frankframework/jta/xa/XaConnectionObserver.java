package org.frankframework.jta.xa;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

import javax.sql.ConnectionEventListener;
import javax.sql.StatementEventListener;
import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

public class XaConnectionObserver implements XAConnection {

	private final XAConnection target;
	private final Function<XAResource,XAResource> resourceObservationProvider;

	public XaConnectionObserver(XAConnection target) {
		this(target, XaResourceObserver::new);
	}
	public XaConnectionObserver(XAConnection target, Function<XAResource,XAResource> resourceObservationProvider) {
		this.target = target;
		this.resourceObservationProvider = resourceObservationProvider;
	}

	@Override
	public void addConnectionEventListener(ConnectionEventListener arg0) {
		target.addConnectionEventListener(arg0);
	}

	@Override
	public void addStatementEventListener(StatementEventListener arg0) {
		target.addStatementEventListener(arg0);
	}

	@Override
	public void close() throws SQLException {
		target.close();
	}

	@Override
	public Connection getConnection() throws SQLException {
		return target.getConnection();
	}

	@Override
	public void removeConnectionEventListener(ConnectionEventListener arg0) {
		target.removeConnectionEventListener(arg0);
	}

	@Override
	public void removeStatementEventListener(StatementEventListener arg0) {
		target.removeStatementEventListener(arg0);
	}

	@Override
	public XAResource getXAResource() throws SQLException {
		return resourceObservationProvider.apply(target.getXAResource());
	}

}
