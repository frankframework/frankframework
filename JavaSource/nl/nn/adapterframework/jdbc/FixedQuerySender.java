/*
 * $Log: FixedQuerySender.java,v $
 * Revision 1.1  2004-03-24 13:28:20  L190409
 * initial version
 *
 */
package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * QuerySender that assumes a fixed query without any parameters.
 * Can be used as a baseclass for parameterized queries.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.FixedQuerySender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQuery(String) query}</td><td>the SQL query text to be excecuted each time sendMessage() is called</td><td>&nbsp;</td></tr>
 * </table>
 * for further configuration options, see {@link JdbcQuerySenderBase}
 * </p>
 * 
 * <p>$Id: FixedQuerySender.java,v 1.1 2004-03-24 13:28:20 L190409 Exp $</p>
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class FixedQuerySender extends JdbcQuerySenderBase {
	public static final String version="$Id: FixedQuerySender.java,v 1.1 2004-03-24 13:28:20 L190409 Exp $";

	private String query=null;
		
	protected PreparedStatement getStatement(Connection con, String correlationID, String message) throws JdbcException, SQLException {
		return con.prepareStatement(getQuery());
	}

	/**
	 * Sets the SQL-query text to be executed each time sendMessage() is called.
	 * @param query
	 */
	public void setQuery(String query) {
		this.query = query;
	}
	public String getQuery() {
		return query;
	}

}
