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
package nl.nn.adapterframework.jdbc;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jms.JMSException;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.dbms.JdbcException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.pipes.Base64Pipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.DB2XMLWriter;
import nl.nn.adapterframework.util.JdbcUtil;

/**
 * StoredProcedureQuerySender is used to send stored procedure queries and retrieve the result.
 *
 * <p>
 * The StoredProcedureQuerySender class has the following features:
 * <ul>
 *     <li>It supports setting the output parameters of the stored procedure by setting 'mode' attribute of
 *     the corresponding 'Param' to 'OUTPUT' or 'INOUT'.</li>
 *     <li>The queryType can only be 'SELECT' or 'OTHER'. Use 'SELECT' when the stored procedure
 *     returns a set of rows, use 'OTHER' if the stored procedure has one or more output parameters.
 *  </li>
 * </ul>
 * </p>
 * <p>
 *     All stored procedure parameters that are not fixed, so specified in the query with a {@code ?}, should
 *     have a corresponding {@link Parameter} entry. Output parameters should have {@code mode="OUTPUT"}, or
 *     {@code mode="INOUT"} depending on how the stored procedure is defined.
 * </p>
 * <p><b>NOTE:</b> See {@link DB2XMLWriter} for ResultSet!</p>
 *
 * @ff.parameters All parameters present are applied to the query to be executed.
 *
 * @since 7.9
 */
public class StoredProcedureQuerySender extends FixedQuerySender {

