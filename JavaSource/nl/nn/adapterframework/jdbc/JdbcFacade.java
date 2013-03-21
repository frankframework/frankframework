/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.Map;

import javax.naming.NamingException;
import javax.sql.DataSource;

import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IXAEnabled;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.jdbc.dbms.DbmsSupportFactory;
import nl.nn.adapterframework.jdbc.dbms.GenericDbmsSupport;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupportFactory;
import nl.nn.adapterframework.jms.JNDIBase;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.task.TimeoutGuard;

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
 * @version $Id$
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class JdbcFacade extends JNDIBase implements INamedObject, HasPhysicalDestination, IXAEnabled {
	
	private String name;
    private String username=null;
    private String password=null;
    
	private Map proxiedDataSources = null;
	private DataSource datasource = null;
	private String datasourceName = null;

	private boolean transacted = false;
	private boolean connectionsArePooled=true;
	
	private IDbmsSupportFactory dbmsSupportFactoryDefault=null;
	private IDbmsSupportFactory dbmsSupportFactory=null;
	private IDbmsSupport dbmsSupport=null;

	protected String getLogPrefix() {
		return "["+this.getClass().getName()+"] ["+getName()+"] ";
	}

	public void setProxiedDataSources(Map proxiedDataSources) {
		this.proxiedDataSources = proxiedDataSources;
	}

	public String getDataSourceNameToUse() throws JdbcException {
		String result = getDatasourceName();
		if (StringUtils.isEmpty(result)) {
			throw new JdbcException(getLogPrefix()+"no datasourceName specified");
		}
		return result;
	}

	protected DataSource getDatasource() throws JdbcException {
		if (datasource==null) {
			String dsName = getDataSourceNameToUse();
			if (proxiedDataSources != null && proxiedDataSources.containsKey(dsName)) {
				log.debug(getLogPrefix()+"looking up proxied Datasource ["+dsName+"]");
				datasource = (DataSource)proxiedDataSources.get(dsName);
			} else {
				String prefixedDsName=getJndiContextPrefix()+dsName;
				log.debug(getLogPrefix()+"looking up Datasource ["+prefixedDsName+"]");
				if (StringUtils.isNotEmpty(getJndiContextPrefix())) {
					log.debug(getLogPrefix()+"using JNDI context prefix ["+getJndiContextPrefix()+"]");
				}
				try {
					datasource =(DataSource) getContext().lookup( prefixedDsName );
				} catch (NamingException e) {
					throw new JdbcException("Could not find Datasource ["+prefixedDsName+"]", e);
				}
			}
			if (datasource==null) {
				throw new JdbcException("Could not find Datasource ["+dsName+"]");
			}
			String dsinfo=getDatasourceInfo();
			if (dsinfo==null) {
				dsinfo=datasource.toString();
			}
			log.info(getLogPrefix()+"looked up Datasource ["+dsName+"]: ["+dsinfo+"]");
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

	
	public int getDatabaseType() {
		IDbmsSupport dbms=getDbmsSupport();
		if (dbms==null) {
			return -1;
		}
		return dbms.getDatabaseType();
	}

	public IDbmsSupportFactory getDbmsSupportFactoryDefault() {
		if (dbmsSupportFactoryDefault==null) {
			dbmsSupportFactoryDefault=new DbmsSupportFactory();
		}
		return dbmsSupportFactoryDefault;
	}
	public void setDbmsSupportFactoryDefault(IDbmsSupportFactory dbmsSupportFactory) {
		this.dbmsSupportFactoryDefault=dbmsSupportFactory;
	}
	
	public IDbmsSupportFactory getDbmsSupportFactory() {
		if (dbmsSupportFactory==null) {
			dbmsSupportFactory=getDbmsSupportFactoryDefault();
		}
		return dbmsSupportFactory;
	}
	public void setDbmsSupportFactory(IDbmsSupportFactory dbmsSupportFactory) {
		this.dbmsSupportFactory=dbmsSupportFactory;
	}

	public void setDbmsSupport(IDbmsSupport dbmsSupport) {
		this.dbmsSupport=dbmsSupport;
	}
		
	public IDbmsSupport getDbmsSupport() {
		if (dbmsSupport==null) {
			Connection conn=null;
			try {
				conn=getConnection();
			} catch (Exception e) {
				throw new RuntimeException("Cannot obtain connection to determine dbmssupport", e);
			}
			try {
				dbmsSupport=getDbmsSupportFactory().getDbmsSupport(conn);
				if (dbmsSupport==null) {
					log.warn(getLogPrefix()+"Could not determine database type from connection");
				} else {
					log.debug(getLogPrefix()+"determined database connection of type ["+dbmsSupport.getDbmsName()+"]");
				}
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
		if (dbmsSupport==null) {
			dbmsSupport=new GenericDbmsSupport();
		}
		return dbmsSupport;
	}
	/**
	 * Obtains a connection to the datasource. 
	 */
	// TODO: consider making this one protected.
	public Connection getConnection() throws JdbcException {
		DataSource ds=getDatasource();
		try {
			if (StringUtils.isNotEmpty(getUsername())) {
				return ds.getConnection(getUsername(),getPassword());
			}
			return ds.getConnection();
		} catch (SQLException e) {
			throw new JdbcException(getLogPrefix()+"cannot open connection on datasource ["+getDataSourceNameToUse()+"]", e);
		}
	}

	public Connection getConnection(int timeout) throws JdbcException, TimeOutException {
		if (timeout<=0) {
			return getConnection();
		}
		TimeoutGuard tg = new TimeoutGuard("Connection ");
		try {
			tg.activateGuard(timeout);
			return getConnection();
		} finally {
			if (tg.cancel()) {
				throw new TimeOutException(getLogPrefix()+"thread has been interrupted");
			} 
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
			} else if (Parameter.TYPE_XMLDATETIME.equals(paramType)) {
				if (value==null) {
					statement.setNull(i+1, Types.TIMESTAMP);
				} else {
					statement.setTimestamp(i+1, new Timestamp(((Date)value).getTime()));
				}
			} else if (Parameter.TYPE_NUMBER.equals(paramType)) {
				if (value==null) {
					statement.setNull(i+1, Types.NUMERIC);
				} else {
					statement.setDouble(i+1, ((Number)value).doubleValue());
				}
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
