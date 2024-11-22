/*
   Copyright 2018 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.ladybug.tibet2;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.Configuration;
import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.dbms.Dbms;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.dbms.JdbcException;
import org.frankframework.jdbc.JdbcFacade;
import org.frankframework.ladybug.LadybugDebugger;
import org.frankframework.stream.Message;
import org.frankframework.util.JdbcUtil;
import org.frankframework.util.StreamUtil;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.CheckpointType;
import nl.nn.testtool.Report;
import nl.nn.testtool.SecurityContext;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.LogStorage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.SearchUtil;

/**
 * @author Jaco de Groot
 */
// LogStorage needs to be implemented because View.setDebugStorage() requires a LogStorage
// Reports can be deleted in the debug tab when a debug storage also implements CrudStorage
public class Tibet2DatabaseStorage extends JdbcFacade implements LogStorage, CrudStorage {
	private static final String TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	private static final String DELETE_ADAPTER_NAME = "DeleteFromExceptionLog";
	private static final String DELETE_ADAPTER_CONFIG = "main";
	private String name;
	private String table;
	private List<String> reportColumnNames;
	private List<String> bigValueColumns;
	private List<String> integerColumns;
	private List<String> timestampColumns;
	private List<String> fixedStringColumns;
	private Map<String, String> fixedStringTables;
	private TestTool testTool;
	private JdbcTemplate jdbcTemplate;
	private LadybugDebugger ibisDebugger;
	private SecurityContext securityContext;

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setTable(String table) {
		this.table = table;
	}

	public String getTable() {
		return table;
	}

	public void setReportColumnNames(List<String> reportColumnNames) {
		this.reportColumnNames = reportColumnNames;
	}

	public List<String> getReportColumnNames() {
		return reportColumnNames;
	}

	public void setBigValueColumns(List<String> bigValueColumns) {
		this.bigValueColumns = bigValueColumns;
	}

	public List<String> getBigValueColumns() {
		return bigValueColumns;
	}

	public void setIntegerColumns(List<String> integerColumns) {
		this.integerColumns = integerColumns;
	}

	public void setFixedStringColumns(List<String> fixedStringColumns) {
		this.fixedStringColumns = fixedStringColumns;
	}

	public void setFixedStringTables(Map<String, String> fixedStringTables) {
		this.fixedStringTables = fixedStringTables;
	}

	public List<String> getIntegerColumns() {
		return integerColumns;
	}

	public void setTimestampColumns(List<String> timestampColumns) {
		this.timestampColumns = timestampColumns;
	}

	public List<String> getTimestampColumns() {
		return timestampColumns;
	}

	public void setTestTool(TestTool testTool) {
		this.testTool = testTool;
	}

	public void setIbisDebugger(LadybugDebugger ibisDebugger) {
		this.ibisDebugger = ibisDebugger;
	}

	/**
	 * Called by TibetView.initBean() (not by Spring)
	 */
	public void setSecurityContext(SecurityContext securityContext) {
		this.securityContext = securityContext;
	}

	@PostConstruct
	public void init() throws JdbcException {
		jdbcTemplate = new JdbcTemplate(getDatasource());
	}

	@Override
	public int getSize() throws StorageException {
		try {
			return jdbcTemplate.queryForObject("select count(*) from " + table, Integer.class);
		} catch(DataAccessException e){
			throw new StorageException("Could not read size", e);
		}
	}

	@Override
	public List<Integer> getStorageIds() throws StorageException {
		try {
			List<Integer> storageIds = jdbcTemplate.query("select LOGID from " + table + " order by LOGID desc",
					(rs, rowNum) -> rs.getInt(1));
			return storageIds;
		} catch(DataAccessException e){
			throw new StorageException("Could not read storage id's", e);
		}
	}

