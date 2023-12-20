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
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.SuppressKeys;
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
 *     <h3>QueryType settings and OUTPUT parameters</h3>
 * The StoredProcedureQuerySender class has the following features:
 * <ul>
 *     <li>It supports setting the output parameters of the stored procedure by setting 'mode' attribute of
 *     the corresponding 'Param' to 'OUTPUT' or 'INOUT'.</li>
 *     <li>The queryType can only be 'SELECT' or 'OTHER'.</li>
 *     <li>Use queryType 'SELECT' when the stored procedure only returns a set of rows, and you need
 *     the output to be the format as {@link FixedQuerySender} (see {@link DB2XMLWriter}).</li>
 *     <li>Use queryType 'OTHER' if the stored procedure has one or more output parameters. With this query type,
 *     the stored procedure can return a result-set along with returning some values in output parameters.
 *     Depending on the database, the stored procedure can even returning multiple result sets or a combination
 *     of result sets as return values, and result sets as REF_CURSOR OUT parameters. </li>
 * </ul>
 * </p>
 * <p>
 *     All stored procedure parameters that are not fixed, so specified in the query with a {@code ?}, should
 *     have a corresponding {@link Parameter} entry. Output parameters should have {@code mode="OUTPUT"}, or
 *     {@code mode="INOUT"} depending on how the stored procedure is defined.
 * </p>
 * <p>
 *	<h3>Sample Output for queryType=OTHER</h3>
 *	<h4>Basic Example with Only Simple Output Parameters</h4>
 *  <code><pre>
	&lt;resultset&gt;
		&lt;result param="r1" type="STRING"&gt;MESSAGE-CONTENTS&lt;/result&gt;
		&lt;result param="r2" type="STRING"&gt;E&lt;/result&gt;
	&lt;/resultset&gt;
 *  </pre></code>
 *
 *	<h4>Example with Resultset and Simple Output Parameters</h4>
 *  <code><pre>
	 &lt;resultset&gt;
		 &lt;result resultNr="1"&gt;
			 &lt;fielddefinition&gt;
				&lt;field name="FIELDNAME"
						  type="columnType"
						  columnDisplaySize=""
						  precision=""
						  scale=""
						  isCurrency=""
						  columnTypeName=""
						  columnClassName=""/&gt;
				 &lt;field ...../&gt;
 		     &lt;/fielddefinition&gt;
			 &lt;rowset&gt;
				 &lt;row number="0"&gt;
					 &lt;field name="TKEY"&gt;MSG-ID&lt;/field&gt;
					 &lt;field name="TCHAR"&gt;E&lt;/field&gt;
					 &lt;field name="TMESSAGE"&gt;MESSAGE-CONTENTS&lt;/field&gt;
					 &lt;field name="TCLOB" null="true"/&gt;
					 &lt;field name="TBLOB" null="true"/&gt;
				 &lt;/row&gt;
                 &lt;row number="1" ...../&gt;
			 &lt;/rowset&gt;
		 &lt;/result&gt;
		 &lt;result param="count" type="INTEGER"&gt;5&lt;/result&gt;
	 &lt;/resultset&gt;
 *  </pre></code>
 *
 *	<h4>Example with Simple and Cursor Output Parameters</h4>
 *	<code><pre>
	&lt;resultset&gt;
		&lt;result param="count" type="INTEGER"&gt;5&lt;/result&gt;
		&lt;result param="cursor1" type="LIST"&gt;
			 &lt;fielddefinition&gt;
				&lt;field name="FIELDNAME"
						  type="columnType"
						  columnDisplaySize=""
						  precision=""
						  scale=""
						  isCurrency=""
						  columnTypeName=""
						  columnClassName=""/&gt;
				 &lt;field ...../&gt;
 		     &lt;/fielddefinition&gt;
			&lt;rowset&gt;
				&lt;row number="0"&gt;
					&lt;field name="TKEY"&gt;MSG-ID&lt;/field&gt;
					&lt;field name="TCHAR"&gt;E&lt;/field&gt;
					&lt;field name="TMESSAGE"&gt;MESSAGE-CONTENTS&lt;/field&gt;
					&lt;field name="TCLOB" null="true"/&gt;
					&lt;field name="TBLOB" null="true"/&gt;
				&lt;/row&gt;
				&lt;row number="1" ..... /&gt;
			&lt;/rowset&gt;
		&lt;/result&gt;
	&lt;/resultset&gt;
 *	</pre></code>
 * </p>
 * <p><em>NOTE:</em> Support for stored procedures is currently experimental and changes in the currently produced output-format
 * are expected.</p>
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

		if (getQueryTypeEnum() == QueryType.OTHER && getOutputFormat() != null) {
			ConfigurationWarnings.add(this, log, "When querytype is OTHER, the setting for outputFormat is currently ignored.", SuppressKeys.CONFIGURATION_VALIDATION);
		}

		super.configure();

		// Have to check this after "super.configure()" b/c otherwise the datasource-name is not set
		// and cannot check DMBS support features.
		if (getQueryTypeEnum() == QueryType.SELECT && !getDbmsSupport().isStoredProcedureResultSetSupported()) {
			throw new ConfigurationException("QueryType SELECT for Stored Procedures is not supported for database " + getDbmsSupport().getDbmsName());
		}

		outputParameters = buildOutputParameterMap(getParameterList(), getQuery());
		if (isScalar() && outputParameters.size() > 1) {
			ConfigurationWarnings.add(this, log, "When result should be scalar, only the first output parameter is used. Others are ignored.", SuppressKeys.CONFIGURATION_VALIDATION);
		}
		if (getQueryTypeEnum() == QueryType.SELECT && !outputParameters.isEmpty()) {
			ConfigurationWarnings.add(this, log, "OUT parameters are ignored when QueryType = SELECT", SuppressKeys.CONFIGURATION_VALIDATION);
		}

		if (!getQuery().matches("(?i)^\\s*(call|exec|\\{\\s*\\?(\\{\\w+\\})?\\s*=\\s*call)\\s+.*")) {
			ConfigurationWarnings.add(this, log, "Stored Procedure query should start with CALL or EXEC SQL statement", SuppressKeys.CONFIGURATION_VALIDATION);
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
