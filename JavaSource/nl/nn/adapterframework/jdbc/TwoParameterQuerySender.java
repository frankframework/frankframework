/*
 * $Log: TwoParameterQuerySender.java,v $
 * Revision 1.4  2005-03-31 08:12:46  L190409
 * included deprecation warning
 *
 * Revision 1.3  2004/10/19 08:12:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made obsolete with introduction of generic parameter handling
 *
 * Revision 1.2  2004/03/26 10:43:08  Johan Verrips <johan.verrips@ibissource.org>
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

import nl.nn.adapterframework.configuration.ConfigurationException;

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
 * @deprecated Please use FixedQuerySender with nested {@link nl.nn.adapterframework.parameters.Parameter parameters} instead.
 */
public class TwoParameterQuerySender extends OneParameterQuerySender {
	public static final String version="$Id: TwoParameterQuerySender.java,v 1.4 2005-03-31 08:12:46 L190409 Exp $";

	public void configure() throws ConfigurationException {
		log.warn("Sender ["+getName()+"] is using class ["+getClass().getName()+"] which is deprecated. Please consider using [nl.nn.adapterframework.jdbc.FixedQuerySender] instead");
		super.configure();
	}


	protected PreparedStatement getStatement(Connection con, String correlationID, String message) throws JdbcException, SQLException {
		PreparedStatement stmt = super.getStatement(con, correlationID, message);
		stmt.setString(2,correlationID);
		return stmt;
	}
}
