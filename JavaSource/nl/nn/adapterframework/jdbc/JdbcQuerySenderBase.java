/*
 * $Log: JdbcQuerySenderBase.java,v $
 * Revision 1.14  2005-09-08 16:00:52  europe\L190409
 * make synchronous an attribute, default="true"
 *
 * Revision 1.13  2005/08/25 15:48:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * close all jdbc-objects in finally clause
 *
 * Revision 1.12  2005/07/28 07:33:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * close statement also for update-statements
 *
 * Revision 1.11  2005/07/19 12:36:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved applyParameters to JdbcFacade
 *
 * Revision 1.10  2005/06/28 09:05:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * explicit closing of resultset
 *
 * Revision 1.9  2005/06/02 13:48:16  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added 'scalar' attribute, to return a single value
 *
 * Revision 1.8  2005/04/26 15:20:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
 * <tr><td>{@link #setConnectionsArePooled(boolean) connectionsArePooled}</td><td>when true, it is assumed that an connectionpooling mechanism is present. Before a message is sent, a new connection is obtained, that is closed after the message is sent. When transacted is true, connectionsArePooled is true, too</td><td>true</td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>&nbsp;</td><td>false</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQueryType(String) queryType}</td><td>either "select" for queries that return data, or anything else for queries that return no data.</td><td>"other"</td></tr>
 * <tr><td>{@link #setMaxRows(int) maxRows}</td><td>maximum number of rows returned</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setStartRow(int) startRow}</td><td>the number of the first row returned from the output</td><td>1</td></tr>
 * <tr><td>{@link #setScalar(boolean) scalar}</td><td>when true, the value of the first column of the first row (or the StartRow) is returned as the only result</td><td>false</td></tr>
 * <tr><td>{@link #setSynchronous(boolean) synchronous}</td><td>&nbsp;</td><td>true</td></tr>
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
	public static final String version="$RCSfile: JdbcQuerySenderBase.java,v $ $Revision: 1.14 $ $Date: 2005-09-08 16:00:52 $";

	private String queryType = "other";
	private int startRow=1;
	private int maxRows=-1; // return all rows
	private boolean scalar=false;
	private boolean synchronous=true;

	
	/**
	 * Obtain a prepared statement to be executed.
	 * Method-stub to be overridden in descender-classes.
	 */
	protected abstract PreparedStatement getStatement(Connection con, String correlationID, String message) throws JdbcException, SQLException;



	protected String sendMessage(Connection connection, String correlationID, String message, ParameterResolutionContext prc) throws SenderException{
		PreparedStatement statement=null;
		
		try {
			statement = getStatement(connection, correlationID, message);
			if (prc != null && paramList != null) {
				applyParameters(statement, prc.getValues(paramList));
			}
			if ("select".equalsIgnoreCase(getQueryType())) {
				return executeSelectQuery(statement);
			} else {
				int numRowsAffected = statement.executeUpdate();
				if (isScalar()) {
					return numRowsAffected+"";
				}
				return "<result><rowsupdated>" + numRowsAffected + "</rowsupdated></result>";
			}
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix() + "got exception evaluating parameters", e);
		} catch (SQLException e) {
			throw new SenderException(getLogPrefix() + "got exception sending message", e);
		} catch (JdbcException e) {
			throw new SenderException(e);
		} finally {
			try {
				if (statement!=null) {
					statement.close();
				}
			} catch (SQLException e) {
				log.warn(new SenderException(getLogPrefix() + "got exception closing SQL statement",e ));
			}
		}
	}

	private String executeSelectQuery(PreparedStatement statement) throws SenderException{
		ResultSet resultset=null;
		try {
			String result=null;
			
			if (getMaxRows()>0) {
				statement.setMaxRows(getMaxRows()+ ( getStartRow()>1 ? getStartRow()-1 : 0));
			}

			resultset = statement.executeQuery();

			if (getStartRow()>1) {
				resultset.absolute(getStartRow()-1);
				log.debug(getLogPrefix() + "Index set at position: " +  resultset.getRow() );
			}
				
			if (isScalar()) {
				if (resultset.next()) {
					result = resultset.getString(1);
					if (resultset.wasNull()) {
						result = null;
					}
				}
			} else {
				// Create XML and give the maxlength as a parameter
				DB2XMLWriter transformer = new DB2XMLWriter();
				result = transformer.getXML(resultset, getMaxRows());
			}
			return result;
		} catch (SQLException sqle) {
			throw new SenderException(getLogPrefix() + "got exception executing a SELECT SQL command",sqle );
		} finally {
			try {
				if (resultset!=null) {
					resultset.close();
				}
			} catch (SQLException e) {
				log.warn(new SenderException(getLogPrefix() + "got exception closing resultset",e));
			}
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


	public boolean isScalar() {
		return scalar;
	}

	public void setScalar(boolean b) {
		scalar = b;
	}


	public void setSynchronous(boolean synchronous) {
	   this.synchronous=synchronous;
	}
	public boolean isSynchronous() {
	   return synchronous;
	}

}
