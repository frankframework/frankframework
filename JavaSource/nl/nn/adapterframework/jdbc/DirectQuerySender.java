/*
 * $Log: DirectQuerySender.java,v $
 * Revision 1.1  2004-03-24 13:28:20  L190409
 * initial version
 *
 */
package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


/**
 * QuerySender that interprets the input message as a query.
 * Messages are expected to contain sql-text.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.DirectQuerySender</td><td>&nbsp;</td></tr>
 * </table>
 * for further configuration options, see {@link JdbcQuerySenderBase}
 * </p>
 * 
 * <p>$Id: DirectQuerySender.java,v 1.1 2004-03-24 13:28:20 L190409 Exp $</p>
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class DirectQuerySender extends JdbcQuerySenderBase {
	public static final String version="$Id: DirectQuerySender.java,v 1.1 2004-03-24 13:28:20 L190409 Exp $";
	protected PreparedStatement getStatement(Connection con, String correlationID, String message) throws SQLException {
		return con.prepareStatement(message);
	}
}