	@Override
	public List<List<Object>> getMetadata(int maxNumberOfRecords,
			final List<String> metadataNames, List<String> searchValues,
			int metadataValueType) throws StorageException {
		// Prevent SQL injection (searchValues are passed as parameters to the SQL statement)
		for (String metadataName : metadataNames) {
			if (!reportColumnNames.contains(metadataName)) {
				throw new StorageException("Invalid metadata name: " + metadataName);
			}
		}
		List<String> rangeSearchValues = new ArrayList<>();
		List<String> regexSearchValues = new ArrayList<>();
		for (int i = 0; i < searchValues.size(); i++) {
			String searchValue = searchValues.get(i);
			if (searchValue != null && searchValue.startsWith("<")
					&& searchValue.endsWith(">")) {
				rangeSearchValues.add(searchValue);
				regexSearchValues.add(null);
				searchValues.remove(i);
				searchValues.add(i, null);
			} else if (searchValue != null && searchValue.startsWith("(")
					&& searchValue.endsWith(")")) {
				rangeSearchValues.add(null);
				regexSearchValues.add(searchValue);
				searchValues.remove(i);
				searchValues.add(i, null);
			} else {
				rangeSearchValues.add(null);
				regexSearchValues.add(null);
			}
		}
		IDbmsSupport dbmsSupport = getDbmsSupport();
		StringBuilder query = new StringBuilder("select"
				+ dbmsSupport.provideFirstRowsHintAfterFirstKeyword(maxNumberOfRecords)  + " * from (select ");
		List<Object> args = new ArrayList<>();
		List<Integer> argTypes = new ArrayList<>();
		boolean first = true;
		for (String metadataName : metadataNames) {
			if (first) {
				first = false;
			} else {
				query.append(", ");
			}
			if (bigValueColumns.contains(metadataName)) {
				query.append("substr(").append(metadataName).append(", 1, 100)");
			} else {
				query.append(metadataName);
			}
		}
		String rowNumber = getRowNumber(metadataNames.get(0));
		if (StringUtils.isNotEmpty(rowNumber)) {
			if (first) {
				first = false;
			} else {
				query.append(", ");
			}
			query.append(rowNumber);
		}
		query.append(" from ").append(table);
		// According to SimpleDateFormat javadoc it needs to be synchronized when accessed by multiple threads, hence
		// instantiate it here instead of instantiating it at class level and synchronizing it.
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(TIMESTAMP_PATTERN);
		for (int i = 0; i < rangeSearchValues.size(); i++) {
			String searchValue = rangeSearchValues.get(i);
			if (searchValue != null) {
				int j = searchValue.indexOf('|');
				if (j != -1) {
					String column = metadataNames.get(i);
					String searchValueLeft = searchValue.substring(1, j);
					String searchValueRight = searchValue.substring(j + 1,
							searchValue.length() - 1);
					if (StringUtils.isNotEmpty(searchValueLeft)) {
						if (integerColumns.contains(column)) {
							addNumberExpression(query, args, argTypes, column, ">=", searchValueLeft);
						} else if (timestampColumns.contains(column)) {
							addTimestampExpression(query, args, argTypes, column, ">=",
									searchValueLeft, simpleDateFormat);
						}
					}
					if (StringUtils.isNotEmpty(searchValueRight)) {
						if (integerColumns.contains(column)) {
							addNumberExpression(query, args, argTypes, column, "<=", searchValueRight);
						} else if (timestampColumns.contains(column)) {
							addTimestampExpression(query, args, argTypes, column, "<=",
									searchValueRight, simpleDateFormat);
						}
					}
				} else {
					throw new StorageException("Separator | not found");
				}
			}
		}
		for (int i = 0; i < searchValues.size(); i++) {
			String searchValue = searchValues.get(i);
			if (StringUtils.isNotEmpty(searchValue)) {
				String column = metadataNames.get(i);
				if (integerColumns.contains(column)) {
					addNumberExpression(query, args, argTypes, column, "<=", searchValue);
				} else if (timestampColumns.contains(column)) {
					addTimestampExpression(query, args, argTypes, column, "<=", searchValue, simpleDateFormat);
				} else if (fixedStringColumns != null && fixedStringColumns.contains(column)) {
					addFixedStringExpression(query, args, argTypes, column, searchValue);
				} else {
					addLikeExpression(query, args, argTypes, column, searchValue);
				}
			}
		}
		query.append(")");
		if (StringUtils.isNotEmpty(rowNumber)) {
			query.append(" where rn < ?");
			args.add(maxNumberOfRecords + 1);
			argTypes.add(Types.INTEGER);
		}
		query.append(" order by ");
		query.append(metadataNames.get(0) + " desc");
		log.debug("Metadata query: {}", query.toString());
		List<List<Object>> metadata;
		try {
			metadata = jdbcTemplate.query(query.toString(), args.toArray(), argTypes.stream().mapToInt(i -> i).toArray(),
					(rs, rowNum) ->
						{
							List<Object> row = new ArrayList<Object>();
							for (int i = 0; i < metadataNames.size(); i++) {
								if (integerColumns.contains(metadataNames.get(i))) {
									row.add(rs.getInt(i + 1));
								} else if (timestampColumns.contains(metadataNames.get(i))) {
									row.add(simpleDateFormat.format(rs.getTimestamp(i + 1)));
								} else {
									row.add(getValue(rs, i + 1));
								}
							}
							return row;
						}
					);
		} catch(DataAccessException e){
			throw new StorageException("Could not read metadata", e);
		}
		for (int i = 0; i < metadata.size(); i++) {
			if (!SearchUtil.matches((List<Object>)metadata.get(i), regexSearchValues)) {
				metadata.remove(i);
				i--;
			}
		}
		return metadata;
	}

