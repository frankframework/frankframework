package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.util.DB2XMLWriter;
import nl.nn.adapterframework.util.XmlUtils;
import org.w3c.dom.Element;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Perform a sql query on the datasource on which the adapter is working.
 * The query as well as additional parameters are specified by an XML message.<br/>
 *
 * Sample xml:<br/><code><pre>
 *	&lt;query&gt;
 *	    &lt;sqlquery&gt;select * from mytable&lt;/sqlquery&gt;
 *	    &lt;maxlength&gt;30&lt;/maxlength&gt;
 *	    &lt;startindex&gt;31&lt;/startindex&gt;
 *	&lt;/query&gt;
 * </pre></code> <br/>
 * <p>
 * Notice: it must be valid XML. The maxlength and startindex elements are optional
 * and will only used when performing a select query.
 * The result will be an xml representation of the resultset when performing a select.
 * In case of an update/insert/delete statement, the number of rows that have been updated
 * will be returned.
 * </p><p>
 * Notice 2: currently only SELECT, UPDATE, INSERT and DELETE queries are supported
 * </p>
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(Object, nl.nn.adapterframework.core.PipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setDatasourceName(String) datasourceName}</td><td>the name of the datasource to perform the query on</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQuery(String) query}</td><td>the query to be executed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxLengthDefault(String) maxLengthDefault}</td><td>the default value for the maximum number of rows to be returned for a select statement</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * @version Id
 * @author Dennis van Loon IOS
 */

public class JDBCConnector extends FixedForwardPipe {
	public static final String version="$Id: JDBCConnector.java,v 1.3 2004-03-26 10:42:35 NNVZNL01#L180564 Exp $";

