/*
 * $Log: TwoParameterQuerySender.java,v $
 * Revision 1.2  2004-03-26 10:43:08  NNVZNL01#L180564
 * added @version tag in javadoc
 *
 * Revision 1.1  2004/03/24 13:28:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * initial version
 *
 */
package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * QuerySender that assumes a fixed query with two string parameter that are substituted with the message and messageId, respectively.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.TwoParameterQuerySender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQuery(String) query}</td><td>the SQL query text to be excecuted each time sendMessage() is called</td><td>&nbsp;</td></tr>
 * </table>
 * for further configuration options, see {@link JdbcQuerySenderBase}
 * </p>
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class TwoParameterQuerySender extends OneParameterQuerySender {
	public static final String version="$Id: TwoParameterQuerySender.java,v 1.2 2004-03-26 10:43:08 NNVZNL01#L180564 Exp $";

	protected PreparedStatement getStatement(Connection con, String correlationID, String message) throws JdbcException, SQLException {
		PreparedStatement stmt = super.getStatement(con, correlationID, message);
		stmt.setString(2,correlationID);
		return stmt;
	}
}
