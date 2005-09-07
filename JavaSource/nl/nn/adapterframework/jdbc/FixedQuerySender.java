/*
 * $Log: FixedQuerySender.java,v $
 * Revision 1.6  2005-09-07 15:37:07  europe\L190409
 * updated javadoc
 *
 * Revision 1.5  2005/08/30 15:58:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected version-string
 *
 * Revision 1.4  2005/08/30 15:57:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added test-preparation in open(), but commented it out, as it didn't work
 *
 * Revision 1.3  2004/10/19 08:11:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified JavaDoc
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

/**
 * QuerySender that assumes a fixed query, possibly with attributes.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.FixedQuerySender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQuery(String) query}</td><td>the SQL query text to be excecuted each time sendMessage() is called</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDatasourceName(String) datasourceName}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDatasourceNameXA(String) datasourceNameXA}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUsername(String) username}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>&nbsp;</td><td>false</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQueryType(String) queryType}</td><td>either "select" for queries that return data, or anything else for queries that return no data.</td><td>"other"</td></tr>
 * <tr><td>{@link #setMaxRows(int) maxRows}</td><td>maximum number of rows returned</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setStartRow(int) startRow}</td><td>the number of the first row returned from the output</td><td>1</td></tr>
 * <tr><td>{@link #setScalar(boolean) scalar}</td><td>when true, the value of the first column of the first row (or the StartRow) is returned as the only result</td><td>false</td></tr>
 * </table>
 * </p>
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class FixedQuerySender extends JdbcQuerySenderBase {
	public static final String version = "$RCSfile: FixedQuerySender.java,v $ $Revision: 1.6 $ $Date: 2005-09-07 15:37:07 $";

	private String query=null;
		
	protected PreparedStatement getStatement(Connection con, String correlationID, String message) throws JdbcException, SQLException {
		return con.prepareStatement(getQuery());
	}

/*
	public void open() throws SenderException {
		super.open();
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix() +"test preparing statement for query ["+getQuery()+"]");
			Connection c = null;
			try {
				c = getConnection();
				PreparedStatement stmt = null;
				try {
					stmt = getStatement(c, null, null);
				} finally {
					if (stmt != null) {
						try {
							stmt.close();
						} catch (SQLException e) {
							log.warn(new SenderException(getLogPrefix() + "caught exception closing statement after test-preparing statement", e));
						}
					}
				}
			} catch (Exception e) {
				throw new SenderException(getLogPrefix()+"caught exception test-preparing query",e);
			} finally {
				if (c!=null) {
					try {
						c.close();
					} catch (SQLException e) {
						log.warn(new SenderException(getLogPrefix() + "caught exception closing connection after test-preparing statement", e));
					}
				}
			}
		}
	}
*/

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
