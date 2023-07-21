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

import javax.jms.JMSException;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.ParameterList;
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
	private int[] outputParameterPositions = new int[0];


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
		super.configure();
		if (outputParameters != null) {
			outputParameterPositions = StringUtil.splitToStream(outputParameters, ",;")
					.mapToInt(Integer::parseInt)
					.toArray();
		}

		if (isScalar() && outputParameterPositions.length > 1) {
			throw new ConfigurationException("When result should be scalar, only a single output can be returned from the stored procedure.");
		}
	}

	@Override
	protected PreparedStatement prepareQueryWithResultSet(Connection con, String query, int resultSetConcurrency) throws SQLException {
		final CallableStatement callableStatement = con.prepareCall(query, ResultSet.TYPE_FORWARD_ONLY, resultSetConcurrency);
		if (outputParameterPositions.length > 0) {
			// TODO: This does not work with Oracle -- "SQLFeatureNotSupportedException" :'(
			final ParameterMetaData parameterMetaData = callableStatement.getParameterMetaData();
			for (int param : outputParameterPositions) {
				// Not all drivers support JDBCType (for instance, PostgreSQL) so use the type number
				// For some databases (PostgreSQL) the value should already be set when registering out-parameter.
				callableStatement.setNull(param, parameterMetaData.getParameterType(param));
				callableStatement.registerOutParameter(param, parameterMetaData.getParameterType(param));
			}
		}
		return callableStatement;
	}

	@Override
	protected PreparedStatement prepareQueryWithColumnsReturned(Connection con, String query, String[] columnsReturned) throws SQLException {
		throw new IllegalArgumentException("Stored Procedures do not support 'columnsReturned', specify outputParameters");
	}

	@Override
	protected Message executeOtherQuery(Connection connection, PreparedStatement statement, String query, PreparedStatement resStmt, Message message, PipeLineSession session, ParameterList parameterList) throws SenderException {
		Message result = super.executeOtherQuery(connection, statement, query, resStmt, message, session, parameterList);
		if (outputParameterPositions.length == 0) {
			return result;
		}
		try {
			return getResult(new StoredProcedureResultWrapper((CallableStatement) statement, statement.getParameterMetaData(), outputParameterPositions));
		} catch (JdbcException | JMSException | IOException | SQLException e) {
			throw new SenderException(e);
		}
	}

}
