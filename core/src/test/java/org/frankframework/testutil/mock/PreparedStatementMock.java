package org.frankframework.testutil.mock;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.mockito.Mockito;

import org.frankframework.util.StringUtil;

public abstract class PreparedStatementMock extends Mockito implements PreparedStatement {

	private String query;
	private Map<Integer, Object> parameterMap;

	public void setQuery(String query) {
		this.query = query;
		parameterMap = new HashMap<>();
	}

	public Map<Integer, Object> getParameters() {
		return parameterMap;
	}

	public Map<String, Object> getNamedParameters() {
		Map<String, Object> map = new HashMap<>();

		//Prepare parameterMap. We can assume its a proper query!
		int fieldTo = query.indexOf(")");
		String fields = query.substring(query.indexOf("(")+1, fieldTo);
		String values = query.substring(query.indexOf("(", fieldTo)+1, query.lastIndexOf(")"));
		Iterator<String> fieldIter = StringUtil.split(fields).iterator();
		Iterator<String> valueIter = StringUtil.split(values).iterator();

		int index = 1;
		while (fieldIter.hasNext()) {
			String fieldName = fieldIter.next();
			String fieldIndex = valueIter.next();
			Object value;
			if("?".equals(fieldIndex)) {
				value = parameterMap.get(index);
				index++;
			} else {
				value = fieldIndex;
			}
			map.put(fieldName, value);
		}
		return map;
	}

	public static PreparedStatementMock newInstance(String query) {
		PreparedStatementMock mock = mock(PreparedStatementMock.class, CALLS_REAL_METHODS);
		mock.setQuery(query);
		return mock;
	}

	@Override
	public int executeUpdate() throws SQLException {
		return 1;
	}

	@Override
	public void setNull(int parameterIndex, int sqlType) throws SQLException {
		parameterMap.put(parameterIndex, null);
	}

	@Override
	public void setString(int parameterIndex, String x) throws SQLException {
		parameterMap.put(parameterIndex, x);
	}

	@Override
	public void clearParameters() throws SQLException {
		parameterMap.clear();
	}

	@Override
	public void setObject(int parameterIndex, Object x) throws SQLException {
		parameterMap.put(parameterIndex, x);
	}

	@Override
	public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
		parameterMap.put(parameterIndex, x);
	}
}
