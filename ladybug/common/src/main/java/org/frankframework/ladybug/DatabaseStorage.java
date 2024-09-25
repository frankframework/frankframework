/*
Copyright 2022-2024 WeAreFrank!

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
package org.frankframework.ladybug;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.sql.DataSource;

import jakarta.annotation.PostConstruct;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import nl.nn.testtool.Config;
import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.MetadataFieldExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.LogStorage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.storage.database.DbmsSupport;
import nl.nn.testtool.storage.database.OptionalJtaTransactionManager;
import nl.nn.testtool.util.Export;
import nl.nn.testtool.util.Import;
import nl.nn.testtool.util.SearchUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
* Database storage implementation for Ladybug. The configuration of a transaction manager
* (see {@link Config#ladybugTransactionManager(DataSource)} will disable auto-commit so PostgreSQL will not throw the following
* exception on insert of a report:
*   org.postgresql.util.PSQLException: Large Objects may not be used in auto-commit mode.
* It would also be possible to set auto-commit to false on Connection(Pool) or DataSource level but then still a
* transaction manger needs to be configured for JdbcTemplate to commit changes. Otherwise everything seems to be
* working fine (logging shows insert query) but no data appears in database and debug tab.
* 
* @see OptionalJtaTransactionManager
* @author Jaco de Groot
*/
//Without proxyTargetClass = true the test webapp will give: Bean named 'proofOfMigrationStorage' is expected to be of
//type 'nl.nn.testtool.storage.proofofmigration.ProofOfMigrationStorage' but was actually of type 'jdk.proxy3.$Proxy26'
//@EnableTransactionManagement(proxyTargetClass = true)
//REQUIRES_NEW to prevent interference with transactions in the application using Ladybug. E.g. when an error occurs in
//a Frank!Framework adapter the insert of the Ladybug report should not be rolled back (which happens otherwise because
//transactions are thread bound and Ladybug runs in the same thread). With NOT_SUPPORTED PostgreSQL will complain about
//auto-commit.
//Although isolation = Isolation.READ_UNCOMMITTED could be considered for performance reasons it will give the
//following error for Oracle in the Frank!Framework test matrix: READ_COMMITTED and SERIALIZABLE are the only valid
//transaction levels.
@Transactional(propagation = Propagation.REQUIRES_NEW)
//@Dependent disabled for Quarkus for now because of the use of JdbcTemplate
public class DatabaseStorage implements LogStorage, CrudStorage {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	protected static final String TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	protected @Getter String name;
	protected @Setter String table;
	protected @Setter  @Autowired List<String> metadataNames; // Used as column names in this storage
	protected @Setter String storageIdColumn;
	protected @Setter List<String> bigValueColumns; // Columns for which to limit the number of retrieved characters to 100
	protected @Setter Boolean storeReportXml;
	protected @Setter Long maxStorageSize;
	protected @Setter @Getter  @Autowired JdbcTemplate ladybugJdbcTemplate;
	protected @Setter @Getter  @Autowired DbmsSupport dbmsSupport;
	protected @Setter @Getter  @Autowired MetadataExtractor metadataExtractor;
	protected String lastExceptionMessage;

	public void setName(String naaam) {
		this.name = naaam;
	}
	// Return defaults in get methods to make is possible for overriding class to return different default values when
	// set method hasn't been called (by checking for null value)

	public String getTable() {
		if (table == null) {
			return "LADYBUG";
		} else {
			return table;
		}
	}

	public List<String> getMetadataNames() {
		if (getMetadataExtractor().getExtraMetadataFieldExtractors() != null) {
			List<String> metadataNames = new ArrayList<String>(this.metadataNames);
			for (MetadataFieldExtractor extractor : getMetadataExtractor().getExtraMetadataFieldExtractors()) {
				// ExtraMetadataFieldExtractors can override standard fields (see extraMetadataFieldExtractors in
				// MetadataExtractor)
				if (!metadataNames.contains(extractor.getName())) {
					metadataNames.add(extractor.getName());
				}
			}
			return metadataNames;
		} else {
			return metadataNames;
		}
	}

	public String getStorageIdColumn() {
		if (storageIdColumn == null) {
			return "storageId";
		} else {
			return storageIdColumn;
		}
	}

	public List<String> getBigValueColumns() {
		if (bigValueColumns == null) {
			return new ArrayList<String>(Arrays.asList("reportXml"));
		} else {
			return bigValueColumns;
		}
	}

