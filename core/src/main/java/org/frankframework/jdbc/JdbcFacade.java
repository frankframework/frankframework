/*
   Copyright 2013 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.IXAEnabled;
import org.frankframework.core.TimeoutException;
import org.frankframework.dbms.DbmsSupportFactory;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.dbms.JdbcException;
import org.frankframework.jdbc.datasource.TransactionalDbmsSupportAwareDataSourceProxy;
import org.frankframework.jndi.JndiBase;
import org.frankframework.task.TimeoutGuard;
import org.frankframework.util.AppConstants;
import org.frankframework.util.CredentialFactory;

import lombok.Getter;
import lombok.Setter;

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
public class JdbcFacade extends JndiBase implements HasPhysicalDestination, IXAEnabled {
	private final @Getter String domain = "JDBC";
	private String datasourceName = null;
	@Getter private String authAlias = null;
	@Getter private String username = null;
	private String password = null;

	private boolean transacted = false;
	private boolean connectionsArePooled=true; // TODO: make this a property of the DataSourceFactory

	private DbmsSupportFactory dbmsSupportFactory=null;
	private IDbmsSupport dbmsSupport=null;
	private CredentialFactory cf=null;

	private @Setter @Getter IDataSourceFactory dataSourceFactory = null; // Spring should wire this!

	private DataSource datasource = null;

	protected String getLogPrefix() {
		return "["+this.getClass().getName()+"] ["+getName()+"] ";
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getDatasourceName())) {
			setDatasourceName(AppConstants.getInstance(getConfigurationClassLoader()).getProperty(IDataSourceFactory.DEFAULT_DATASOURCE_NAME_PROPERTY));
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
	}

	protected DataSource getDatasource() throws JdbcException {
		if (datasource==null) {
			String dsName = getDatasourceName();
			if(StringUtils.isBlank(dsName)) { //for getPhysicalDestinationName (console) when not yet configured.
				throw new JdbcException("No datasourceName specified");
			}

			try {
				datasource = getDataSourceFactory().getDataSource(dsName, getJndiEnv());
			} catch (NamingException | IllegalStateException e) {
				throw new JdbcException("Could not find Datasource ["+dsName+"]", e);
			}
			if (datasource==null) {
				throw new JdbcException("Could not find Datasource ["+dsName+"]");
			}

			String dsinfo = datasource.toString();
			log.info("{}looked up Datasource [{}]: [{}]", getLogPrefix(), dsName, dsinfo);
		}
		return datasource;
	}

	@Deprecated
	public String getDatasourceInfo() throws JdbcException {
		if(getDatasource() instanceof TransactionalDbmsSupportAwareDataSourceProxy) {
			return ((TransactionalDbmsSupportAwareDataSourceProxy) getDatasource()).getInfo();
		}
		throw new IllegalStateException("Datasource should always be of type TransactionalDbmsSupportAwareDataSourceProxy, found: " + getDatasource().getClass().getName());
	}

	public void setDbmsSupportFactory(DbmsSupportFactory dbmsSupportFactory) {
		this.dbmsSupportFactory=dbmsSupportFactory;
	}

	public IDbmsSupport getDbmsSupport() {
		if (dbmsSupport != null) {
			return dbmsSupport;
		}
		try {
			if (datasource instanceof TransactionalDbmsSupportAwareDataSourceProxy proxy) {
				Map<String, String> md = proxy.getMetaData();
				dbmsSupport = dbmsSupportFactory.getDbmsSupport(md.get("product"), md.get("product-version"));
			}
			if (dbmsSupport == null) {
				dbmsSupport = dbmsSupportFactory.getDbmsSupport(getDatasource());
			}
		} catch (JdbcException | SQLException e) {
			throw new IllegalStateException("cannot obtain connection to determine dbmsSupport", e);
		}
		return dbmsSupport;
	}

	/**
	 * Obtains a connection to the datasource.
	 */
	// TODO: consider making this one protected.
	public Connection getConnection() throws JdbcException {
		DataSource ds = getDatasource();
		try {
			if (cf!=null) {
				return ds.getConnection(cf.getUsername(), cf.getPassword());
			}
			return ds.getConnection();
		} catch (SQLException e) {
			throw new JdbcException(getLogPrefix()+"cannot open connection on datasource ["+getDatasourceName()+"]", e);
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
	@Deprecated(forRemoval = true, since = "7.7.0")
	@ConfigurationWarning("We discourage the use of jmsRealms for datasources. To specify a datasource other then the default, use the datasourceName attribute directly, instead of referring to a realm")
	public void setJmsRealm(String jmsRealmName) {
		super.setJmsRealm(jmsRealmName); //super.setJmsRealm(...) sets the jmsRealmName only when a realm is found
		if(StringUtils.isEmpty(getJmsRealmName())) { //confirm that the configured jmsRealm exists
			throw new IllegalStateException("JmsRealm ["+jmsRealmName+"] not found");
		}
	}

	/**
	 * Returns the name and location of the database that this objects operates on.
	 * If no previous connection was made or it cannot determine the destination
	 * it returns 'unknown'
	 *
	 * @see HasPhysicalDestination#getPhysicalDestinationName()
	 */
	@Override
	public String getPhysicalDestinationName() {
		try {
			DataSource dataSource = getDatasource();
			if(dataSource instanceof TransactionalDbmsSupportAwareDataSourceProxy proxy) {
				String dName = proxy.getDestinationName();
				if(dName != null) return dName;
			}
		} catch (JdbcException e) {
			return "no datasource found for datasourceName ["+getDatasourceName()+"]";
		}
		return "unknown";
	}

	/**
	 * JNDI name of datasource to be used, can be configured via jmsRealm, too
	 * @ff.default {@value IDataSourceFactory#DEFAULT_DATASOURCE_NAME_PROPERTY}
	 */
	public void setDatasourceName(String datasourceName) {
		this.datasourceName = datasourceName;
	}
	public String getDatasourceName() {
		return datasourceName;
	}

	/** Authentication alias used to authenticate when connecting to database */
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}

	/** User name for authentication when connecting to database, when none found from <code>authAlias</code> */
	public void setUsername(String username) {
		this.username = username;
	}

	/** Password for authentication when connecting to database, when none found from <code>authAlias</code> */
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

	/**
	 * informs the sender that the obtained connection is from a pool (and thus connections are reused and never closed)
	 * @ff.default true
	 */
	public void setConnectionsArePooled(boolean b) {
		connectionsArePooled = b;
	}
	public boolean isConnectionsArePooled() {
		return connectionsArePooled || isTransacted();
	}

}