	private @Nullable String getRowNumber(String metadataName) {
		Dbms dbms = getDbmsSupport().getDbms();
		if (dbms == Dbms.ORACLE || dbms == Dbms.MSSQL) {
			return "row_number() over (order by "+metadataName+("desc"==null?"":" "+"desc")+") rn";
		}
		return null;
	}

	@Override
	public Report getReport(Integer storageId) throws StorageException {
		final Report report = new Report();
		report.setTestTool(testTool);
		report.setStorage(this);
		report.setStorageId(storageId);
		report.setName("Table " + table);
		report.setStubStrategy("Never");
		final List<Checkpoint> checkpoints = new ArrayList<>();
		report.setCheckpoints(checkpoints);
		StringBuilder query = new StringBuilder("select "
				+ reportColumnNames.get(0));
		for (int i = 1; i < reportColumnNames.size(); i++) {
			query.append(", ").append(reportColumnNames.get(i));
		}
		query.append(" from ").append(table).append(" where LOGID = ?");
		try {
			jdbcTemplate.query(query.toString(),
					new Object[]{storageId},
					new int[] {Types.INTEGER},
					(rs, rowNum) ->
						{
							for (int i = 0; i < reportColumnNames.size(); i++) {
								String value = getValue(rs, i + 1);
								Checkpoint checkpoint = new Checkpoint(report,
										Thread.currentThread().getName(),
										Tibet2DatabaseStorage.class.getName(),
										"Column " + reportColumnNames.get(i),
										CheckpointType.INPUTPOINT.toInt(), 0);
								checkpoint.setMessage(value);
								checkpoints.add(checkpoint);
							}
							return null;
						}
					);
		} catch(DataAccessException e){
			throw new StorageException("Could not read report", e);
		}
		return report;
	}

	@Override
	public void close() {
	}

	private void addLikeExpression(StringBuilder query, List<Object> args, List<Integer> argTypes,
			String column, String searchValue) {
		if (searchValue.startsWith("~") && searchValue.contains("*")) {
			addExpression(query, "lower(" + column + ") like lower(?)");
		} else if (searchValue.startsWith("~")) {
			addExpression(query, "lower(" + column + ")=lower(?)");
		} else if (searchValue.contains("*")) {
			addExpression(query, column + " like ?");
		} else {
			addExpression(query, column + "=?");
		}

		if (searchValue.startsWith("~")) {
			searchValue = searchValue.substring(1);
		}
		searchValue = searchValue.replaceAll("\\*", "%");
		args.add(searchValue);
		argTypes.add(Types.VARCHAR);
	}

