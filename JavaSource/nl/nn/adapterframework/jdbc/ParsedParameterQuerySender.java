/*
 * $Log: ParsedParameterQuerySender.java,v $
 * Revision 1.1  2004-03-24 13:28:20  L190409
 * initial version
 *
 */
package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * QuerySender that accepts an XML string containing paramters for the query.
 *
 * Sample xml:<br/><code><pre>
 *	&lt;parameters&gt;
 *	    &lt;parameter type="string" value="woensdag" /&gt;
 *	    &lt;parameter type="int" value="14" /&gt;
 *	&lt;/parameters&gt;
 * </pre></code> <br/>
 * 
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.ParsedParameterQuerySender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQuery(String) query}</td><td>the SQL query text to be excecuted each time sendMessage() is called</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQueryParameterParserClassName(String) queryParameterParserClassName}</td><td>the class that parses the input message and sets parameters from it</td><td>nl.nn.adapterframework.jdbc.QueryParameterParser</td></tr>
 * </table>
 * for further configuration options, see {@link JdbcQuerySenderBase}
 * </p>
 * 
 * <p>$Id: ParsedParameterQuerySender.java,v 1.1 2004-03-24 13:28:20 L190409 Exp $</p>
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class ParsedParameterQuerySender extends FixedQuerySender {
	public static final String version="$Id: ParsedParameterQuerySender.java,v 1.1 2004-03-24 13:28:20 L190409 Exp $";
	private String queryParameterParserClassName="nl.nn.adapterframework.jdbc.QueryParameterParser";

	protected IQueryParameterParser createParameterParser(String queryParameterParserClassName) throws JdbcException {
		try {
			Class clazz = Class.forName(queryParameterParserClassName);
			return (IQueryParameterParser)clazz.getConstructor(null).newInstance(null);
		} catch (Exception e) {
			throw new JdbcException(getLogPrefix()+"cannot create for query parameter parser, class ["+queryParameterParserClassName+"]");
		}
	}

	protected void setParameters(PreparedStatement stmt, String correlationID, String message) throws JdbcException {
		IQueryParameterParser qpp = createParameterParser(getQueryParameterParserClassName());
		qpp.parse(stmt, correlationID,message);
	}

	protected PreparedStatement getStatement(Connection con, String correlationID, String message) throws JdbcException, SQLException {
		PreparedStatement stmt = super.getStatement(con, correlationID, message);
		setParameters(stmt, correlationID, message);
		return stmt;
	}

	/**
	 * Sets the classname of the QueryParameterParser; the corresponding class must implement nl.nn.adapterframework.jdbc.IQueryParameterParser.
	 */
	public void setQueryParameterParserClassName(String queryParameterParserClassName) {
		this.queryParameterParserClassName = queryParameterParserClassName;
	}
	public String getQueryParameterParserClassName() {
		return queryParameterParserClassName;
	}


}