	/**
	 * All stored procedure OUT parameters indexed by their position
	 * in the query parameter list (1-based).
	 */
	private Map<Integer, Parameter> outputParameters;

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isNotBlank(getColumnsReturned())) {
			throw new ConfigurationException("Cannot use 'columnsReturned' with StoredProcedureSender, use 'outputParameters' instead.");
		}
		if (getQueryTypeEnum() != QueryType.SELECT && getQueryTypeEnum() != QueryType.OTHER) {
			throw new ConfigurationException("For StoredProcedureSender, queryType can only be 'SELECT' or 'OTHER'");
		}
		if (getQueryTypeEnum() == QueryType.SELECT && !getDbmsSupport().isStoredProcedureResultSetSupported()) {
			throw new ConfigurationException("QueryType SELECT for Stored Procedures is not supported for database " + getDbmsSupport().getDbmsName());
		}

		super.configure();

		// Have to check this after "super.configure()" b/c otherwise the datasource-name is not set
		// and cannot check DMBS support features.
		if (!getDbmsSupport().isStoredProceduresSupported()) {
			throw new ConfigurationException("Stored Procedures are not supported for database " + getDbmsSupport().getDbmsName());
		}

		outputParameters = buildOutputParameterMap(getParameterList(), getQuery());
		if (!outputParameters.isEmpty() && (!getDbmsSupport().isStoredProcedureOutParametersSupported())) {
			throw new ConfigurationException("Stored Procedure OUT parameters are not supported for database " + getDbmsSupport().getDbmsName());
		}
		boolean hasCursorOutParam = outputParameters.values().stream()
				.anyMatch(p -> p.getType() == Parameter.ParameterType.LIST);
		if (hasCursorOutParam && !getDbmsSupport().isStoredProcedureRefCursorOutParameterSupported()) {
			throw new ConfigurationException("Cursor or Ref Cursor OUT parameters for stored procedures are not supported for " + getDbmsSupport().getDbmsName());
		}

		if (isScalar() && outputParameters.size() > 1) {
			throw new ConfigurationException("When result should be scalar, only a single output can be returned from the stored procedure.");
		}

		if (!getQuery().matches("(?i)^\\s*(call|exec|\\{\\s*\\?(\\{\\w+\\})?\\s*=\\s*call)\\s+.*")) {
			throw new ConfigurationException("Stored Procedure query should start with CALL or EXEC SQL statement");
		}
	}

	/**
	 * Build a map of all output-parameters for the stored procedure, indexed by
	 * their parameter-position (1-based) in the parameter list of the call.
	 *
	 * @param parameterList Full list of all parameters configured on the StoredProcedureSender.
	 * @param query The query that is configured
	 * @return Output-parameters indexed by position in the query parameter-list.
	 */
	private Map<Integer, Parameter> buildOutputParameterMap(ParameterList parameterList, String query) {
		if (parameterList == null) {
			return Collections.emptyMap();
		}
		Pattern queryParamPattern = Pattern.compile("\\?(\\{\\w+\\})?");
		Matcher parameterMatcher = queryParamPattern.matcher(query);
		List<String> queryParameterNames = new ArrayList<>();
		while (parameterMatcher.find()) {
			queryParameterNames.add(parameterMatcher.group(1));
		}
		Map<String, Integer> queryParameterMap = new HashMap<>();
		for (int i = 0; i < queryParameterNames.size(); i++) {
			queryParameterMap.put(queryParameterNames.get(i), i+1);
		}

		Map<Integer, Parameter> result = new HashMap<>();
		int pos = 0;
		for (Parameter param : parameterList) {
			++pos;

			if (param.getMode() == Parameter.ParameterMode.INPUT) {
				continue;
			}

			result.put(queryParameterMap.getOrDefault(param.getName(), pos), param);
		}

		return result;
	}

	@Override
	protected PreparedStatement prepareQueryWithResultSet(Connection con, String query, int resultSetConcurrency) throws SQLException {
		final CallableStatement callableStatement = con.prepareCall(query, ResultSet.TYPE_FORWARD_ONLY, resultSetConcurrency);
		ParameterMetaData parameterMetaData = callableStatement.getParameterMetaData();
		for (Map.Entry<Integer, Parameter> entry : outputParameters.entrySet()) {
			final int position = entry.getKey();
			final Parameter param = entry.getValue();
			final int typeNr;
			// Parameter metadata are more accurate than our parameter type mapping and
			// for some databases, this can cause exceptions.
			// But for Oracle we do need our own mapping.
			if (getDbmsSupport().canFetchStatementParameterMetaData() && param.getType() != Parameter.ParameterType.LIST) {
				typeNr = parameterMetaData.getParameterType(position);
			} else {
				typeNr = JdbcUtil.mapParameterTypeToSqlType(getDbmsSupport(), param.getType()).getVendorTypeNumber();
			}
			callableStatement.registerOutParameter(position, typeNr);
		}
		return callableStatement;
	}

	@Override
	protected PreparedStatement prepareQueryWithColumnsReturned(Connection con, String query, String[] columnsReturned) throws SQLException {
		throw new IllegalArgumentException("Stored Procedures do not support 'columnsReturned', specify outputParameters");
	}

	@Override
	protected Message executeOtherQuery(Connection connection, PreparedStatement statement, String query, String resultQuery, PreparedStatement resStmt, Message message, PipeLineSession session, ParameterList parameterList) throws SenderException {
		try {
			CallableStatement callableStatement = (CallableStatement) statement;
			boolean alsoGetResultSets = callableStatement.execute();
			return getResult(callableStatement, alsoGetResultSets, resultQuery, resStmt);
		} catch (JdbcException | JMSException | IOException | SQLException e) {
			throw new SenderException(e);
		}
	}

	private Message getResult(CallableStatement callableStatement, boolean alsoGetResultSets, String resultQuery, PreparedStatement resStmt) throws SQLException, JMSException, IOException, JdbcException {
		int updateCount = callableStatement.getUpdateCount();
		if (resStmt != null || outputParameters.isEmpty() && (!alsoGetResultSets || updateCount != -1)) {
			return getUpdateStatementResult(callableStatement, resultQuery, resStmt, updateCount);
		}
		if (isScalar() || isScalarExtended()) {
			return getResult(new StoredProcedureResultWrapper(getDbmsSupport(), callableStatement, callableStatement.getParameterMetaData(), outputParameters));
		}

		DB2XMLWriter db2xml = buildDb2XMLWriter();
		String result = db2xml.getXML(getDbmsSupport(), callableStatement, alsoGetResultSets, outputParameters, getMaxRows(), isIncludeFieldDefinition());
		return Message.asMessage(result);
	}

	/**
	 * A SQL statement that calls a stored procedure. The statement should begin with the <code>CALL</code> or <code>EXEC</code>
	 * SQL keyword depending on SQL dialect. In case of doubt, the safe choice is to always start with <code>CALL</code> and choose Oracle dialect.
	 *
	 * @param query The SQL statement to invoke the stored procedure.
	 */
	@Override
	public void setQuery(final String query) {
		super.setQuery(query);
	}

	/**
	 * The query type. For stored procedures, valid query types are {@link JdbcQuerySenderBase.QueryType#SELECT} and {@link JdbcQuerySenderBase.QueryType#OTHER}.
	 * Use {@link JdbcQuerySenderBase.QueryType#SELECT} when your stored procedure returns a row set (not supported by Oracle and PostgreSQL).
	 * Use {@link JdbcQuerySenderBase.QueryType#OTHER} when your stored procedure returns values via <code>OUT</code> or <code>INOUT</code> parameters, or does not return
	 * anything at all.
	 * <p>
	 * Using any other value will be rejected.
	 * </p>
	 *
	 * @param queryType The queryType.
	 */
	@Override
	public void setQueryType(final String queryType) {
		super.setQueryType(queryType);
	}

	/**
	 * This feature is not supported for StoredProcedureQuerySender.
	 *
	 * @ff.protected
	 */
	@Override
	public void setColumnsReturned(final String string) {
		super.setColumnsReturned(string);
	}

	/**
	 * This feature is not supported for StoredProcedureQuerySender.
	 *
	 * @ff.protected
	 */
	@Override
	public void setLockRows(final boolean b) {
		super.setLockRows(b);
	}

	/**
	 * This feature is not supported for StoredProcedureQuerySender.
	 *
	 * @ff.protected
	 */
	@Override
	public void setLockWait(final int i) {
		super.setLockWait(i);
	}

	/**
	 * This feature is not supported for StoredProcedureQuerySender.
	 *
	 * @ff.protected
	 */
	@Override
	public void setBlobColumn(final int i) {
		super.setBlobColumn(i);
	}

	/**
	 * This feature is not supported for StoredProcedureQuerySender.
	 *
	 * @ff.protected
	 */
	@Override
	public void setBlobSessionKey(final String string) {
		super.setBlobSessionKey(string);
	}

	/**
	 * This feature is not supported for StoredProcedureQuerySender.
	 *
	 * @ff.protected
	 */
	@Override
	public void setBlobsCompressed(final boolean b) {
		super.setBlobsCompressed(b);
	}

	/**
	 * This feature is not supported for StoredProcedureQuerySender.
	 *
	 * @ff.protected
	 */
	@Override
	public void setBlobBase64Direction(final Base64Pipe.Direction value) {
		super.setBlobBase64Direction(value);
	}

	/**
	 * This feature is not supported for StoredProcedureQuerySender.
	 *
	 * @ff.protected
	 */
	@Override
	public void setBlobCharset(final String string) {
		super.setBlobCharset(string);
	}

	/**
	 * This feature is not supported for StoredProcedureQuerySender.
	 *
	 * @ff.protected
	 */
	@Override
	public void setBlobSmartGet(final boolean b) {
		super.setBlobSmartGet(b);
	}

	/**
	 * This feature is not supported for StoredProcedureQuerySender.
	 *
	 * @ff.protected
	 */
	@Override
	public void setClobColumn(final int i) {
		super.setClobColumn(i);
	}

	/**
	 * This feature is not supported for StoredProcedureQuerySender.
	 *
	 * @ff.protected
	 */
	@Override
	public void setClobSessionKey(final String string) {
		super.setClobSessionKey(string);
	}

	/**
	 * This feature is not supported for StoredProcedureQuerySender.
	 *
	 * @ff.protected
	 */
	@Override
	public void setAvoidLocking(final boolean avoidLocking) {
		super.setAvoidLocking(avoidLocking);
	}

	/**
	 * This feature is not supported for StoredProcedureQuerySender.
	 *
	 * @ff.protected
	 */
	@Override
	public void setMaxRows(final int i) {
		super.setMaxRows(i);
	}

	/**
	 * This feature is not supported for StoredProcedureQuerySender.
	 *
	 * @ff.protected
	 */
	@Override
	public void setStartRow(final int i) {
		super.setStartRow(i);
	}

	/**
	 * This feature is not supported for StoredProcedureQuerySender.
	 *
	 * @ff.protected
	 */
	@Override
	public void setRowIdSessionKey(final String string) {
		super.setRowIdSessionKey(string);
	}

}
