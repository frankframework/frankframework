/*
 * $Log: JdbcQuerySenderBase.java,v $
 * Revision 1.1  2004-03-24 13:28:20  L190409
 * initial version
 *
 */
package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.DB2XMLWriter;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * This executes the query that is obtained from the (here still abstract) method getStatement.
 * Descendent classes can override getStatement to provide meaningful statements.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.JdbcQuerySenderBase</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDatasourceName(String) datasourceName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDatasourceNameXA(String) datasourceNameXA}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUsername(String) username}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>&nbsp;</td><td>false</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQueryType(String) queryType}</td><td>either "select" for queries that return data, or anything else for queries that return no data</td><td>"other"</td></tr>
 * <tr><td>{@link #setMaxRows(int) maxRows}</td><td>maximum number of rows returned</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setStartRows(int) startRow}</td><td>the number of the first row returned from the output</td><td>1</td></tr>
 * </table>
 * </p>
 * 
 * <p>$Id: JdbcQuerySenderBase.java,v 1.1 2004-03-24 13:28:20 L190409 Exp $</p>
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public abstract class JdbcQuerySenderBase extends JdbcFacade implements ISender {
	public static final String version="$Id: JdbcQuerySenderBase.java,v 1.1 2004-03-24 13:28:20 L190409 Exp $";

	private String queryType = "other";
	private int startRow=1;
	private int maxRows=0;
	private Connection connection=null;

	public JdbcQuerySenderBase() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * @see nl.nn.adapterframework.core.ISender#configure()
	 */
	public void configure() throws ConfigurationException {
		try {
			if (getDatasource()==null) {
				throw new ConfigurationException(getLogPrefix()+"has no datasource");
			}
		} catch (JdbcException e) {
			throw new ConfigurationException(e);
		}
	}

	public void open() throws SenderException {
		if (!isTransacted()) {
			try {
				connection = getConnection();
			} catch (JdbcException e) {
				throw new SenderException(e);
			}
		}
	}	
	
	public void close() throws SenderException {
	    try {
	        if (connection != null) {
				connection.close();
	        }
	    } catch (SQLException e) {
	        throw new SenderException(getLogPrefix() + "caught exception stopping sender", e);
	    } finally {
			connection = null;
	    }
	}

	
	/**
	 * returns <code>true</code> if the {@link setQueryType(String) queryType} is set to "select".
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
	
	public String sendMessage(String correlationID, String message) throws SenderException {
		if (isTransacted()) {
			try {
				Connection c = getConnection();
				String result = sendMessage(correlationID, message, c);
				c.close();
				return result;
			} catch (SQLException e) {
				throw new SenderException(getLogPrefix() + "caught exception sender message, ID=["+correlationID+"]", e);
			} catch (JdbcException e) {
				throw new SenderException(e);
			}
		} else {
			synchronized (connection) {
				return sendMessage(correlationID, message, connection);
			}
		}
	}

	private String sendMessage(String correlationID, String message, Connection connection) throws SenderException{
		PreparedStatement statement;
		
		try {
			statement = getStatement(connection, correlationID, message);
			if (isSynchronous()) {
				return executeSelectQuery(statement);
			} else {
				int numRowsAffected = statement.executeUpdate();
				return "<result><rowsupdated>" + numRowsAffected + "</rowsupdated></result>";
			}
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
	
	public String toString() {
		String result  = super.toString();
        ToStringBuilder ts=new ToStringBuilder(this);
        ts.append("name", getName() );
        ts.append("version", version);
        result += ts.toString();
        return result;

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
