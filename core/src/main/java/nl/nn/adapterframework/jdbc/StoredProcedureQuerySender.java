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
import java.sql.JDBCType;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jms.JMSException;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.pipes.Base64Pipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.DB2XMLWriter;
import nl.nn.adapterframework.util.StringUtil;

/**
 * StoredProcedureQuerySender is used to send stored procedure queries and retrieve the result.
 *
 * <p>
 * The StoredProcedureQuerySender class has the following features:
 * <ul>
 *     <li>It supports setting the output parameters of the stored procedure by position.</li>
 *     <li>The queryType can only be 'SELECT' or 'OTHER'. Use 'SELECT' when the stored procedure
 *     returns a set of rows, use 'OTHER' if the stored procedure has one or more output parameters.
 *  </li>
 * </ul>
 * </p>
 * <p><b>NOTE:</b> See {@link DB2XMLWriter} for ResultSet!</p>
 *
 * @ff.parameters All parameters present are applied to the query to be executed.
 *
 * @since 7.9
 */
public class StoredProcedureQuerySender extends FixedQuerySender {

	private @Getter String outputParameters;
	private StoredProcedureParamDef[] outputParameterDefs = new StoredProcedureParamDef[0];


	/**
	 * Sets the output parameters of the store procedure, separated by commas. Each output parameter of the stored
	 * procedure should be specified by its index in the parameter list of the SQL, with the first parameter being
	 * number 1.
	 * <p>
	 *     Example:<br/>
	 *     If there is a stored procedure {@code get_message_and_status_by_id} with a single input parameter, the message id,
	 *     and two output parameters, the message and the message status, then the query should be specified as:
	 *     <code>call get_message_and_status_by_id(?, ?, ?)</code>,
	 *     and {@code outputParameters} should be:
	 *     <code>outputParameters="2,3"</code>
	 * </p>
	 *
	 * @param outputParameters the output parameters to be set
	 */
	public void setOutputParameters(String outputParameters) {
		this.outputParameters = outputParameters;
	}


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

		if (outputParameters != null) {
			if (!getDbmsSupport().isStoredProcedureOutParametersSupported()) {
				throw new ConfigurationException("Stored Procedure OUT parameters are not supported for database " + getDbmsSupport().getDbmsName());
			}
			outputParameterDefs = parseOutParameters(getQuery(), outputParameters);
			if (!getDbmsSupport().canFetchStatementParameterMetaData()) {
				if (!Arrays.stream(outputParameterDefs).allMatch(def -> def.getType() != null)) {
					throw new ConfigurationException("Target database " + getDbmsSupport().getDbmsName() + " requires types of all output parameters to be specified.");
				}
			}
		}

		if (isScalar() && outputParameterDefs.length > 1) {
			throw new ConfigurationException("When result should be scalar, only a single output can be returned from the stored procedure.");
		}

		if (!getQuery().matches("(?i)^\\s*(call|exec|\\{\\s*\\?(\\{\\w+\\})?\\s*=\\s*call)\\s+.*")) {
			throw new ConfigurationException("Stored Procedure query should start with CALL or EXEC SQL statement");
		}
	}

	private StoredProcedureParamDef[] parseOutParameters(String query, String outParamSpec) {
		Pattern queryParamPattern = Pattern.compile("\\?(\\{\\w+\\})?");
		Matcher parameterMatcher = queryParamPattern.matcher(query);
		List<String> queryParameterNames = new ArrayList<>();
		while (parameterMatcher.find()) {
			queryParameterNames.add(parameterMatcher.group(1));
		}

		return StringUtil.splitToStream(outParamSpec, ",;")
				.map(p -> StringUtil.split(p, ":"))
				.map(pl -> {
					int pos = Integer.parseInt(pl.get(0));
					String name = queryParameterNames.get(pos-1);
					JDBCType type;
					if (pl.size() == 1) {
						type = null;
					} else {
						type = JDBCType.valueOf(pl.get(1));
					}
					if (name == null) {
						return new StoredProcedureParamDef(pos, type);
					}
					return new StoredProcedureParamDef(pos, type, name);
				})
				.toArray(StoredProcedureParamDef[]::new);
	}

	@Override
	protected PreparedStatement prepareQueryWithResultSet(Connection con, String query, int resultSetConcurrency) throws SQLException {
		final CallableStatement callableStatement = con.prepareCall(query, ResultSet.TYPE_FORWARD_ONLY, resultSetConcurrency);
		if (outputParameterDefs.length > 0) {
			// TODO: This does not work with Oracle -- "SQLFeatureNotSupportedException" :'(
			final ParameterMetaData parameterMetaData;
			if (getDbmsSupport().canFetchStatementParameterMetaData()) {
				parameterMetaData = callableStatement.getParameterMetaData();
			} else {
				parameterMetaData = null;
			}
			for (StoredProcedureParamDef param : outputParameterDefs) {
				// Not all drivers support JDBCType (for instance, PostgreSQL) so use the type number
				// For some databases (PostgreSQL) the value should already be set when registering out-parameter.
				int position = param.getPosition();
				int typeNr;
				if (param.getType() != null) {
					typeNr = param.getType().getVendorTypeNumber();
				} else if (parameterMetaData != null) {
					typeNr = parameterMetaData.getParameterType(position);
				} else {
					throw new IllegalStateException("Cannot determine parameter type for parameter nr " + position);
				}
				callableStatement.setNull(position, typeNr);
				callableStatement.registerOutParameter(position, typeNr);
			}
		}
		return callableStatement;
	}

	@Override
	protected PreparedStatement prepareQueryWithColumnsReturned(Connection con, String query, String[] columnsReturned) throws SQLException {
		throw new IllegalArgumentException("Stored Procedures do not support 'columnsReturned', specify outputParameters");
	}

	@Override
	protected Message executeOtherQuery(Connection connection, PreparedStatement statement, String query, String resultQuery, PreparedStatement resStmt, Message message, PipeLineSession session, ParameterList parameterList) throws SenderException {
		Message result = super.executeOtherQuery(connection, statement, query, resultQuery, resStmt, message, session, parameterList);
		if (outputParameterDefs.length == 0) {
			return result;
		}
		try {
			ParameterMetaData parameterMetaData;
			if (getDbmsSupport().canFetchStatementParameterMetaData()) {
				parameterMetaData = statement.getParameterMetaData();
			} else {
				parameterMetaData = null;
			}
			return getResult(new StoredProcedureResultWrapper((CallableStatement) statement, parameterMetaData, outputParameterDefs));
		} catch (JdbcException | JMSException | IOException | SQLException e) {
			throw new SenderException(e);
		}
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