	public boolean isStoreReportXml() {
		if (storeReportXml == null) {
			return false;
		} else {
			return storeReportXml;
		}
	}

	public long getMaxStorageSize() {
		if (maxStorageSize == null) {
			return 100 * 1024 * 1024;
		} else {
			return maxStorageSize;
		}
	}

	@PostConstruct
	public void init() throws StorageException {
		if (!(getMetadataNames() != null && getMetadataNames().contains(getStorageIdColumn()))) {
			throw new StorageException("List metadataNames " + metadataNames
					+ " should at least contain storageId column name '" + getStorageIdColumn() + "'");
		}
	}

	@Override
	public void store(Report report) throws StorageException {
		byte[] reportBytes = Export.getReportBytes(report);
		String reportXml = report.toXml();
		long storageSize = reportBytes.length;
		if (isStoreReportXml()) {
			storageSize = storageSize + reportXml.length();
		}
		report.setStorageSize(storageSize);
		StringBuilder query = new StringBuilder("insert into " + getTable() + " (");
		for (String column : getMetadataNames()) {
			// Column storageId is expected to be an auto increment column
			if (!column.equals(getStorageIdColumn())) {
				if (query.charAt(query.length() - 1) != '(') {
					query.append(", ");
				}
				query.append(column);
			}
		}
		query.append(", report");
		if (isStoreReportXml()) {
			query.append(", reportxml");
		}
		if (dbmsSupport.autoIncrementKeyMustBeInserted()) {
			query.append(", " + getStorageIdColumn());
		}
		query.append(") values (");
		for (String column : getMetadataNames()) {
			if (!column.equals(getStorageIdColumn())) {
				if (query.charAt(query.length() - 1) != '(') {
					query.append(", ");
				}
				query.append("?");
			}
		}
		query.append(", ?");
		if (isStoreReportXml()) {
			query.append(", ?");
		}
		if (dbmsSupport.autoIncrementKeyMustBeInserted()) {
			query.append(", " + dbmsSupport.autoIncrementInsertValue("SEQ_" + getTable()));
		}
		query.append(")");
		log.debug("Store report query: " + query.toString());
		ladybugJdbcTemplate.update(query.toString(),
				new PreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps) throws SQLException {
						int i = 1;
						for (String column : getMetadataNames()) {
							if (!column.equals(getStorageIdColumn())) {
								if (isInteger(column)) {
									ps.setInt(i, (Integer)metadataExtractor.getMetadata(report, column, MetadataExtractor.VALUE_TYPE_OBJECT));
								} else if (isLong(column)) {
									ps.setLong(i, (Long)metadataExtractor.getMetadata(report, column, MetadataExtractor.VALUE_TYPE_OBJECT));
								} else if (isTimestamp(column)) {
									ps.setTimestamp(i, new Timestamp((Long)metadataExtractor.getMetadata(report, column, MetadataExtractor.VALUE_TYPE_OBJECT)));
								} else {
									ps.setString(i, (String)metadataExtractor.getMetadata(report, column, MetadataExtractor.VALUE_TYPE_OBJECT));
								}
								i++;
							}
						}
//						ps.setBlob(i, new ByteArrayInputStream(reportBytes));
						ps.setBytes(i, reportBytes);
						i++;
						if (isStoreReportXml()) {
							ps.setClob(i, new StringReader(reportXml));
						}
					}
				});
		if (getMaxStorageSize() > -1) {
			String averageQuery = "select avg(storageSize) from " + getTable();
			int averageStorageSize = ladybugJdbcTemplate.queryForObject(averageQuery, Integer.class);
			log.debug("Get average storage size query (returned " + averageStorageSize + "): " + averageQuery);
			int maxNrOfReports = (int)(getMaxStorageSize() / averageStorageSize);
			String deleteQuery = "delete from " + getTable() + " where " + getStorageIdColumn()
					+ " <= ((select max(" + getStorageIdColumn() + ") from " + getTable() + ") - ?)";
			delete(deleteQuery, maxNrOfReports);
		}
	}

	@Override
	public void storeWithoutException(Report report) {
		try {
			store(report);
		} catch(Throwable throwable) {
			lastExceptionMessage = throwable.getMessage();
			// When StorageException is should already be logged
			if (!(throwable instanceof StorageException)) {
				log.error("Caught unexpected throwable storing report", throwable);
			}
		}
	}

	@Override
	public String getWarningsAndErrors() {
		return lastExceptionMessage;
	}

	@Override
	@Transactional
	public Report getReport(Integer storageId) throws StorageException {
		String query = "select report from " + getTable() + " where " + getStorageIdColumn() + " = ?";
		log.debug("Get report query: " + query);
		List<Report> result = ladybugJdbcTemplate.query(query, new Object[]{storageId}, new int[] {Types.INTEGER},
				(resultSet, rowNum) -> getReport(storageId, resultSet.getBytes(1)));
//				(resultSet, rowNum) -> getReport(storageId, resultSet.getBlob(1)));
		return result.get(0);
	}

	// StorageException is allowed by Storage.getReport(), hence no need to handle it in the lambda expression that will
	// call this method
	@SneakyThrows
	private static Report getReport(Integer storageId, Blob blob) {
		return Import.getReport(blob.getBinaryStream(), storageId, blob.length(), log);
	}

	@SneakyThrows
	private static Report getReport(Integer storageId, byte[] blob) {
		return Import.getReport(new ByteArrayInputStream(blob), storageId, (long) blob.length, log);
	}

	@Override
	public void update(Report report) throws StorageException {
		delete(report.getStorageId());
		store(report);
	}

	@Override
	public void delete(Report report) throws StorageException {
		delete(report.getStorageId());
	}

	private void delete(Integer storageId) throws StorageException {
		String query = "delete from " + getTable() + " where " + getStorageIdColumn() + " = ?";
		delete(query, storageId);
	}

	private void delete(String query, int i) throws StorageException {
		log.debug("Delete report query (with param value " + i + "): " + query);
		ladybugJdbcTemplate.update(query,
				new PreparedStatementSetter() {
					@Override
					public void setValues(PreparedStatement ps) throws SQLException {
						ps.setInt(1, i);
					}
				});
	}

	@Override
	public void clear() throws StorageException {
		throw new StorageException("Clear method is not implemented yet...");
	}

	@Override
	public int getSize() throws StorageException {
		StringBuilder query = new StringBuilder();
		buildSizeQuery(query);
		log.debug("Get size query: " + query);
		try {
			return ladybugJdbcTemplate.queryForObject(query.toString(), Integer.class);
		} catch(DataAccessException e){
			throw new StorageException("Could not read size", e);
		}
	}

	protected void buildSizeQuery(StringBuilder query) throws StorageException {
		query.append("select count(*) from " + getTable());
	}

	@Override
	public List<Integer> getStorageIds() throws StorageException {
		String query = getStorageIdsQuery();
		log.debug("Get storage id's query: " + query);
		try {
			List<Integer> storageIds = ladybugJdbcTemplate.query(query, (rs, rowNum) -> rs.getInt(1));
			return storageIds;
		} catch(DataAccessException e){
			throw new StorageException("Could not read storage id's", e);
		}
	}

	protected String getStorageIdsQuery() {
		return "select " + getStorageIdColumn() + " from " + getTable() + " order by " + getStorageIdColumn() + " desc";
	}

	@Override
	public List<List<Object>> getMetadata(int maxNumberOfRecords, List<String> metadataNames, List<String> searchValues,
			int metadataValueType) throws StorageException {
		// Prevent SQL injection (searchValues are passed as parameters to the SQL statement)
		for (String metadataName : metadataNames) {
			if (!getMetadataNames().contains(metadataName)) {
				throw new StorageException("Invalid metadata name: " + metadataName);
			}
		}
		List<String> rangeSearchValues = new ArrayList<String>();
		List<String> regexSearchValues = new ArrayList<String>();
		if (searchValues != null) {
			for (int i = 0; i < searchValues.size(); i++) {
				String searchValue = searchValues.get(i);
				if (searchValue != null && searchValue.startsWith("<") && searchValue.endsWith(">") && (
						isInteger(metadataNames.get(i))
						|| isLong(metadataNames.get(i))
						|| isTimestamp(metadataNames.get(i))
						)) {
					rangeSearchValues.add(searchValue);
					regexSearchValues.add(null);
					searchValues.remove(i);
					searchValues.add(i, null);
				} else if (searchValue != null && searchValue.startsWith("(") && searchValue.endsWith(")")) {
					rangeSearchValues.add(null);
					regexSearchValues.add(searchValue);
					searchValues.remove(i);
					searchValues.add(i, null);
				} else {
					rangeSearchValues.add(null);
					regexSearchValues.add(null);
				}
			}
		}
		StringBuilder query = new StringBuilder();
		List<Object> args = new ArrayList<Object>();
		List<Integer> argTypes = new ArrayList<Integer>();
		buildMetadataQuery(maxNumberOfRecords, metadataNames, searchValues, rangeSearchValues, query, args, argTypes);
		if (log.isDebugEnabled()) {
			log.debug("Get metadata query (with arguments: " + args + "): " + query.toString());
		}
		List<List<Object>> metadata;
		try {
			metadata = ladybugJdbcTemplate.query(query.toString(), args.toArray(),
					argTypes.stream().mapToInt(i -> i).toArray(),
					(rs, rowNum) ->
						{
							List<Object> row = new ArrayList<Object>();
							for (int i = 0; i < metadataNames.size(); i++) {
								Object value = null;
								if (isInteger(metadataNames.get(i))) {
									value = rs.getInt(i + 1);
								} else if (isLong(metadataNames.get(i))) {
									value = rs.getLong(i + 1);
								} else if (isTimestamp(metadataNames.get(i))) {
									value = rs.getTimestamp(i + 1).getTime();
								} else {
									value = rs.getString(i + 1);
								}
								row.add(metadataExtractor.fromObjectToMetadataValueType(metadataNames.get(i), value,
										metadataValueType));
							}
							return row;
						}
					);
		} catch(DataAccessException e){
			throw new StorageException("Could not read metadata", e);
		}
		postProcessMetadataResult(metadata, maxNumberOfRecords, metadataNames, searchValues, metadataValueType);
		if (searchValues != null) {
			for (int i = 0; i < metadata.size(); i++) {
				if (!SearchUtil.matches((List<Object>)metadata.get(i), regexSearchValues)) {
					metadata.remove(i);
					i--;
				}
			}
		}
		return metadata;
	}

	protected void buildMetadataQuery(int maxNumberOfRecords, List<String> metadataNames, List<String> searchValues,
			List<String> rangeSearchValues, StringBuilder query, List<Object> args, List<Integer> argTypes)
			throws StorageException {
		query.append("select" + dbmsSupport.provideFirstRowsHintAfterFirstKeyword(maxNumberOfRecords));
		String rowNumber = "";//dbmsSupport.getRowNumber(metadataNames.get(0), "desc");
		if (StringUtils.isNotEmpty(rowNumber)) {
			query.append(" * from (select");
		}
		boolean first = true;
		for (String metadataName : metadataNames) {
			if (first) {
				first = false;
			} else {
				query.append(",");
			}
			if (isBigValue(metadataName)) {
				query.append(" substr(" + metadataName + ", 1, 100)");
			} else {
				query.append(" " + metadataName);
			}
		}
		if (StringUtils.isNotEmpty(rowNumber)) {
			if (first) {
				first = false;
			} else {
				query.append(",");
			}
			query.append(" " + rowNumber);
		}
		query.append(" from " + getTable());
		// where
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
						if (isInteger(column) || isLong(column)) {
							addNumberExpression(query, args, argTypes, column, ">=", searchValueLeft);
						} else if (isTimestamp(column)) {
							addTimestampExpression(query, args, argTypes, column, ">=", searchValueLeft);
						}
					}
					if (StringUtils.isNotEmpty(searchValueRight)) {
						if (isInteger(column) || isLong(column)) {
							addNumberExpression(query, args, argTypes, column, "<=", searchValueRight);
						} else if (isTimestamp(column)) {
							addTimestampExpression(query, args, argTypes, column, "<=", searchValueRight);
						}
					}
				} else {
					throw new StorageException("Separator | not found");
				}
			}
		}
		if (searchValues != null) {
			for (int i = 0; i < searchValues.size(); i++) {
				String searchValue = searchValues.get(i);
				if (StringUtils.isNotEmpty(searchValue)) {
					String column = metadataNames.get(i);
					if (searchValue.equals("null")) {
						addExpression(query, column + " is null");
					} else if (isInteger(column)) {
						addNumberExpression(query, args, argTypes, column, "<=", searchValue);
					} else if (isTimestamp(column)) {
						addTimestampExpression(query, args, argTypes, column, "<=", searchValue);
					} else {
						addLikeOrEqualsExpression(query, args, argTypes, column, searchValue);
					}
				}
			}
		}
		if (query.charAt(query.length() - 1) == ' ') {
			// Clean up trailing space used by addExpression() to determine to add "where" or "and"
			query.deleteCharAt(query.length() - 1);
		}
		if (StringUtils.isNotEmpty(rowNumber)) {
			// An extra select was added earlier because rowNumber is not empty
			query.append(") where " + dbmsSupport.getRowNumberShortName() + " < ?");
			args.add(maxNumberOfRecords + 1);
			argTypes.add(Types.INTEGER);
		}
		query.append(" order by ");
		query.append(metadataNames.get(0) + " desc");
		query.append(dbmsSupport.provideTrailingFirstRowsHint(maxNumberOfRecords));
	}

	/*
	 * Override this method in a subclass when needed
	 */
	protected void postProcessMetadataResult(List<List<Object>> metadata, int maxNumberOfRecords,
			List<String> metadataNames, List<String> searchValues, int metadataValueType) {
	}

	@Override
	public void close() {
	}

	private void addLikeOrEqualsExpression(StringBuilder query, List<Object> args, List<Integer> argTypes,
			String column, String searchValue) throws StorageException {
		if (!(searchValue.startsWith("[") && searchValue.endsWith("]"))
				&& !(searchValue.startsWith("*") || searchValue.endsWith("*"))) {
			searchValue = "*" + searchValue + "*";
		}
		if (searchValue.contains("*")) {
			if (searchValue.startsWith("[[") && searchValue.endsWith("]]")) {
				addExpression(query, column + " like ?");
			} else {
				addExpression(query, "lower(" + column + ") like lower(?)");
			}
		} else {
			if (searchValue.startsWith("[[") && searchValue.endsWith("]]")) {
				addExpression(query, column + " = ?");
			} else {
				addExpression(query, "lower(" + column + ") = lower(?)");
			}
		}
		if (searchValue.startsWith("[") && searchValue.endsWith("]")) {
			searchValue = searchValue.substring(1, searchValue.length() - 1);
		}
		if (searchValue.startsWith("[") && searchValue.endsWith("]")) {
			searchValue = searchValue.substring(1, searchValue.length() - 1);
		}
		searchValue = searchValue.replaceAll("\\%", "\\\\%");
		searchValue = searchValue.replaceAll("\\_", "\\\\_");
		searchValue = searchValue.replaceAll("\\*", "%");
		args.add(searchValue);
		argTypes.add(Types.VARCHAR);
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
			String column, String operator, String searchValue) throws StorageException {
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
		long l = (Long)metadataExtractor.fromStringtoObject(column, searchValueToParse);
		args.add(new Timestamp(l));
		argTypes.add(Types.TIMESTAMP);
		addExpression(query, column + " " + operator + " ?");
	}

	private void throwExceptionOnInvalidTimestamp(String searchValue) throws StorageException {
		throw new StorageException("Search value '" + searchValue + "' doesn't comply with (the beginning of) pattern " + TIMESTAMP_PATTERN);
	}

	private void addExpression(StringBuilder query, String expression) {
		if (query.charAt(query.length() - 1) == ' ') {
			query.append("and " + expression + " ");
		} else {
			query.append(" where " + expression + " ");
		}
	}

	public boolean isInteger(String column) {
		return metadataExtractor.isInteger(column);
	}

	public boolean isLong(String column) {
		return metadataExtractor.isLong(column);
	}

	public boolean isTimestamp(String column) {
		return metadataExtractor.isTimestamp(column);
	}

	public boolean isBigValue(String column) {
		return getBigValueColumns().contains(column);
	}

	@Override
	public int getFilterType(String column) {
		return FILTER_RESET;
	}

	@Override
	public List<Object> getFilterValues(String column) throws StorageException {
		return null;
	}

	public String getUserHelp(String column) {
		String userHelpBase = SearchUtil.getUserHelpWildcards();
		String userHelpBaseNonString = "Search all rows which are less than or equal to the search value.";
		if (isInteger(column) || isLong(column)) {
			userHelpBase = userHelpBaseNonString
					+ " When the search value starts with < and ends with > a range search is done between and including the specified values separated by |.";
		} else if (isTimestamp(column)) {
			userHelpBase = userHelpBaseNonString
				+ " When the search value only complies with the beginning of pattern yyyy-MM-dd'T'HH:mm:ss.SSS, it will be supplemented with the end of 9999-12-31T23:59:59.999";
		}
		return userHelpBase + SearchUtil.getUserHelpRegex() + SearchUtil.getUserHelpNullAndEmpty();
	}

}
