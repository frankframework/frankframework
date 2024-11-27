package org.frankframework.testutil.mock;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.dbms.JdbcException;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.jdbc.datasource.TransactionalDbmsSupportAwareDataSourceProxy;
import org.frankframework.testutil.TestConfiguration;

/**
 * Enables the ability to provide a mockable FixedQuerySender. In some places a new QuerySender is created to execute (custom) statements.
 * This allows the result to be mocked.
 *
 * @see TestConfiguration#mockQuery(String, ResultSet)
 * @author Niels Meijer
 */
public class FixedQuerySenderMock extends FixedQuerySender {
	private final Map<String, ResultSet> mocks = new HashMap<>();

	@Override
	public IDbmsSupport getDbmsSupport() {
		ResultSet mock = mocks.get(getQuery());
		if(mock != null) {
			return DbmsSupportMock.newInstance();
		}

		return super.getDbmsSupport();
	}

	@Override
	public Connection getConnection() throws JdbcException {
		ResultSet mock = mocks.get(getQuery());
		if(mock != null) {
			try {
				Connection conn = Mockito.mock(Connection.class);
				DatabaseMetaData md= Mockito.mock(DatabaseMetaData.class);
				Mockito.doReturn(md).when(conn).getMetaData();
				PreparedStatement stmt = Mockito.mock(PreparedStatement.class);
				Mockito.doReturn(stmt).when(conn).prepareStatement(Mockito.anyString());
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
		ResultSet mock = mocks.get(getQuery());
		if(mock != null) {
			return Mockito.mock(TransactionalDbmsSupportAwareDataSourceProxy.class);
		}
		return super.getDatasource();
	}

	public static class ResultSetBuilder {
		private final List<Map<String, Object>> rows = new ArrayList<>();
		private Map<String, Object> row = new HashMap<>();
		private final String INDEX_PREFIX = "index::";
		private AtomicInteger index = new AtomicInteger(1);

		public static ResultSetBuilder create() {
			return new ResultSetBuilder();
		}

		public ResultSetBuilder addRow() {
			if(row != null) {
				rows.add(row);
				index = new AtomicInteger(1); //Reset the row index
			}

			row = new HashMap<>();
			return this;
		}

		/** Add index based values in chronological order */
		public ResultSetBuilder setValue(Object value) {
			return setValue(INDEX_PREFIX+index.getAndIncrement(), value);
		}

		public ResultSetBuilder setValue(String rowName, Object value) {
			row.put(rowName, value);
			return this;
		}

		public ResultSet build() throws SQLException {
			rows.add(row); //Add the last row

			ResultSet rs = Mockito.mock(ResultSet.class);
			Mockito.doAnswer((Answer<Boolean>) invocation -> {
				if (!rows.isEmpty()) {
					row = rows.remove(0);
					return true;
				}
				return false;
			}).when(rs).next();

			Mockito.doAnswer((Answer<String>) invocation -> {
				String key = invocation.getArgument(0);
				return (String) row.get(key);
			}).when(rs).getString(Mockito.anyString());

			Mockito.doAnswer((Answer<String>) invocation -> {
				int index = invocation.getArgument(0);
				return (String) row.get(INDEX_PREFIX + index);
			}).when(rs).getString(Mockito.anyInt());

			return rs;
		}
	}

	public void addMockedQueries(Map<String, ResultSet> mocks) {
		this.mocks.putAll(mocks);
	}

	public void addMock(String query, ResultSet resultSet) {
		mocks.put(query, resultSet);
	}
}
