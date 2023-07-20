package nl.nn.adapterframework.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.JDBCType;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

class StoredProcedureResultWrapper implements ResultSet {

	private final CallableStatement delegate;
	private final LinkedHashMap<Integer, JDBCType> parameterMappings;

	private boolean hasNext = true;

	StoredProcedureResultWrapper(CallableStatement delegate, LinkedHashMap<Integer, JDBCType> parameterMappings) {
		this.delegate = delegate;
		this.parameterMappings = parameterMappings;
	}

	@Override
	public boolean next() throws SQLException {
		if (hasNext) {
			hasNext = false;
			return true;
		}
		return false;
	}

	@Override
	public void close() throws SQLException {
		delegate.close();
	}

	@Override
	public boolean wasNull() {
		return false;
	}

	@Override
	public String getString(int columnIndex) throws SQLException {
		return delegate.getString(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public boolean getBoolean(int columnIndex) throws SQLException {
		return delegate.getBoolean(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public byte getByte(int columnIndex) throws SQLException {
		return delegate.getByte(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public short getShort(int columnIndex) throws SQLException {
		return delegate.getShort(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public int getInt(int columnIndex) throws SQLException {
		return delegate.getInt(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public long getLong(int columnIndex) throws SQLException {
		return delegate.getLong(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public float getFloat(int columnIndex) throws SQLException {
		return delegate.getFloat(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public double getDouble(int columnIndex) throws SQLException {
		return delegate.getDouble(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
		return delegate.getBigDecimal(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public byte[] getBytes(int columnIndex) throws SQLException {
		return delegate.getBytes(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public Date getDate(int columnIndex) throws SQLException {
		return delegate.getDate(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public Time getTime(int columnIndex) throws SQLException {
		return delegate.getTime(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		return delegate.getTimestamp(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public InputStream getAsciiStream(int columnIndex) {
		throw new UnsupportedOperationException("Cannot get ASCII stream from CallableStatement");
	}

	@Override
	public InputStream getUnicodeStream(int columnIndex) {
		throw new UnsupportedOperationException("Cannot get Unicode stream from CallableStatement");
	}

	@Override
	public InputStream getBinaryStream(int columnIndex) {
		throw new UnsupportedOperationException("Cannot get binary stream from CallableStatement");
	}

	@Override
	public String getString(String columnLabel) throws SQLException {
		return delegate.getString(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public boolean getBoolean(String columnLabel) throws SQLException {
		return delegate.getBoolean(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public byte getByte(String columnLabel) throws SQLException {
		return delegate.getByte(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public short getShort(String columnLabel) throws SQLException {
		return delegate.getShort(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public int getInt(String columnLabel) throws SQLException {
		return delegate.getInt(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public long getLong(String columnLabel) throws SQLException {
		return delegate.getLong(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public float getFloat(String columnLabel) throws SQLException {
		return delegate.getFloat(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public double getDouble(String columnLabel) throws SQLException {
		return delegate.getDouble(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
		return delegate.getBigDecimal(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public byte[] getBytes(String columnLabel) throws SQLException {
		return delegate.getBytes(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public Date getDate(String columnLabel) throws SQLException {
		return delegate.getDate(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public Time getTime(String columnLabel) throws SQLException {
		return delegate.getTime(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public Timestamp getTimestamp(String columnLabel) throws SQLException {
		return delegate.getTimestamp(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public InputStream getAsciiStream(String columnLabel) {
		throw new UnsupportedOperationException("Cannot get ASCII stream from CallableStatement");
	}

	@Override
	public InputStream getUnicodeStream(String columnLabel) {
		throw new UnsupportedOperationException("Cannot get Unicode stream from CallableStatement");
	}

	@Override
	public InputStream getBinaryStream(String columnLabel) {
		throw new UnsupportedOperationException("Cannot get binary stream from CallableStatement");
	}

	@Override
	public SQLWarning getWarnings() throws SQLException {
		return delegate.getWarnings();
	}

	@Override
	public void clearWarnings() throws SQLException {
		delegate.clearWarnings();
	}

	@Override
	public String getCursorName() {
		throw new UnsupportedOperationException("Cannot get Cursor name from CallableStatement");
	}

	@Override
	public ResultSetMetaData getMetaData() throws SQLException {
		return new MyResultSetMetaData();
	}

	@Override
	public Object getObject(int columnIndex) throws SQLException {
		return delegate.getObject(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public Object getObject(String columnLabel) throws SQLException {
		return delegate.getObject(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public int findColumn(String columnLabel) throws SQLException {
		int idx = 1;
		for (Integer paramNr : parameterMappings.keySet()) {
			String columnName = String.valueOf(paramNr);
			if (columnName.equalsIgnoreCase(columnLabel)) {
				return idx;
			}
			idx++;
		}
		throw new SQLException("Column with label [" + columnLabel + "] not found");
	}

	@Override
	public Reader getCharacterStream(int columnIndex) throws SQLException {
		return delegate.getCharacterStream(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public Reader getCharacterStream(String columnLabel) throws SQLException {
		return delegate.getCharacterStream(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		return delegate.getBigDecimal(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
		return delegate.getBigDecimal(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public boolean isBeforeFirst() {
		return hasNext;
	}

	@Override
	public boolean isAfterLast() {
		return !hasNext;
	}

	@Override
	public boolean isFirst() {
		return !hasNext;
	}

	@Override
	public boolean isLast() {
		return !hasNext;
	}

	@Override
	public void beforeFirst() {
		hasNext = true;
	}

	@Override
	public void afterLast() {
		hasNext = false;
	}

	@Override
	public boolean first() throws SQLException {
		return true;
	}

	@Override
	public boolean last() throws SQLException {
		return false;
	}

	@Override
	public int getRow() {
		return 0;
	}

	@Override
	public boolean absolute(int row) throws SQLException {
		return true;
	}

	@Override
	public boolean relative(int rows) throws SQLException {
		return true;
	}

	@Override
	public boolean previous() throws SQLException {
		return true;
	}

	@Override
	public void setFetchDirection(int direction) {

	}

	@Override
	public int getFetchDirection() {
		return ResultSet.FETCH_FORWARD;
	}

	@Override
	public void setFetchSize(int rows) {

	}

	@Override
	public int getFetchSize() {
		return 1;
	}

	@Override
	public int getType() throws SQLException {
		return ResultSet.TYPE_FORWARD_ONLY;
	}

	@Override
	public int getConcurrency() {
		return ResultSet.CONCUR_READ_ONLY;
	}

	@Override
	public boolean rowUpdated() {
		return false;
	}

	@Override
	public boolean rowInserted() {
		return false;
	}

	@Override
	public boolean rowDeleted() {
		return false;
	}

	@Override
	public void updateNull(int columnIndex) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateBoolean(int columnIndex, boolean x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateByte(int columnIndex, byte x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateShort(int columnIndex, short x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateInt(int columnIndex, int x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateLong(int columnIndex, long x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateFloat(int columnIndex, float x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateDouble(int columnIndex, double x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateBigDecimal(int columnIndex, BigDecimal x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateString(int columnIndex, String x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateBytes(int columnIndex, byte[] x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateDate(int columnIndex, Date x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateTime(int columnIndex, Time x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateTimestamp(int columnIndex, Timestamp x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, int length) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, int length) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, int length) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateObject(int columnIndex, Object x, int scaleOrLength) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateObject(int columnIndex, Object x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateNull(String columnLabel) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateBoolean(String columnLabel, boolean x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateByte(String columnLabel, byte x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateShort(String columnLabel, short x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateInt(String columnLabel, int x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateLong(String columnLabel, long x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateFloat(String columnLabel, float x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateDouble(String columnLabel, double x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateBigDecimal(String columnLabel, BigDecimal x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateString(String columnLabel, String x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateBytes(String columnLabel, byte[] x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateDate(String columnLabel, Date x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateTime(String columnLabel, Time x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateTimestamp(String columnLabel, Timestamp x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, int length) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, int length) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, int length) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateObject(String columnLabel, Object x, int scaleOrLength) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateObject(String columnLabel, Object x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void insertRow() {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateRow() {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void deleteRow() {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void refreshRow() {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void cancelRowUpdates() {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void moveToInsertRow() {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void moveToCurrentRow() {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public Statement getStatement() throws SQLException {
		return delegate;
	}

	@Override
	public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
		return delegate.getObject(mapColumnIndexToParamNr(columnIndex), map);
	}

	@Override
	public Ref getRef(int columnIndex) throws SQLException {
		return delegate.getRef(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public Blob getBlob(int columnIndex) throws SQLException {
		return delegate.getBlob(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public Clob getClob(int columnIndex) throws SQLException {
		return delegate.getClob(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public Array getArray(int columnIndex) throws SQLException {
		return delegate.getArray(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
		return delegate.getObject(mapColumnLabelToParamNr(columnLabel), map);
	}

	@Override
	public Ref getRef(String columnLabel) throws SQLException {
		return delegate.getRef(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public Blob getBlob(String columnLabel) throws SQLException {
		return delegate.getBlob(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public Clob getClob(String columnLabel) throws SQLException {
		return delegate.getClob(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public Array getArray(String columnLabel) throws SQLException {
		return delegate.getArray(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		return delegate.getDate(mapColumnIndexToParamNr(columnIndex), cal);
	}

	@Override
	public Date getDate(String columnLabel, Calendar cal) throws SQLException {
		return delegate.getDate(mapColumnLabelToParamNr(columnLabel), cal);
	}

	@Override
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		return delegate.getTime(mapColumnIndexToParamNr(columnIndex), cal);
	}

	@Override
	public Time getTime(String columnLabel, Calendar cal) throws SQLException {
		return delegate.getTime(mapColumnLabelToParamNr(columnLabel), cal);
	}

	@Override
	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
		return delegate.getTimestamp(mapColumnIndexToParamNr(columnIndex), cal);
	}

	@Override
	public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
		return delegate.getTimestamp(mapColumnLabelToParamNr(columnLabel), cal);
	}

	@Override
	public URL getURL(int columnIndex) throws SQLException {
		return delegate.getURL(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public URL getURL(String columnLabel) throws SQLException {
		return delegate.getURL(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public void updateRef(int columnIndex, Ref x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateRef(String columnLabel, Ref x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateBlob(int columnIndex, Blob x) throws SQLException {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateBlob(String columnLabel, Blob x) throws SQLException {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateClob(int columnIndex, Clob x) throws SQLException {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateClob(String columnLabel, Clob x) throws SQLException {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateArray(int columnIndex, Array x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateArray(String columnLabel, Array x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public RowId getRowId(int columnIndex) {
		throw new UnsupportedOperationException("ROWID not supported for Callable Statement results");
	}

	@Override
	public RowId getRowId(String columnLabel) {
		throw new UnsupportedOperationException("ROWID not supported for Callable Statement results");
	}

	@Override
	public void updateRowId(int columnIndex, RowId x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateRowId(String columnLabel, RowId x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public int getHoldability() {
		return ResultSet.CLOSE_CURSORS_AT_COMMIT;
	}

	@Override
	public boolean isClosed() throws SQLException {
		return delegate.isClosed();
	}

	@Override
	public void updateNString(int columnIndex, String nString) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateNString(String columnLabel, String nString) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateNClob(int columnIndex, NClob nClob) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateNClob(String columnLabel, NClob nClob) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public NClob getNClob(int columnIndex) throws SQLException {
		return delegate.getNClob(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public NClob getNClob(String columnLabel) throws SQLException {
		return delegate.getNClob(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public SQLXML getSQLXML(int columnIndex) throws SQLException {
		return delegate.getSQLXML(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public SQLXML getSQLXML(String columnLabel) throws SQLException {
		return delegate.getSQLXML(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public void updateSQLXML(int columnIndex, SQLXML xmlObject) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateSQLXML(String columnLabel, SQLXML xmlObject) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public String getNString(int columnIndex) throws SQLException {
		return delegate.getNString(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public String getNString(String columnLabel) throws SQLException {
		return delegate.getNString(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public Reader getNCharacterStream(int columnIndex) throws SQLException {
		return delegate.getNCharacterStream(mapColumnIndexToParamNr(columnIndex));
	}

	@Override
	public Reader getNCharacterStream(String columnLabel) throws SQLException {
		return delegate.getNCharacterStream(mapColumnLabelToParamNr(columnLabel));
	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x, long length) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader, long length) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, long length) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, long length) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, long length) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, long length) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, long length) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, long length) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateNClob(int columnIndex, Reader reader, long length) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateNClob(String columnLabel, Reader reader, long length) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateClob(int columnIndex, Reader reader) throws SQLException {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateClob(String columnLabel, Reader reader) throws SQLException {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateNClob(int columnIndex, Reader reader) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public void updateNClob(String columnLabel, Reader reader) {
		throw new UnsupportedOperationException("Cannot update results from Callable Statement");
	}

	@Override
	public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
		return delegate.getObject(mapColumnIndexToParamNr(columnIndex), type);
	}

	@Override
	public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
		return delegate.getObject(mapColumnLabelToParamNr(columnLabel), type);
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return delegate.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return delegate.isWrapperFor(iface);
	}

	private JDBCType mapColumnIndexToParamType(final int column) {
		return parameterMappings.values().toArray(new JDBCType[0])[column - 1];
	}

	private Integer mapColumnIndexToParamNr(final int column) {
		return parameterMappings.keySet().toArray(new Integer[0])[column - 1];
	}

	private int mapColumnLabelToParamNr(final String columnLabel) {
		return Integer.parseInt(columnLabel);
	}

	private class MyResultSetMetaData implements ResultSetMetaData {

		public MyResultSetMetaData() {
		}

		@Override
		public int getColumnCount() {
			return parameterMappings.size();
		}

		@Override
		public boolean isAutoIncrement(int column) {
			return false;
		}

		@Override
		public boolean isCaseSensitive(int column) {
			return false;
		}

		@Override
		public boolean isSearchable(int column) {
			return false;
		}

		@Override
		public boolean isCurrency(int column) {
			return false;
		}

		@Override
		public int isNullable(int column) {
			return ResultSetMetaData.columnNullableUnknown;
		}

		@Override
		public boolean isSigned(int column) {
			return false;
		}

		@Override
		public int getColumnDisplaySize(int column) {
			return 0;
		}

		@Override
		public String getColumnLabel(int column) {
			return mapColumnIndexToParamNr(column).toString();
		}

		@Override
		public String getColumnName(int column) {
			return mapColumnIndexToParamNr(column).toString();
		}

		@Override
		public String getSchemaName(int column) {
			return null;
		}

		@Override
		public int getPrecision(int column) {
			return 0;
		}

		@Override
		public int getScale(int column) {
			return 0;
		}

		@Override
		public String getTableName(int column) {
			return null;
		}

		@Override
		public String getCatalogName(int column) {
			return null;
		}

		@Override
		public int getColumnType(int column) {
			return mapColumnIndexToParamType(column).getVendorTypeNumber();
		}

		@Override
		public String getColumnTypeName(int column) {
			return mapColumnIndexToParamType(column).getName();
		}

		@Override
		public boolean isReadOnly(int column) {
			return true;
		}

		@Override
		public boolean isWritable(int column) {
			return false;
		}

		@Override
		public boolean isDefinitelyWritable(int column) {
			return false;
		}

		@Override
		public String getColumnClassName(int column) throws SQLException {
			Object value = delegate.getObject(mapColumnIndexToParamNr(column));
			if (value == null) return null;
			return value.getClass().getName();
		}

		@Override
		public <T> T unwrap(Class<T> iface) {
			return null;
		}

		@Override
		public boolean isWrapperFor(Class<?> iface) {
			return false;
		}
	}
}
