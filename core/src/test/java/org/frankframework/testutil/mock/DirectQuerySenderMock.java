package org.frankframework.testutil.mock;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.mockito.Mockito;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.dbms.GenericDbmsSupport;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.dbms.JdbcException;
import org.frankframework.jdbc.DirectQuerySender;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.stream.Message;

/**
 * Enables the ability to provide a mockable DirectQuerySender. In some places a new QuerySender is created to execute (custom) statements.
 * This allows the result to be mocked.
 *
 * @See {@link BusTestBase#mockDirectQuerySenderResult(String, Message)}
 *
 * @author Niels Meijer
 */
public class DirectQuerySenderMock extends DirectQuerySender {
	private final Map<String, Message> mocks = new HashMap<>();

	@Override
	public Connection getConnection() throws JdbcException {
		if(mocks.containsKey(getName())) {
			try {
				Connection conn = Mockito.mock(Connection.class);
				DatabaseMetaData md= Mockito.mock(DatabaseMetaData.class);
				Mockito.doReturn(md).when(conn).getMetaData();
				PreparedStatement stmt = Mockito.mock(PreparedStatement.class);
				Mockito.doReturn(stmt).when(conn).prepareStatement(Mockito.anyString());
				ResultSet mock = FixedQuerySenderMock.ResultSetBuilder.create().build();
				Mockito.doReturn(mock).when(stmt).executeQuery();
				return conn;
			} catch (SQLException e) {
				throw new JdbcException(e);
			}
		}
		return super.getConnection();
	}

	@Override
	protected DataSource getDatasource() throws JdbcException {
		if(mocks.containsKey(getName())) {
			return Mockito.mock(DataSource.class);
		}
		return super.getDatasource();
	}

	@Override
	public Connection openBlock(PipeLineSession session) throws SenderException, TimeoutException {
		return null;
	}

	@Override
	public void closeBlock(Connection connection, PipeLineSession session) throws SenderException {
		//ignore
	}

	@Override
	public SenderResult sendMessage(Connection blockHandle, Message message, PipeLineSession session) throws SenderException, TimeoutException {
		Message mockResult = mocks.get(getName());
		if(!Message.isNull(mockResult)) {
			return new SenderResult(mockResult);
		}
		return new SenderResult(message);
	}

	@Override
	public IDbmsSupport getDbmsSupport() {
		return new GenericDbmsSupport();
	}

	public void addMockedQueries(Map<String, Message> mocks) {
		this.mocks.putAll(mocks);
	}

	public void addMock(String query, Message resultSet) {
		mocks.put(query, resultSet);
	}
}