	private final static String QUERYTAG = "sqlquery";
	private final static String STARTINDEXTAG = "startindex";
	private final static String MAXLENGTHTAG = "maxlength";
	private PreparedStatement prep = null;
	private ResultSet resultset = null;
    private DataSource datasource = null;
	private String datasourceName = null;
	private String query = null;
	private String queryType = null;
	private int startIndex;
	private int maxLength;
	private String maxLengthDefault;

public void configure() throws ConfigurationException {
    super.configure();
    if (getMaxThreads()!=1) {
	    throw new ConfigurationException("sorry, cannot assume this Pipe-class to be thread safe, please set maxThreads to 1");
    }
}
/**
 * Perform the sql query specified in the xml message
 */
public PipeRunResult doPipe(Object input) throws PipeRunException {

    if ( !(input instanceof String)) {
    	throw new PipeRunException(this, "String expected, got a ["+input.getClass().getName()+"]");
    }
	parseInput(input);

	String resultXml="";

	Connection connection = null;
	
    try {
      connection = datasource.getConnection();
      prep = connection.prepareStatement(getQuery());

      if( getQueryType().equalsIgnoreCase("select") ) {
		resultXml = doSelect();
      } else if ( getQueryType().equalsIgnoreCase("update") ) {
		resultXml = doUpdate();
      }

    } catch ( SQLException e ) {
      throw new PipeRunException(this, "["+getName()+"] got SQLException executing SQL query ["+getQuery()+"]",  e );
    } catch ( Exception e) {
      throw new PipeRunException(this, "["+getName()+"] got error executing SQL query ["+getQuery()+"]", e );
    } finally {
		try {
	      if (resultset != null)
            resultset.close();
          if (prep != null)
            prep.close();
          if (connection != null)
        	connection.close();
		} catch( SQLException sqle ) {
			throw new PipeRunException(this, "["+getName()+"] got error executing finally in SQL command", sqle );
		}
    }

    return new PipeRunResult(getForward(), resultXml);
}
/**
 * Performs the sql query and generates the result in case of a select statement
 */
private String doSelect() throws PipeRunException{
	
	try {
		resultset = prep.executeQuery();

		// Determine the number of rows in the resultset
		resultset.last();
		int numberOfRows = resultset.getRow();
		log.debug("[" + getName() + "] Number of rows in resultset: " + numberOfRows );

		// Do something if there are any records in the resultset
		if( numberOfRows > 0 ) {
			if( numberOfRows < getStartIndex() ) {
				resultset.absolute(numberOfRows);
			} else {
				if (getStartIndex() == 1) {
					resultset.beforeFirst();
				} else {
					resultset.absolute(getStartIndex()-1);
				}
			}
	    }		     
		log.debug("[" + getName() + "] Index set at position: " +  resultset.getRow() );
				
		// Create XML and give the maxlength as a parameter
		DB2XMLWriter transformer = new DB2XMLWriter();
		return transformer.getXML(resultset, getMaxLength());
	} catch (SQLException sqle) {
		throw new PipeRunException(this, "["+getName()+"] got error executing a SELECT SQL command",sqle );
	}

}
/**
 * Performs the sql query and generates the result in case of an update statement
 */
private String doUpdate() throws PipeRunException{
	
	try {
		int i = prep.executeUpdate();
		return "<result><rowsupdated>" + i + "</rowsupdated></result>";
	} catch (SQLException sqle) {
		throw new PipeRunException(this, "["+getName()+"] got error executing a UPDATE SQL command", sqle );
	}

}
/**
 * Datasource objects which handles the connections to the database
 */
protected DataSource getDatasource() {
	return datasource;
}
/**
 * The name of the datasource where the SQL operates on
 */
public String getDatasourceName() {
	return datasourceName;
}
/**
 * The maximum number of rows to be returned for a select statement.
 */
protected int getMaxLength() {
	return maxLength;
}
/**
 * The default value for the maximum number of rows to be returned for a select statement
 */
public String getMaxLengthDefault() {
	return maxLengthDefault;
}
/**
 * The query to be executed.
 */
public String getQuery() {
	return query;
}
/**
 * The type of query that will be executed.
 * Possible values are select (for select) and update (for update/insert/delete).
 */
protected String getQueryType() {
	return queryType;
}
/**
 * The index of the first record in the resultset that will be returned.
 */
public int getStartIndex() {
	return startIndex;
}
/**
 * parses the XML input string to variables in this class.
 * @param input  XML String
 * @throws PipeRunException
 */
protected void parseInput(Object input) throws PipeRunException {
    Element queryElement;
    try {
        queryElement = XmlUtils.buildElement((String) input);

		// get the query from the message 
        setQuery("");
        if (null != XmlUtils.getChildTagAsString(queryElement, QUERYTAG))
        	setQuery(XmlUtils.getChildTagAsString(queryElement, QUERYTAG));
        log.info("[" + getName() + "] Query: " + getQuery() );

        // validate the query, there should be at least one space (see type determination)
        if( getQuery().indexOf(" ") == -1 )
        	throw new PipeRunException(this, "[" + getName() + "] Invalid sql query, no operation specified");

        // figure out the type of operation, select or update
        String operation = getQuery().substring(0, getQuery().indexOf(" "));
		if (operation.equalsIgnoreCase("select")) {
			setQueryType("select");			
		} else if (operation.equalsIgnoreCase("update") || operation.equalsIgnoreCase("insert") || operation.equalsIgnoreCase("delete")) {
			setQueryType("update");			
		} else {
        	throw new PipeRunException(this, "[" + getName() + "] Invalid sql query, no valid operation specified");
		}
        log.info("[" + getName() + "] Querytype: " + getQueryType() );
		
        // set the default value for startindex and check if there is a (valid) value in the message
        setStartIndex(1);
        if (null != XmlUtils.getChildTagAsString(queryElement, STARTINDEXTAG)) {
        	// if there is one, use it if it is a valid int ( must be > 0 )
        	try {
				setStartIndex( Integer.parseInt( XmlUtils.getChildTagAsString(queryElement, STARTINDEXTAG)  ));
  		      	if (getStartIndex() < 1) {
    	    		setStartIndex( 1 );
     		   		log.info("[" + getName() + "] Specfied startindex must be > 0, value set to: " + getStartIndex() );
       		 	}
        	} catch (NumberFormatException nfe) {
	        	log.info("[" + getName() + "] Specified startindex is not a number, value set to: " + getStartIndex() );
        	}
        }
        log.info("[" + getName() + "] Startindex: " + getStartIndex() );

        // set the default value for maxlength and check if there is a (valid) value in the message
        setMaxLength(  Integer.parseInt(getMaxLengthDefault()) );
        if (null != XmlUtils.getChildTagAsString(queryElement, MAXLENGTHTAG)) {
        	// if there is one, use it if it is a valid int ( must be > 0 ) 
        	try {
				setMaxLength( Integer.parseInt( XmlUtils.getChildTagAsString(queryElement, MAXLENGTHTAG) ));
	        	if (getMaxLength() < 1) {
 		       		setMaxLength(  Integer.parseInt(getMaxLengthDefault()) );
   		     		log.info("[" + getName() + "] Specfied maxlength must be > 0, value set to " + getMaxLength());
     		   	}
        	} catch (NumberFormatException nfe) {
	        	log.info("[" + getName() + "] Specified maxlength is not a number, value set to: " + getMaxLength() );
        	}
        }
        log.info("[" + getName() + "] Maxlength " + getMaxLength() );
        	
    } catch (nl.nn.adapterframework.util.DomBuilderException e) {
        throw new PipeRunException(this, "[" + getName() + "] Error during parsing input message", e);
    }

}
/**
 * Set the name of the datasource where the SQL operates on.
 */
public void setDatasourceName(String datasourceName) {
	this.datasourceName = datasourceName;
}
/**
 * Set the maximum number of rows to be returned for a select statement.
 */
protected void setMaxLength(int newMaxLength) {
	maxLength = newMaxLength;
}
/**
 * Set the default value for the maximum number of rows to be returned for a select statement
 */
public void setMaxLengthDefault(String newMaxlengthDefault) {
	maxLengthDefault = newMaxlengthDefault;
}
/**
 * Set the query to be executed.
 */
public void setQuery(String newQuery) {
	query = newQuery;
}
/**
 * Set the type of query that will be executed.
 */
protected void setQueryType(String newQueryType) {
	queryType = newQueryType;
}
/**
 * Set the index of the first record in the resultset that will be returned.
 */
public void setStartIndex(int newStartIndex) {
	startIndex = newStartIndex;
}
    public void start() throws PipeStartException {
    	try {
         	Context context = new InitialContext();
         	if (context==null) {
				log.info( "["+getName()+"]: context is null" );
				throw new PipeStartException( "["+getName()+"]: context is null" );
         	}
       		this.datasource =(DataSource) context.lookup( getDatasourceName() );
    	} catch (NamingException ne) {
			throw new PipeStartException("["+getName()+ "] Lookup failed for " +  getDatasourceName(), ne);   
		}
    }
}
