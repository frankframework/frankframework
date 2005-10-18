/*
 * $Log: DirectQuerySender.java,v $
 * Revision 1.6  2005-10-18 07:10:12  europe\L190409
 * update javadoc
 *
 * Revision 1.5  2005/09/29 13:59:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * provided attributes and handling for nullValue,columnsReturned and resultQuery
 *
 * Revision 1.4  2005/09/07 15:37:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.3  2004/10/19 08:11:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified JavaDoc
 *
 * Revision 1.2  2004/03/26 10:43:09  Johan Verrips <johan.verrips@ibissource.org>
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
 * QuerySender that interprets the input message as a query.
 * Messages are expected to contain sql-text.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.DirectQuerySender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the sender</td><td>&nbsp;</td></tr>
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
 * <tr><td>{@link #setSynchronous(boolean) synchronous}</td><td>&nbsp;</td><td>true</td></tr>
 * <tr><td>{@link #setTrimSpaces(boolean) trimSpaces}</td><td>&nbsp;</td><td>true</td></tr>
 * <tr><td>{@link #setBlobsCompressed(boolean) blobsCompressed}</td><td>controls whether blobdata is stored compressed in the database</td><td>true</td></tr>
 * <tr><td>{@link #setResultQuery(String) resultQuery}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setColumnsReturned(String) columnsReturned}</td>comma separated list of columns whose values are to be returned. Works only if the driver implements JDBC 3.0 getGeneratedKeys()<td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class DirectQuerySender extends JdbcQuerySenderBase {
	public static final String version="$RCSfile: DirectQuerySender.java,v $ $Revision: 1.6 $ $Date: 2005-10-18 07:10:12 $";
	protected PreparedStatement getStatement(Connection con, String correlationID, String message) throws SQLException {
		String[] columnsReturned = getColumnsReturnedList();
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix() +"preparing statement for query ["+message+"]");
		}
		if (columnsReturned!=null) {
			return con.prepareStatement(message,columnsReturned);
		}
		return con.prepareStatement(message);
	}
}
