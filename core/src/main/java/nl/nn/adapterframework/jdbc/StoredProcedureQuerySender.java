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
import nl.nn.adapterframework.util.StringUtil;

public class StoredProcedureQuerySender extends FixedQuerySender {

	private @Getter String outputParameters;
	private int[] outputParameterPositions = new int[0];


	/**
	 * Sets the output parameters of the store procedure, separated by commas.
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
		final ParameterMetaData parameterMetaData = callableStatement.getParameterMetaData();
		for (int param : outputParameterPositions) {
			// Not all drivers support JDBCType (for instance, PostgreSQL) so use the type number
			// For some databases (PostgreSQL) the value should already be set when registering out-parameter.
			callableStatement.setNull(param, parameterMetaData.getParameterType(param));
			callableStatement.registerOutParameter(param, parameterMetaData.getParameterType(param));
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
