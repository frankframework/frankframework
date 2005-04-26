/*
 * $Log: JdbcQuerySenderBase.java,v $
 * Revision 1.8  2005-04-26 15:20:34  L190409
 * introduced JdbcSenderBase, with non-sql oriented basics
 *
 * Revision 1.7  2004/11/10 12:56:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected parameter setting routine
 *
 * Revision 1.6  2004/10/19 06:41:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified parameter handling
 *
 * Revision 1.5  2004/04/08 16:12:16  Dennis van Loon <dennis.van.loon@ibissource.org>
 * changed default value for maxRows to -1 (show All)
 *
 * Revision 1.4  2004/03/31 12:04:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed javadoc
 *
 * Revision 1.3  2004/03/26 10:43:07  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.2  2004/03/26 09:50:52  Johan Verrips <johan.verrips@ibissource.org>
 * Updated javadoc
 *
 * Revision 1.1  2004/03/24 13:28:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * initial version
 *
 */
package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.DB2XMLWriter;

/**
 * This executes the query that is obtained from the (here still abstract) method getStatement.
 * Descendent classes can override getStatement to provide meaningful statements.
 * If used with parameters, the values of the parameters will be applied to the statement. 
 * Each occurrence of a questionmark ('?') will be replaced by a parameter value. Parameters are applied
 * in order: The n-th questionmark is replaced by the value of the n-th parameter.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.JdbcQuerySenderBase</td><td>&nbsp;</td></tr>
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
 * </table>
 * </p>
 * 
 * Queries that return no data (queryType 'other') return a message indicating the number of rows processed
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public abstract class JdbcQuerySenderBase extends JdbcSenderBase {
	public static final String version="$Id: JdbcQuerySenderBase.java,v 1.8 2005-04-26 15:20:34 L190409 Exp $";

	private String queryType = "other";
	private int startRow=1;
	private int maxRows=-1; // return all rows

	/**
	 * returns <code>true</code> if the {@link #setQueryType(String) queryType} is set to "select".
	 * @see nl.nn.adapterframework.core.ISender#isSynchronous()
	 */
	public boolean isSynchronous() {
	   return getQueryType().equalsIgnoreCase("select");
	}
	
	/**
	 * Obtain a prepared statement to be executed.
	 * Method-stub to be overridden in descender-classes.
	 */
	protected abstract PreparedStatement getStatement(Connection con, String correlationID, String message) throws JdbcException, SQLException;



	protected void applyParameters(PreparedStatement statement, ParameterValueList parameters) throws SQLException {
		// statement.clearParameters();
		for (int i=0; i< parameters.size(); i++) {
			String parameterValue = (String)parameters.getParameterValue(i).getValue();
//			log.debug("applying parameter ["+(i+1)+","+parameters.getParameterValue(i).getDefinition().getName()+"], value["+parameterValue+"]");
			statement.setString(i+1, parameterValue);
		}
	}
	
	protected String sendMessage(Connection connection, String correlationID, String message, ParameterResolutionContext prc) throws SenderException{
		PreparedStatement statement;
		
		try {
			statement = getStatement(connection, correlationID, message);
			if (prc != null && paramList != null) {
				applyParameters(statement, prc.getValues(paramList));
			}
			if ("select".equalsIgnoreCase(getQueryType())) {
				return executeSelectQuery(statement);
			} else {
				int numRowsAffected = statement.executeUpdate();
				return "<result><rowsupdated>" + numRowsAffected + "</rowsupdated></result>";
			}
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix() + "got exception evaluating parameters", e);
		} catch (SQLException e) {
			throw new SenderException(getLogPrefix() + "got exception sending message", e);
		} catch (JdbcException e) {
			throw new SenderException(e);
		}
	}

	private String executeSelectQuery(PreparedStatement statement) throws SenderException{
		try {
			if (getMaxRows()>0) {
				statement.setMaxRows(getMaxRows()+ ( getStartRow()>1 ? getStartRow()-1 : 0));
			}

			ResultSet resultset = statement.executeQuery();
			
			if (getStartRow()>1) {
				resultset.absolute(getStartRow()-1);
				log.debug(getLogPrefix() + "Index set at position: " +  resultset.getRow() );
			}
					
			// Create XML and give the maxlength as a parameter
			DB2XMLWriter transformer = new DB2XMLWriter();
			return transformer.getXML(resultset, getMaxRows());
		} catch (SQLException sqle) {
			throw new SenderException(getLogPrefix() + "got exception executing a SELECT SQL command",sqle );
		}
	}
	
	
	/**
	 * Controls wheter output is expected from the query. 
	 * Possible values: 
	 * <ul>
	 * <li>select:</li> output is expected
	 * <li><i>anything else</i>:</li> no output is expected, the number of rows affected is returned
	 * </ul>
	 */
	public void setQueryType(String queryType) {
		this.queryType = queryType;
	}
	public String getQueryType() {
		return queryType;
	}

	/**
	 * Sets the maximum number of rows to be returned from the output of <code>select</code> queries.
	 * The default is 0, which will return all rows.
	 */
	public void setMaxRows(int i) {
		maxRows = i;
	}
	public int getMaxRows() {
		return maxRows;
	}

	/**
	 * Sets the number of the first row to be returned from the output of <code>select</code> queries.
	 * Rows before this are skipped from the output.
	 */
	public void setStartRow(int i) {
		startRow = i;
	}
	public int getStartRow() {
		return startRow;
	}


}
