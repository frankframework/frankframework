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
import java.sql.SQLException;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IXAEnabled;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupportFactory;
import nl.nn.adapterframework.jndi.TransactionalDbmsSupportAwareDataSourceProxy;
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
	private final @Getter(onMethod = @__(@Override)) String domain = "JDBC";
	private String datasourceName = null;
	private String authAlias = null;
	private String username = null;
	private String password = null;

	private boolean transacted = false;
	private boolean connectionsArePooled=true; // TODO: make this a property of the DataSourceFactory

	private IDbmsSupportFactory dbmsSupportFactory=null;
	private IDbmsSupport dbmsSupport=null;
	private CredentialFactory cf=null;
	private StatisticsKeeper connectionStatistics;

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

			String dsinfo = datasource.toString();
			log.info(getLogPrefix()+"looked up Datasource ["+dsName+"]: ["+dsinfo+"]");
		}
		return datasource;
	}

	public String getDatasourceInfo() throws JdbcException {
		String dsinfo=null;
		if(getDatasource() instanceof TransactionalDbmsSupportAwareDataSourceProxy) {
			return ((TransactionalDbmsSupportAwareDataSourceProxy) getDatasource()).getInfo();
		}
		try (Connection conn=getConnection()) {
			DatabaseMetaData md=conn.getMetaData();
			String product=md.getDatabaseProductName();
			String productVersion=md.getDatabaseProductVersion();
			String driver=md.getDriverName();
			String driverVersion=md.getDriverVersion();
			String url=md.getURL();
			String user=md.getUserName();
			dsinfo ="user ["+user+"] url ["+url+"] product ["+product+"] product version ["+productVersion+"] driver ["+driver+"] driver version ["+driverVersion+"]";
		} catch (SQLException e) {
			log.warn("Exception determining databaseinfo", e);
		}
		return dsinfo;
	}

	public void setDbmsSupportFactory(IDbmsSupportFactory dbmsSupportFactory) {
		this.dbmsSupportFactory=dbmsSupportFactory;
	}

	public IDbmsSupport getDbmsSupport() {
		if (dbmsSupport==null) {
			try {
				dbmsSupport = dbmsSupportFactory.getDbmsSupport(getDatasource());
			} catch (JdbcException e) {
				throw new IllegalStateException("cannot obtain connection to determine dbmssupport", e);
			}
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

	public Connection getConnectionWithTimeout(int timeout) throws JdbcException, TimeoutException {
		if (timeout<=0) {
			return getConnection();
		}
		TimeoutGuard tg = new TimeoutGuard("Connection ");
		try {
			tg.activateGuard(timeout);
			return getConnection();
		} finally {
			if (tg.cancel()) {
				throw new TimeoutException(getLogPrefix()+"thread has been interrupted");
			}
		}
	}

	@Override
	public void iterateOverStatistics(StatisticsKeeperIterationHandler hski, Object data, Action action) throws SenderException {
		hski.handleStatisticsKeeper(data, connectionStatistics);
	}

	@Override
	@Deprecated
	@ConfigurationWarning("We discourage the use of jmsRealms for datasources. To specify a datasource other then the default, use the datasourceName attribute directly, instead of referring to a realm")
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
		try {
			DataSource dataSource;
			try {
				dataSource = getDatasource();
			} catch (Exception e) {
				return "no datasource found for datasourceName ["+getDatasourceName()+"]";
			}
			//Try to minimise the amount of DB connections
			if(dataSource instanceof TransactionalDbmsSupportAwareDataSourceProxy) {
				return ((TransactionalDbmsSupportAwareDataSourceProxy) dataSource).getDestinationName();
			}

			try (Connection connection = getConnection()) {
				DatabaseMetaData metadata = connection.getMetaData();
				String result = metadata.getURL();

				String catalog=null;
				catalog=connection.getCatalog();
				result += catalog!=null ? ("/"+catalog):"";
				return result;
			}
		} catch (Exception e) {
			log.warn(getLogPrefix()+"exception retrieving PhysicalDestinationName", e);
		}
		return "unknown";
	}

	@IbisDoc({"JNDI name of datasource to be used, can be configured via jmsRealm, too", "${"+JndiDataSourceFactory.DEFAULT_DATASOURCE_NAME_PROPERTY+"}"})
	public void setDatasourceName(String datasourceName) {
		this.datasourceName = datasourceName;
	}
	public String getDatasourceName() {
		return datasourceName;
	}

	@IbisDoc({"Authentication alias used to authenticate when connecting to database", "" })
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}
	public String getAuthAlias() {
		return authAlias;
	}

	@IbisDoc({"User name for authentication when connecting to database, when none found from <code>authAlias</code>", ""})
	public void setUsername(String username) {
		this.username = username;
	}
	public String getUsername() {
		return username;
	}

	@IbisDoc({"Password for authentication when connecting to database, when none found from <code>authAlias</code>", ""})
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

	@IbisDoc({"informs the sender that the obtained connection is from a pool", "true"})
	public boolean isConnectionsArePooled() {
		return connectionsArePooled || isTransacted();
	}
	public void setConnectionsArePooled(boolean b) {
		connectionsArePooled = b;
	}

}