	private void addFixedStringExpression(StringBuilder query, List<Object> args, List<Integer> argTypes,
			String column, String searchValue) {
		String[] svs = searchValue.split(",");
		String questionMarks= "";
		for (int i = 0; i < svs.length; i++) {
			if (i == 0) {
				questionMarks = "?";
			} else {
				questionMarks = questionMarks + ",?";
			}
			args.add(svs[i]);
			argTypes.add(Types.VARCHAR);
		}
		addExpression(query, column + " in (" + questionMarks + ")");
	}

	private void addNumberExpression(StringBuilder query, List<Object> args, List<Integer> argTypes,
			String column, String operator, String searchValue)
					throws StorageException {
		try {
			BigDecimal bigDecimal = new BigDecimal(searchValue);
			addExpression(query, column + " " + operator + " ?");
			args.add(bigDecimal);
			argTypes.add(Types.DECIMAL);
		} catch(NumberFormatException e) {
			throw new StorageException("Search value '" + searchValue
					+ "' isn't a valid number");
		}
	}

	private void addTimestampExpression(StringBuilder query, List<Object> args, List<Integer> argTypes,
			String column, String operator, String searchValue,
			SimpleDateFormat simpleDateFormat) throws StorageException {
		String searchValueToParse;
		if (searchValue.length() < 23) {
			if (">=".equals(operator)) {
				searchValueToParse = searchValue + "0000-00-00T00:00:00.000".substring(searchValue.length());
			} else {
				searchValueToParse = searchValue + "9999-12-31T23:59:59.999".substring(searchValue.length());
			}
			int year = -1;
			int month = -1;
			int dayOfMonth = -1;
			try {
				year = Integer.parseInt(searchValueToParse.substring(0, 4));
				month = Integer.parseInt(searchValueToParse.substring(5, 7)) - 1;
				dayOfMonth = Integer.parseInt(searchValueToParse.substring(8, 10));
				Integer.parseInt(searchValueToParse.substring(11, 13));
				Integer.parseInt(searchValueToParse.substring(14, 16));
				Integer.parseInt(searchValueToParse.substring(17, 19));
				Integer.parseInt(searchValueToParse.substring(20, 23));
			} catch(NumberFormatException e) {
				throwExceptionOnInvalidTimestamp(searchValue);
			}
			if (searchValueToParse.charAt(4) != '-'
					|| searchValueToParse.charAt(7) != '-'
					|| searchValueToParse.charAt(7) != '-'
					|| searchValueToParse.charAt(10) != 'T'
					|| searchValueToParse.charAt(13) != ':'
					|| searchValueToParse.charAt(16) != ':'
					|| searchValueToParse.charAt(19) != '.') {
				throwExceptionOnInvalidTimestamp(searchValue);
			}
			if (!">=".equals(operator)) {
				Calendar calendar = new GregorianCalendar(year, month, 1);
				int maxDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
				if (dayOfMonth > maxDayOfMonth) {
					searchValueToParse = searchValueToParse.substring(0, 8) + maxDayOfMonth + searchValueToParse.substring(10);
				}
			}
		} else {
			searchValueToParse = searchValue;
		}
		try {
			args.add(new Timestamp(simpleDateFormat.parse(searchValueToParse).getTime()));
			argTypes.add(Types.TIMESTAMP);
			addExpression(query, column + " " + operator + " ?");
		} catch (ParseException e) {
			throwExceptionOnInvalidTimestamp(searchValue);
		}
	}

	private void throwExceptionOnInvalidTimestamp(String searchValue) throws StorageException {
		throw new StorageException("Search value '" + searchValue + "' doesn't comply with (the beginning of) pattern " + TIMESTAMP_PATTERN);
	}

	private void addExpression(StringBuilder query, String expression) {
		if (query.charAt(query.length() - 1) == ' ') {
			query.append("and ").append(expression).append(" ");
		} else {
			query.append(" where ").append(expression).append(" ");
		}
	}

	private String getValue(ResultSet rs, int columnIndex) throws SQLException {
		try {
			return JdbcUtil.getValue(getDbmsSupport(), rs, columnIndex, rs.getMetaData(), StreamUtil.DEFAULT_INPUT_STREAM_ENCODING, true, "", false, true, false);
		} catch (IOException e) {
			throw new SQLException("IOException reading value");
		}
	}

