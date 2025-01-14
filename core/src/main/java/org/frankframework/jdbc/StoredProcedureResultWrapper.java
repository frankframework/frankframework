/*
   Copyright 2023 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;

import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.parameters.IParameter;
import org.frankframework.util.JdbcUtil;

public class StoredProcedureResultWrapper implements ResultSet {

	@Nonnull private final IDbmsSupport dbmsSupport;
	@Nonnull private final CallableStatement delegate;
	@Nonnull private final ParameterMetaData parameterMetaData;
	@Nonnull private final List<Map.Entry<Integer, IParameter>> parameterPositions;

	private static final UnsupportedOperationException CANNOT_UPDATE_RESULTS_EXCEPTION = new UnsupportedOperationException("Cannot update results from Callable Statement");

	private boolean hasNext;

	/**
	 * Class that wraps a CallableStatement to present its output-parameters as if they were
	 * a {@link ResultSet}.
	 *
	 * @param delegate The {@link CallableStatement} to be wrapped
	 * @param parameterPositions The position of each output-parameter in the overal list of stored procedure parameters
	 */
	public StoredProcedureResultWrapper(
			@Nonnull IDbmsSupport dbmsSupport,
			@Nonnull CallableStatement delegate, @Nonnull ParameterMetaData parameterMetaData, @Nonnull Map<Integer, IParameter> parameterPositions) {
		this.dbmsSupport = dbmsSupport;
		this.delegate = delegate;
		this.parameterMetaData = parameterMetaData;
		this.parameterPositions = parameterPositions.entrySet()
				.stream()
				.sorted(Map.Entry.comparingByKey())
				.collect(Collectors.toList());
		this.hasNext = !parameterPositions.isEmpty();
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
	public boolean wasNull() throws SQLException {
		return delegate.wasNull();
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
		for (Map.Entry<Integer, IParameter> entry : parameterPositions) {
			if (entry.getValue().getName().equalsIgnoreCase(columnLabel)) {
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
		// No-op
	}

	@Override
	public int getFetchDirection() {
		return ResultSet.FETCH_FORWARD;
	}

	@Override
	public void setFetchSize(int rows) {
		// No-op
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
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateBoolean(int columnIndex, boolean x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateByte(int columnIndex, byte x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateShort(int columnIndex, short x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateInt(int columnIndex, int x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateLong(int columnIndex, long x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateFloat(int columnIndex, float x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateDouble(int columnIndex, double x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateBigDecimal(int columnIndex, BigDecimal x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateString(int columnIndex, String x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateBytes(int columnIndex, byte[] x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateDate(int columnIndex, Date x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateTime(int columnIndex, Time x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateTimestamp(int columnIndex, Timestamp x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, int length) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, int length) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, int length) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateObject(int columnIndex, Object x, int scaleOrLength) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateObject(int columnIndex, Object x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateNull(String columnLabel) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateBoolean(String columnLabel, boolean x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateByte(String columnLabel, byte x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateShort(String columnLabel, short x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateInt(String columnLabel, int x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateLong(String columnLabel, long x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateFloat(String columnLabel, float x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateDouble(String columnLabel, double x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateBigDecimal(String columnLabel, BigDecimal x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateString(String columnLabel, String x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateBytes(String columnLabel, byte[] x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateDate(String columnLabel, Date x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateTime(String columnLabel, Time x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateTimestamp(String columnLabel, Timestamp x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, int length) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, int length) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, int length) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateObject(String columnLabel, Object x, int scaleOrLength) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateObject(String columnLabel, Object x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void insertRow() {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateRow() {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void deleteRow() {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void refreshRow() {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void cancelRowUpdates() {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void moveToInsertRow() {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void moveToCurrentRow() {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
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
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateRef(String columnLabel, Ref x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateBlob(int columnIndex, Blob x) throws SQLException {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateBlob(String columnLabel, Blob x) throws SQLException {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateClob(int columnIndex, Clob x) throws SQLException {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateClob(String columnLabel, Clob x) throws SQLException {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateArray(int columnIndex, Array x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateArray(String columnLabel, Array x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
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
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateRowId(String columnLabel, RowId x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
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
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateNString(String columnLabel, String nString) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateNClob(int columnIndex, NClob nClob) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateNClob(String columnLabel, NClob nClob) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
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
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateSQLXML(String columnLabel, SQLXML xmlObject) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
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
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader, long length) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x, long length) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x, long length) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x, long length) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x, long length) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x, long length) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader, long length) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateNClob(int columnIndex, Reader reader, long length) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateNClob(String columnLabel, Reader reader, long length) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateNCharacterStream(int columnIndex, Reader x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateNCharacterStream(String columnLabel, Reader reader) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateAsciiStream(int columnIndex, InputStream x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateBinaryStream(int columnIndex, InputStream x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateCharacterStream(int columnIndex, Reader x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateAsciiStream(String columnLabel, InputStream x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateBinaryStream(String columnLabel, InputStream x) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateCharacterStream(String columnLabel, Reader reader) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateClob(int columnIndex, Reader reader) throws SQLException {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateClob(String columnLabel, Reader reader) throws SQLException {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateNClob(int columnIndex, Reader reader) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
	}

	@Override
	public void updateNClob(String columnLabel, Reader reader) {
		throw CANNOT_UPDATE_RESULTS_EXCEPTION;
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

	private Integer mapColumnIndexToParamNr(final int column) {
		return parameterPositions.get(column - 1).getKey();
	}

	private int mapColumnLabelToParamNr(final String columnLabel) throws SQLException {
		return parameterPositions.stream()
				.filter(entry -> entry.getValue().getName().equalsIgnoreCase(columnLabel))
				.findFirst()
				.map(Map.Entry::getKey)
				.orElseThrow(() -> new SQLException("Cannot find parameter with label [" + columnLabel + "]"));
	}

	private class MyResultSetMetaData implements ResultSetMetaData {

		private SQLType getSqlType(final int column) {
			return JdbcUtil.mapParameterTypeToSqlType(dbmsSupport, parameterPositions.get(column - 1).getValue().getType());
		}

		@Override
		public int getColumnCount() {
			return parameterPositions.size();
		}

		@Override
		public boolean isAutoIncrement(int column) {
			return false;
		}

		@Override
		public boolean isCaseSensitive(int column) {
			return true;
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
			return getColumnName(column);
		}

		@Override
		public String getColumnName(int column) {
			return parameterPositions.get(column - 1).getValue().getName();
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
			return getSqlType(column).getVendorTypeNumber();
		}

		@Override
		public String getColumnTypeName(int column) {
			return getSqlType(column).getName();
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
			// Might fail with exception, mostly on Oracle, but I don't have a good alternative
			return parameterMetaData.getParameterClassName(column);
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
