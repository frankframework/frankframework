package nl.nn.adapterframework.testutil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jdbc.JdbcException;

public class FixedQuerySenderMock extends FixedQuerySender {
	private ResultSet rs = null;

	public FixedQuerySenderMock() {
		this(Mockito.mock(ResultSet.class));
	}

	public FixedQuerySenderMock(ResultSet rs) {
		setName("FixedQuerySenderMock");
		setQuery("select * from dummy");
		this.rs = rs;
	}

	@Override
	public Connection getConnection() throws JdbcException {
		try {
			Connection conn = Mockito.mock(Connection.class);
			PreparedStatement stmt = Mockito.mock(PreparedStatement.class);
			Mockito.doReturn(stmt).when(conn).prepareStatement(Mockito.anyString());
			Mockito.doReturn(rs).when(stmt).executeQuery();
			return conn;
		} catch (SQLException e) {
			throw new JdbcException(e);
		}
	}

	@Override
	protected DataSource getDatasource() throws JdbcException {
		return Mockito.mock(DataSource.class);
	}

	public static class ResultSetBuilder {
		private List<Map<String, Object>> rows = new ArrayList<>();
		private Map<String, Object> row = new HashMap<>();

		public static ResultSetBuilder create() {
			return new ResultSetBuilder();
		}

		public ResultSetBuilder addRow() {
			if(row != null) {
				rows.add(row);
			}

			row = new HashMap<>();
			return this;
		}

		public ResultSetBuilder setValue(String rowName, Object value) {
			row.put(rowName, value);
			return this;
		}

		public ResultSet build() throws SQLException {
			rows.add(row); //Add the last row

			ResultSet rs = Mockito.mock(ResultSet.class);
			Mockito.doAnswer(new Answer<Boolean>() {
				@Override
				public Boolean answer(InvocationOnMock invocation) throws Throwable {
					if(!rows.isEmpty()) {
						row = rows.remove(0);
						return true;
					}
					return false;
				}}).when(rs).next();
			Mockito.doAnswer(new Answer<String>() {
				@Override
				public String answer(InvocationOnMock invocation) throws Throwable {
					String key = invocation.getArgument(0);
					return (String) row.get(key);
				}}).when(rs).getString(Mockito.anyString());

			return rs;
		}
	}
}