	@Override
	public int getFilterType(String column) {
		if (fixedStringColumns != null && fixedStringColumns.contains(column)) {
			return FILTER_SELECT;
		} else {
			return FILTER_RESET;
		}
	}

	@Override
	public List<Object> getFilterValues(String column) throws StorageException {
		// Prevent SQL injection
		if (!reportColumnNames.contains(column)) {
			throw new StorageException("Invalid metadata name: " + column);
		}
		String query;
		if (fixedStringTables.containsKey(column)) {
			query = "select " + column + " from " + fixedStringTables.get(column) + " order by " + column + " asc";
		} else {
			query = "select distinct " + column + " from " + table + " order by " + column + " asc";
		}
		try {
			List<Object> filterValues = jdbcTemplate.query(query, (rs, rowNum) -> rs.getObject(1));
			return filterValues;
		} catch (DataAccessException e) {
			throw new StorageException("Could not read filter values", e);
		}
	}

	@Override
	public String getUserHelp(String column) {
		if (integerColumns.contains(column)) {
			return "Search all rows which are less than or equal to the search value";
		} else if (timestampColumns.contains(column)) {
			return """
					Search all rows which are less than or equal to the search value.\
					 When the search value only complies with the beginning of pattern yyyy-MM-dd'T'HH:mm:ss.SSS, it will be internally completed according to the value 9999-12-31T23:59:59.999\
					""";
		} else if (fixedStringColumns != null
				&& fixedStringColumns.contains(column)) {
			return """
					Search all rows which completely equal the search value (case sensitive).\
					 Select one or more values from the drop-down list which is activated by clicking on the gray button directly above this field\
					""";
		} else {
			return """
					Search all rows which completely equal the search value (case sensitive).\
					 The wilcard character '*' is supported.\
					 When the search value start with the character '~', the search is performed case insensitive\
					""";
		}
	}

	@Override
	public void store(Report report) throws StorageException {
		throw new StorageException("Store method is not implemented");
	}

	@Override
	public void update(Report report) throws StorageException {
		throw new StorageException("Update method is not implemented");
	}

	@Override
	public void delete(Report report) throws StorageException {
		if (!"Table EXCEPTIONLOG".equals(report.getName())) {
			throw new StorageException("Delete method is not implemented for '" + report.getName() + "'");
		}
		List<Checkpoint> checkpoints = report.getCheckpoints();
		Checkpoint checkpoint = checkpoints.get(0);
		Message message = new Message(checkpoint.getMessage());
		Configuration config = ibisDebugger.getIbisManager().getConfiguration(DELETE_ADAPTER_CONFIG);
		if (config == null) {
			throw new StorageException("Configuration '" + DELETE_ADAPTER_CONFIG + "' not found");
		}
		Adapter adapter = config.getRegisteredAdapter(DELETE_ADAPTER_NAME);
		if (adapter == null) {
			throw new StorageException("Adapter '" + DELETE_ADAPTER_NAME + "' not found");
		}

		try (PipeLineSession pipeLineSession = new PipeLineSession()) {
			if (securityContext.getUserPrincipal() != null)
				pipeLineSession.put("principal", securityContext.getUserPrincipal().getName());
			PipeLineResult processResult = adapter.processMessageDirect(TestTool.getCorrelationId(), message, pipeLineSession);
			if (!processResult.isSuccessful()) {
				throw new StorageException("Delete failed (see logging for more details)");
			}

			try {
				String result = processResult.getResult().asString();
				if (!"<ok/>".equalsIgnoreCase(result)) {
					throw new StorageException("Delete failed: " + result);
				}
			} catch (IOException e) {
				throw new StorageException("Delete failed", e);
			}
		}
	}

	@Override
	public void clear() throws StorageException {
		throw new StorageException("Clear method is not implemented");
	}

	@Override
	public void storeWithoutException(Report report) {
	}

	@Override
	public String getWarningsAndErrors() {
		return null;
	}

}
