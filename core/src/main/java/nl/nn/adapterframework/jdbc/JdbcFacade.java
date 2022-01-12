/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ApplicationWarnings;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.SuppressKeys;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IXAEnabled;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.jdbc.dbms.Dbms;
import nl.nn.adapterframework.jdbc.dbms.DbmsSupportFactory;
import nl.nn.adapterframework.jdbc.dbms.GenericDbmsSupport;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupportFactory;
import nl.nn.adapterframework.jndi.JndiBase;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;
import nl.nn.adapterframework.statistics.HasStatistics;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.statistics.StatisticsKeeperIterationHandler;
import nl.nn.adapterframework.task.TimeoutGuard;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.CredentialFactory;

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
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class JdbcFacade extends JndiBase implements HasPhysicalDestination, IXAEnabled, HasStatistics {

	private String datasourceName = null;
	private String authAlias = null;
	private String username = null;
	private String password = null;

	private boolean transacted = false;
	private boolean connectionsArePooled=true; // TODO: make this a property of the DataSourceFactory
	
	private IDbmsSupportFactory dbmsSupportFactoryDefault=null;
	private IDbmsSupportFactory dbmsSupportFactory=null;
	private IDbmsSupport dbmsSupport=null;
	private CredentialFactory cf=null;
	private StatisticsKeeper connectionStatistics;
	private String applicationServerType = AppConstants.getInstance().getResolvedProperty(AppConstants.APPLICATION_SERVER_TYPE_PROPERTY);
	private boolean suppressResultSetHoldabilityWarning = AppConstants.getInstance().getBoolean(SuppressKeys.RESULT_SET_HOLDABILITY.getKey(), false);

	private @Setter @Getter IDataSourceFactory dataSourceFactory = null; // Spring should wire this!

	private DataSource datasource = null;

	protected String getLogPrefix() {
		return "["+this.getClass().getName()+"] ["+getName()+"] ";
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getDatasourceName())) {
			setDatasourceName(AppConstants.getInstance(getConfigurationClassLoader()).getResolvedProperty(JndiDataSourceFactory.DEFAULT_DATASOURCE_NAME_PROPERTY));
		}
		try {
			if (getDatasource() == null) {
				throw new ConfigurationException(getLogPrefix() + "has no datasource");
			}
		} catch (JdbcException e) {
			throw new ConfigurationException(e);
		}
		if (StringUtils.isNotEmpty(getUsername()) || StringUtils.isNotEmpty(getAuthAlias())) {
			cf = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
		}
		connectionStatistics = new StatisticsKeeper("getConnection for "+getName());
	}

	protected DataSource getDatasource() throws JdbcException {
		if (datasource==null) {
			String dsName = getDatasourceName();
			try {
				datasource = getDataSourceFactory().getDataSource(dsName, getJndiEnv());
			} catch (NamingException e) {
				throw new JdbcException("Could not find Datasource ["+dsName+"]", e);
			}
			if (datasource==null) {
				throw new JdbcException("Could not find Datasource ["+dsName+"]");
			}
			String dsinfo=getDatasourceInfo();
			if (dsinfo==null) {
				dsinfo=datasource.toString();
			}
			log.info(getLogPrefix()+"looked up Datasource ["+dsName+"]: ["+dsinfo+"]");
			datasource = new TransactionAwareDataSourceProxy(datasource);
		}
		return datasource;
	}

	public String getDatasourceInfo() throws JdbcException {
		String dsinfo=null;
		try (Connection conn=getConnection()) {
			DatabaseMetaData md=conn.getMetaData();
			String product=md.getDatabaseProductName();
			String productVersion=md.getDatabaseProductVersion();
			String driver=md.getDriverName();
			String driverVersion=md.getDriverVersion();
			String url=md.getURL();
			String user=md.getUserName();
			boolean showWarning = !suppressResultSetHoldabilityWarning || (getDatabaseType() == Dbms.DB2 && "WAS".equals(applicationServerType));
			if (showWarning && md.getResultSetHoldability() != ResultSet.HOLD_CURSORS_OVER_COMMIT) {
				// For (some?) combinations of WebShere and (XA) Databases this seems to be the default and result in the following exception:
				// com.ibm.websphere.ce.cm.ObjectClosedException: DSRA9110E: ResultSet is closed.
				// When a ResultSetIteratingPipe is calling next() on the ResultSet after processing the first message it will throw the exception when:
				// - the ResultSetIteratingPipe is non-transacted and the sender calls a sub-adapter that is transacted (transactionAttribute="Required")
				// - the ResultSetIteratingPipe is transacted and sender contains a non transacted sender (transactionAttribute="NotSupported")
				// Either none, or both need to be transacted.
				// See issue #2015 ((ObjectClosedException) DSRA9110E: ResultSet is closed) on www.github.com
				ApplicationWarnings.add(log, "The database's default holdability for ResultSet objects is " + md.getResultSetHoldability() + " instead of " + ResultSet.HOLD_CURSORS_OVER_COMMIT + " (ResultSet.HOLD_CURSORS_OVER_COMMIT)");
			}
			dsinfo ="user ["+user+"] url ["+url+"] product ["+product+"] product version ["+productVersion+"] driver ["+driver+"] driver version ["+driverVersion+"]";
		} catch (SQLException e) {
			log.warn("Exception determining databaseinfo",e);
		}
		return dsinfo;
	}

	public Dbms getDatabaseType() {
		IDbmsSupport dbms=getDbmsSupport();
		return dbms != null ? dbms.getDbms() : Dbms.NONE; 
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
		long t0 = System.currentTimeMillis();
		try {
			DataSource ds = getDatasource();
			try {
				if (cf!=null) {
					return ds.getConnection(cf.getUsername(), cf.getPassword());
				}
				return ds.getConnection();
			} catch (SQLException e) {
				throw new JdbcException(getLogPrefix()+"cannot open connection on datasource ["+getDatasourceName()+"]", e);
			}
		} finally {
			if (connectionStatistics!=null) {
				long t1= System.currentTimeMillis();
				connectionStatistics.addValue(t1-t0);
			}
		}
	}

	public Connection getConnectionWithTimeout(int timeout) throws JdbcException, TimeOutException {
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

	@Override
	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, int action) throws SenderException {
		hski.handleStatisticsKeeper(data, connectionStatistics);
	}

	@Override
	@Deprecated
	@ConfigurationWarning("Please use attribute dataSourceName instead")
	public void setJmsRealm(String jmsRealmName) {
		super.setJmsRealm(jmsRealmName); //super.setJmsRealm(...) sets the jmsRealmName only when a realm is found
		if(StringUtils.isEmpty(getJmsRealmName())) { //confirm that the configured jmsRealm exists
			throw new IllegalStateException("JmsRealm ["+jmsRealmName+"] not found");
		}
	}

	/**
	 * Returns the name and location of the database that this objects operates on.
	 * 
	 * @see nl.nn.adapterframework.core.HasPhysicalDestination#getPhysicalDestinationName()
	 */
	@Override
	public String getPhysicalDestinationName() {
		String result="unknown";
		try (Connection connection = getConnection()) {
			DatabaseMetaData metadata = connection.getMetaData();
			result = metadata.getURL();

			String catalog=null;
			catalog=connection.getCatalog();
			result += catalog!=null ? ("/"+catalog):"";
		} catch (Exception e) {
			log.warn(getLogPrefix()+"exception retrieving PhysicalDestinationName", e);
		}
		return result;
	}

	@IbisDoc({"2", "JNDI name of datasource to be used, can be configured via jmsRealm, too", "${"+JndiDataSourceFactory.DEFAULT_DATASOURCE_NAME_PROPERTY+"}"})
	public void setDatasourceName(String datasourceName) {
		this.datasourceName = datasourceName;
	}
	public String getDatasourceName() {
		return datasourceName;
	}

	@IbisDoc({ "3", "Authentication alias used to authenticate when connecting to database", "" })
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}
	public String getAuthAlias() {
		return authAlias;
	}
	
	@IbisDoc({"4", "User name for authentication when connecting to database, when none found from <code>authAlias</code>", ""})
	public void setUsername(String username) {
		this.username = username;
	}
	public String getUsername() {
		return username;
	}

	@IbisDoc({"5", "Password for authentication when connecting to database, when none found from <code>authAlias</code>", ""})
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
	@Override
	public boolean isTransacted() {
		return transacted;
	}

	@IbisDoc({"6", "informs the sender that the obtained connection is from a pool", "true"})
	public boolean isConnectionsArePooled() {
		return connectionsArePooled || isTransacted();
	}
	public void setConnectionsArePooled(boolean b) {
		connectionsArePooled = b;
	}

}
