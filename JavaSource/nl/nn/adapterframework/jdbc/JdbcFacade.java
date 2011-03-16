/*
 * $Log: JdbcFacade.java,v $
 * Revision 1.34  2011-03-16 16:42:40  L190409
 * introduction of DbmsSupport, including support for MS SQL Server
 *
 * Revision 1.33  2011/01/06 09:48:33  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * *** empty log message ***
 *
 * Revision 1.32  2010/12/31 09:33:01  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * try to prefix with java:/ to find datasource, for JBoss compatibiltity
 *
 * Revision 1.31  2010/12/31 09:32:15  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * *** empty log message ***
 *
 * Revision 1.30  2010/07/12 12:38:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid NPE when connection is null
 *
 * Revision 1.29  2010/02/11 14:25:22  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * moved determination of databaseType to JdbcUtil
 *
 * Revision 1.28  2010/02/02 14:31:47  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * separate method for getting datasource info
 *
 * Revision 1.27  2009/12/08 14:49:26  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * fixed NullPointerException on datatime parameters
 *
 * Revision 1.26  2009/09/07 13:14:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use log from ancestor
 *
 * Revision 1.25  2009/03/26 14:47:36  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added LOCKROWS_SUFFIX
 *
 * Revision 1.24  2007/11/23 14:47:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fix check on XA datasource
 *
 * Revision 1.23  2007/11/23 14:16:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * remove datasourceNameXA
 *
 * Revision 1.22  2007/08/10 11:05:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added note about table SYS.DBA_PENDING_TRANSACTIONS that should be readable
 *
 * Revision 1.21  2007/07/19 15:07:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * check for null datasource
 *
 * Revision 1.20  2007/07/17 15:10:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * show database info in log
 *
 * Revision 1.19  2007/06/14 08:47:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for parameter type=number
 *
 * Revision 1.18  2007/05/24 09:50:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed applying of datetime parameters
 *
 * Revision 1.17  2007/05/23 09:08:53  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added productType detector
 *
 * Revision 1.16  2007/05/16 11:40:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * apply datetime parameters using corresponding methods
 *
 * Revision 1.15  2007/02/12 13:56:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.14  2006/12/12 09:57:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * restore jdbc package
 *
 * Revision 1.12  2005/09/22 15:58:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added warning in comment for getParameterMetadata
 *
 * Revision 1.11  2005/08/24 15:47:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * try to prefix with java:comp/env/ to find datasource (Tomcat compatibility)
 *
 * Revision 1.10  2005/08/17 16:10:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * test for empty strings using StringUtils.isEmpty()
 *
 * Revision 1.9  2005/08/09 15:53:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved exception construction
 *
 * Revision 1.8  2005/07/19 12:36:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved applyParameters to JdbcFacade
 *
 * Revision 1.7  2005/06/13 09:57:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.6  2005/05/31 09:53:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected version
 *
 * Revision 1.5  2005/05/31 09:53:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of attribute 'connectionsArePooled'
 *
 * Revision 1.4  2005/03/31 08:11:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected typo in logging
 *
 * Revision 1.3  2004/03/26 10:43:09  Johan Verrips <johan.verrips@ibissource.org>
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
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

import javax.naming.NamingException;
import javax.sql.DataSource;

import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IXAEnabled;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.dbms.DbmsSupportFactory;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.jms.JNDIBase;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;

import org.apache.commons.lang.StringUtils;

/**
 * Provides functions for JDBC connections.
 * 
 * N.B. Note on using XA transactions:
 * If transactions are used, make sure that the database user can access the table SYS.DBA_PENDING_TRANSACTIONS.
 * If not, transactions present when the server goes down cannot be properly recovered, resulting in exceptions like:
 * <pre>
   The error code was XAER_RMERR. The exception stack trace follows: javax.transaction.xa.XAException
	at oracle.jdbc.xa.OracleXAResource.recover(OracleXAResource.java:508)
   </pre>
 * 
 * 
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class JdbcFacade extends JNDIBase implements INamedObject, HasPhysicalDestination, IXAEnabled {
	public static final String version="$RCSfile: JdbcFacade.java,v $ $Revision: 1.34 $ $Date: 2011-03-16 16:42:40 $";
	
	private String name;
    private String username=null;
    private String password=null;
    
	private DataSource datasource = null;
	private String datasourceName = null;
//	private String datasourceNameXA = null;

	private boolean transacted = false;
	private boolean connectionsArePooled=true;
	
	private IDbmsSupport dbms=null;

	protected String getLogPrefix() {
		return "["+this.getClass().getName()+"] ["+getName()+"] ";
	}

//	/**
//	 * Returns either {@link #getDatasourceName() datasourceName} or {@link #getDatasourceNameXA() datasourceNameXA},
//	 * depending on the value of {@link #isTransacted()}.
//	 * If the right one is not specified, the other is used. 
//	 */
//	public String getDataSourceNameToUse() throws JdbcException {
//		String result = isTransacted() ? getDatasourceNameXA() : getDatasourceName();
//		if (StringUtils.isEmpty(result)) {
//			// try the alternative...
//			result = isTransacted() ? getDatasourceName() : getDatasourceNameXA();
//			if (StringUtils.isEmpty(result)) {
//				throw new JdbcException(getLogPrefix()+"neither datasourceName nor datasourceNameXA are specified");
//			}
//			log.warn(getLogPrefix()+"correct datasourceName attribute not specified, will use ["+result+"]");
//		}
//		return result;
//	}
	public String getDataSourceNameToUse() throws JdbcException {
		String result = getDatasourceName();
		if (StringUtils.isEmpty(result)) {
			throw new JdbcException(getLogPrefix()+"no datasourceName specified");
		}
		return result;
	}

	protected DataSource getDatasource() throws JdbcException {
		// TODO: create bean jndiContextPrefix instead of doing multiple attempts
			if (datasource==null) {
				String dsName = getDataSourceNameToUse();
				try {
					log.debug(getLogPrefix()+"looking up Datasource ["+dsName+"]");
					datasource =(DataSource) getContext().lookup( dsName );
					if (datasource==null) {
						throw new JdbcException("Could not find Datasource ["+dsName+"]");
					}
					String dsinfo=getDatasourceInfo();
					if (dsinfo==null) {
						dsinfo=datasource.toString();
					}
					log.info(getLogPrefix()+"looked up Datasource ["+dsName+"]: ["+dsinfo+"]");
				} catch (NamingException e) {
					try {
						String tomcatDsName="java:comp/env/"+dsName;
						log.debug(getLogPrefix()+"could not find ["+dsName+"], now trying ["+tomcatDsName+"]");
						datasource =(DataSource) getContext().lookup( tomcatDsName );
						log.debug(getLogPrefix()+"looked up Datasource ["+tomcatDsName+"]: ["+datasource+"]");
					} catch (NamingException e2) {
						try {
							String jbossDsName="java:/"+dsName;
							log.debug(getLogPrefix()+"could not find ["+dsName+"], now trying ["+jbossDsName+"]");
							datasource =(DataSource) getContext().lookup( jbossDsName );
							log.debug(getLogPrefix()+"looked up Datasource ["+jbossDsName+"]: ["+datasource+"]");
						} catch (NamingException e3) {
							throw new JdbcException(getLogPrefix()+"cannot find Datasource ["+dsName+"]", e);
						}
					}
				}
			}
			return datasource;
		}

	public String getDatasourceInfo() throws JdbcException {
		String dsinfo=null;
		Connection conn=null;
		try {
			conn=getConnection();
			DatabaseMetaData md=conn.getMetaData();
			String product=md.getDatabaseProductName();
			String productVersion=md.getDatabaseProductVersion();
			String driver=md.getDriverName();
			String driverVersion=md.getDriverVersion();
			String url=md.getURL();
			String user=md.getUserName();
			dsinfo ="user ["+user+"] url ["+url+"] product ["+product+"] version ["+productVersion+"] driver ["+driver+"] version ["+driverVersion+"]";
		} catch (SQLException e) {
			log.warn("Exception determining databaseinfo",e);
		} finally {
			if (conn!=null) {
				try {
					conn.close();
				} catch (SQLException e1) {
					log.warn("exception closing connection for metadata",e1);
				}
			}
		}
		return dsinfo;
	}

	
	public void setDbmsSupport(IDbmsSupport dbmsSupport) {
		this.dbms=dbmsSupport;
	}

	public int getDatabaseType() {
		dbms=getDbmsSupport();
		if (dbms==null) {
			return -1;
		}
		return dbms.getDatabaseType();
	}
	
	public IDbmsSupport getDbmsSupport() {
		if (dbms==null) {
			Connection conn=null;
			try {
				conn=getConnection();
				setDbmsSupport(DbmsSupportFactory.getDbmsSupport(conn));
				//databaseType=DbmsUtil.getDatabaseType(conn);
			} catch (Exception e) {
				log.warn("Exception determining dbmssupport", e);
				return null;
			} finally {
				try {
					if (conn!=null) { 
						conn.close();
					}
				} catch (SQLException e1) {
					log.warn("exception closing connection for dbmssupport",e1);
				}
			}
		}
		return dbms;
	}
	/**
	 * Obtains a connection to the datasource. 
	 */
	// TODO: consider making this one protected.
	public Connection getConnection() throws JdbcException {
		try {
			if (StringUtils.isNotEmpty(getUsername())) {
				return getDatasource().getConnection(getUsername(),getPassword());
			} else {
				return getDatasource().getConnection();
			}
		} catch (SQLException e) {
			throw new JdbcException(getLogPrefix()+"cannot open connection on datasource ["+getDataSourceNameToUse()+"]", e);
		}
	}

	/**
	 * Returns the name and location of the database that this objects operates on.
	 *  
	 * @see nl.nn.adapterframework.core.HasPhysicalDestination#getPhysicalDestinationName()
	 */
	public String getPhysicalDestinationName() {
		String result="unknown";
		try {
			Connection connection = getConnection();
			DatabaseMetaData metadata = connection.getMetaData();
			result = metadata.getURL();
	
			String catalog=null;
			catalog=connection.getCatalog();
			result += catalog!=null ? ("/"+catalog):"";
			
			connection.close();
		} catch (Exception e) {
			log.warn(getLogPrefix()+"exception retrieving PhysicalDestinationName", e);		
		}
		return result;
	}

	protected void applyParameters(PreparedStatement statement, ParameterValueList parameters) throws SQLException, SenderException {
		// statement.clearParameters();
	
/*
		// getParameterMetaData() is not supported on the WebSphere java.sql.PreparedStatement implementation.
		int senderParameterCount = parameters.size();
		int statementParameterCount = statement.getParameterMetaData().getParameterCount();
		if (statementParameterCount<senderParameterCount) {
			throw new SenderException(getLogPrefix()+"statement has more ["+statementParameterCount+"] parameters defined than sender ["+senderParameterCount+"]");
		}
*/		
		
		for (int i=0; i< parameters.size(); i++) {
			ParameterValue pv = parameters.getParameterValue(i);
			String paramType = pv.getDefinition().getType();
			Object value = pv.getValue();
	//		log.debug("applying parameter ["+(i+1)+","+parameters.getParameterValue(i).getDefinition().getName()+"], value["+parameterValue+"]");

			if (Parameter.TYPE_DATE.equals(paramType)) {
				if (value==null) {
					statement.setNull(i+1, Types.DATE);
				} else {
					statement.setDate(i+1, new java.sql.Date(((Date)value).getTime()));
				}
			} else if (Parameter.TYPE_DATETIME.equals(paramType)) {
				if (value==null) {
					statement.setNull(i+1, Types.TIMESTAMP);
				} else {
					statement.setTimestamp(i+1, new Timestamp(((Date)value).getTime()));
				}
			} else if (Parameter.TYPE_TIMESTAMP.equals(paramType)) {
				if (value==null) {
					statement.setNull(i+1, Types.TIMESTAMP);
				} else {
					statement.setTimestamp(i+1, new Timestamp(((Date)value).getTime()));
				}
			} else if (Parameter.TYPE_TIME.equals(paramType)) {
				if (value==null) {
					statement.setNull(i+1, Types.TIME);
				} else {
					statement.setTime(i+1, new java.sql.Time(((Date)value).getTime()));
				}
			} else if (Parameter.TYPE_NUMBER.equals(paramType)) {
				statement.setDouble(i+1, ((Number)value).doubleValue());
			} else { 
				statement.setString(i+1, (String)value);
			}
		}
	}
	


	/**
	 * Sets the name of the object.
	 */
	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}

	/**
	 * Sets the JNDI name of datasource that is used when {@link #isTransacted()} returns <code>false</code> 
	 */
	public void setDatasourceName(String datasourceName) {
		this.datasourceName = datasourceName;
	}
	public String getDatasourceName() {
		return datasourceName;
	}
	/**
	 * Sets the JNDI name of datasource that is used when {@link #isTransacted()} returns <code>true</code> 
	 */
	public void setDatasourceNameXA(String datasourceNameXA) {
		if (StringUtils.isNotEmpty(datasourceNameXA)) {
			throw new IllegalArgumentException(getLogPrefix()+"use of attribute 'datasourceNameXA' is no longer supported. The datasource can now only be specified using attribute 'datasourceName'");
		}
//		this.datasourceNameXA = datasourceNameXA;
	}
//	public String getDatasourceNameXA() {
//		return datasourceNameXA;
//	}

	/**
	 * Sets the user name that is used to open the database connection.
	 * If a value is set, it will be used together with the (@link #setPassword(String) specified password} 
	 * to open the connection to the database.
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	public String getUsername() {
		return username;
	}

	/**
	 * Sets the password that is used to open the database connection.
	 * @see #setPassword(String)
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	protected String getPassword() {
		return password;
	}

	/**
	 * controls the use of transactions
	 */
	public void setTransacted(boolean transacted) {
		this.transacted = transacted;
	}
	public boolean isTransacted() {
		return transacted;
	}

	public boolean isConnectionsArePooled() {
		return connectionsArePooled || isTransacted();
	}
	public void setConnectionsArePooled(boolean b) {
		connectionsArePooled = b;
	}
	
}
