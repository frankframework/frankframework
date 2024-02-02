package org.frankframework.testutil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.frankframework.core.PipeLineSession;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.dbms.JdbcException;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.util.DB2XMLWriter;
import org.frankframework.util.JdbcUtil;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class JdbcTestUtil {

	/**
	 * Executes the given SQL statement. The statement can be any SQL statement that does not return a result set.
	 * Each object in the array is mapped to its most appropriate JDBC type, however not all types are supported. Column types are not considered,
	 * only the class of each parameter.
	 * <p>
	 * Supported Java types and JDBC Type mapping:
	 *     <table>
	 *         <tr><th>{@link java.lang.Integer}</th> <td>{@link Types#INTEGER}</td></tr>
	 *         <tr><th>{@link java.lang.Long}</th> <td>{@link Types#BIGINT}</td></tr>
	 *         <tr><th>{@link java.lang.Float}</th> <td>{@link Types#NUMERIC}</td></tr>
	 *         <tr><th>{@link java.lang.Double}</th> <td>{@link Types#NUMERIC}</td></tr>
	 *         <tr><th>{@link java.sql.Timestamp}</th> <td>{@link Types#TIMESTAMP}</td></tr>
	 *         <tr><th>{@link java.sql.Time}</th> <td>{@link Types#TIME}</td></tr>
	 *         <tr><th>{@link java.sql.Date}</th> <td>{@link Types#DATE}</td></tr>
	 *         <tr><th>{@link java.lang.String}</th> <td>{@link Types#VARCHAR}</td></tr>
	 *     </table>
	 * </p>
	 *
	 * @param connection The JDBC {@link Connection} on which to execute the statement.
	 * @param query      The SQL statement, as a string.
	 * @param params     The statement parameters, see above.
	 * @throws JdbcException if there is an error in statement execution or parameter mapping.
	 */
	public static void executeStatement(Connection connection, String query, Object... params) throws JdbcException {
		log.debug("prepare and execute query [" + query + "]" + org.frankframework.util.DbmsUtil.displayParameters(params));
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			org.frankframework.util.DbmsUtil.applyParameters(stmt, params);
			stmt.execute();
		} catch (Exception e) {
			throw new JdbcException("could not execute query [" + query + "]" + org.frankframework.util.DbmsUtil.displayParameters(params), e);
		}
	}

	public static String selectAllFromTable(IDbmsSupport dbmsSupport, Connection conn, String tableName, String orderBy) throws SQLException {
		String query = "select * from " + tableName + (orderBy != null ? " ORDER BY " + orderBy : "");
		try (PreparedStatement stmt = conn.prepareStatement(query)) {
			try (ResultSet rs = stmt.executeQuery()) {
				DB2XMLWriter db2xml = new DB2XMLWriter();
				return db2xml.getXML(dbmsSupport, rs);
			}
		}
	}

	public static void executeStatement(IDbmsSupport dbmsSupport, Connection connection, String query, ParameterValueList parameterValues, PipeLineSession session) throws JdbcException {
		log.debug("prepare and execute query [" + query + "]" + displayParameters(parameterValues));
		try {
			PreparedStatement stmt = connection.prepareStatement(query);
			JdbcUtil.applyParameters(dbmsSupport, stmt, parameterValues, session);
			stmt.execute();
		} catch (Exception e) {
			throw new JdbcException("could not execute query [" + query + "]" + displayParameters(parameterValues), e);
		}
	}

	public static Object executeQuery(IDbmsSupport dbmsSupport, Connection connection, String query, ParameterValueList parameterValues, PipeLineSession session) throws JdbcException {
		JdbcTestUtil.log.debug("prepare and execute query [" + query + "]" + displayParameters(parameterValues));
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			JdbcUtil.applyParameters(dbmsSupport, stmt, parameterValues, session);
			try (ResultSet rs = stmt.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				int columnsCount = rs.getMetaData().getColumnCount();
				if (columnsCount == 1) {
					return rs.getObject(1);
				}
				List<Object> resultList = new ArrayList<>();
				for (int i = 1; i <= columnsCount; i++) {
					resultList.add(rs.getObject(i));
				}
				return resultList;
			}
		} catch (Exception e) {
			throw new JdbcException("could not obtain value using query [" + query + "]" + displayParameters(parameterValues), e);
		}
	}

	public static String displayParameters(ParameterValueList parameterValues) {
		if (parameterValues == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < parameterValues.size(); i++) {
			sb.append("param").append(i)
					.append(" [")
					.append(parameterValues.getParameterValue(i).getValue())
					.append("]");
		}
		return sb.toString();
	}
}
