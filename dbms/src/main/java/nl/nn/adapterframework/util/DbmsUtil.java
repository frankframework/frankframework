package nl.nn.adapterframework.util;

import nl.nn.adapterframework.dbms.DbmsException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;



public class DbmsUtil {

	protected static Logger log = LogManager.getLogger(DbmsUtil.class);


	/**
	 * Executes query that returns a string. Returns {@literal null} if no results are found.
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
	 * @param connection The JDBC {@link Connection} on which to execute the query
	 * @param query      The SQL query, as string.
	 * @param params     The query parameters, see above.
	 * @return Query result as string, or {@literal  NULL}. The result is taken from only the first result-row, first column.
	 * @throws DbmsException if there is an error in query execution or parameter mapping
	 */
	public static String executeStringQuery(Connection connection, String query, Object... params) throws DbmsException {
		if (log.isDebugEnabled()) log.debug("prepare and execute query [" + query + "]" + displayParameters(params));
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			applyParameters(stmt, params);
			try (ResultSet rs = stmt.executeQuery()) {
				if (!rs.next()) {
					return null;
				}
				return rs.getString(1);
			}
		} catch (Exception e) {
			throw new DbmsException("could not obtain value using query [" + query + "]" + displayParameters(params), e);
		}
	}



	/**
	 * Executes query that returns an integer. Returns {@literal -1} if no results are found.
	 * Each object in the array is mapped to its most appropriate JDBC type, however not all types are supported. Column types are not considered,
	 * only the class of each parameter.
	 * TODO: Introduce a safer return-value than -1 for when no results are found!
	 * <p>
	 *     Supported Java types and JDBC Type mapping:
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
	 * @param connection The JDBC {@link Connection} on which to execute the query
	 * @param query      The SQL query, as string.
	 * @param params     The query parameters, see above.
	 * @return Query result as string, or {@literal  -1}. The result is taken from only the first result-row, first column.
	 * @throws DbmsException if there is an error in query execution or parameter mapping
	 */
	public static int executeIntQuery(Connection connection, String query, Object... params) throws DbmsException {
		if (log.isDebugEnabled()) log.debug("prepare and execute query [" + query + "]" + displayParameters(params));
		try (PreparedStatement stmt = connection.prepareStatement(query)) {
			applyParameters(stmt, params);
			try (ResultSet rs = stmt.executeQuery()) {
				if (!rs.next()) {
					return -1;
				}
				return rs.getInt(1);
			}
		} catch (Exception e) {
			throw new DbmsException("could not obtain value using query [" + query + "]" + displayParameters(params), e);
		}
	}


	/**
	 * Applies parameters to a PreparedStatement.
	 * Each object in the array is mapped to its most appropriate JDBC type, however not all types are supported. Column types are not considered,
	 * only the class of each parameter.
	 * <p>
	 *     Supported Java types and JDBC Type mapping:
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
	 * @param stmt    the PreparedStatement to apply parameters to
	 * @param params  the parameters to apply
	 * @throws SQLException if there is an error applying the parameters
	 */
	public static void applyParameters(PreparedStatement stmt, Object... params) throws SQLException {
		for (int i = 0; i < params.length; i++) {
			Object param = params[i];
			if (param == null) continue;

			int sqlType = deriveSqlType(param);
			stmt.setObject(i + 1, param, sqlType);
		}
	}

	private static int deriveSqlType(final Object param) {
		// NB: So far this is not exhaustive, but previously only INTEGER and VARCHAR were supported, so for now this should do.
		int sqlType;
		if (param instanceof Integer) {
			sqlType = Types.INTEGER;
		} else if (param instanceof Long) {
			sqlType = Types.BIGINT;
		} else if (param instanceof Float) {
			sqlType = Types.NUMERIC;
		} else if (param instanceof Double) {
			sqlType = Types.NUMERIC;
		} else if (param instanceof Timestamp) {
			sqlType = Types.TIMESTAMP;
		} else if (param instanceof Time) {
			sqlType = Types.TIME;
		} else if (param instanceof java.sql.Date) {
			sqlType = Types.DATE;
		} else {
			sqlType = Types.VARCHAR;
		}
		return sqlType;
	}

	public static String displayParameters(Object... params) {
		StringBuilder sb = new StringBuilder(1024);
		for (int i = 0; i < params.length; i++) {
			sb.append(" param").append(i + 1).append(" [").append(params[i]).append("]");
		}
		return sb.toString();
	}
}
