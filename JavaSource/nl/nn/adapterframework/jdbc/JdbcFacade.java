/*
 * $Log: JdbcFacade.java,v $
 * Revision 1.17  2007-05-23 09:08:53  europe\L190409
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
import java.util.Date;

import javax.naming.NamingException;
import javax.sql.DataSource;

import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IXAEnabled;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jms.JNDIBase;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Provides functions for JDBC connections.
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class JdbcFacade extends JNDIBase implements INamedObject, HasPhysicalDestination, IXAEnabled {
	public static final String version="$RCSfile: JdbcFacade.java,v $ $Revision: 1.17 $ $Date: 2007-05-23 09:08:53 $";
    protected Logger log = LogUtil.getLogger(this);
	
	public final static int DATABASE_GENERIC=0;
	public final static int DATABASE_ORACLE=1;
	
	private String name;
    private String username=null;
    private String password=null;
    
	private DataSource datasource = null;
	private String datasourceName = null;
	private String datasourceNameXA = null;

	private boolean transacted = false;
	private boolean connectionsArePooled=true;
	
	private int databaseType=-1;

	protected String getLogPrefix() {
		return "["+this.getClass().getName()+"] ["+getName()+"] ";
	}

	/**
	 * Returns either {@link #getDatasourceName() datasourceName} or {@link #getDatasourceNameXA() datasourceNameXA},
	 * depending on the value of {@link #isTransacted()}.
	 * If the right one is not specified, the other is used. 
	 */
	public String getDataSourceNameToUse() throws JdbcException {
		String result = isTransacted() ? getDatasourceNameXA() : getDatasourceName();
		if (StringUtils.isEmpty(result)) {
			// try the alternative...
			result = isTransacted() ? getDatasourceName() : getDatasourceNameXA();
			if (StringUtils.isEmpty(result)) {
				throw new JdbcException(getLogPrefix()+"neither datasourceName nor datasourceNameXA are specified");
			}
			log.warn(getLogPrefix()+"correct datasourceName attribute not specified, will use ["+result+"]");
		}
		return result;
	}

	protected DataSource getDatasource() throws JdbcException {
		if (datasource==null) {
				String dsName = getDataSourceNameToUse();
				try {
					log.debug(getLogPrefix()+"looking up Datasource ["+dsName+"]");
					datasource =(DataSource) getContext().lookup( dsName );
					log.debug(getLogPrefix()+"looked up Datasource ["+dsName+"]: ["+datasource+"]");
				} catch (NamingException e) {
					try {
						String tomcatDsName="java:comp/env/"+dsName;
						log.debug(getLogPrefix()+"could not find ["+dsName+"], now trying ["+tomcatDsName+"]");
						datasource =(DataSource) getContext().lookup( tomcatDsName );
						log.debug(getLogPrefix()+"looked up Datasource ["+tomcatDsName+"]: ["+datasource+"]");
					} catch (NamingException e2) {
						throw new JdbcException(getLogPrefix()+"cannot find Datasource ["+dsName+"]", e);
					}
				}
		}
		return datasource;
	}
	
	public void setDatabaseType(int type) {
		databaseType=type;
	}

	public int getDatabaseType() throws SQLException, JdbcException {
		if (databaseType<0) {
			Connection conn=getConnection();
			try {
				DatabaseMetaData md=conn.getMetaData();
				String product=md.getDatabaseProductName();
				String driver=md.getDriverName();
				log.info("Database Metadata: product ["+product+"] driver ["+driver+"]");
				if ("Oracle".equals(product)) {
					log.debug("Setting databasetype to ORACLE");
					databaseType=DATABASE_ORACLE;
				} else {
					log.debug("Setting databasetype to GENERIC");
					databaseType=DATABASE_GENERIC;
				}
			} finally {
				conn.close();
			}
		}
		return databaseType;
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

			if (Parameter.TYPE_DATE.equals(paramType) || 
				Parameter.TYPE_DATETIME.equals(paramType)) {
				statement.setDate(i+1, new java.sql.Date(((Date)value).getTime()));
			} else if (Parameter.TYPE_TIME.equals(paramType)) {
				statement.setTime(i+1, new java.sql.Time(((Date)value).getTime()));
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
		this.datasourceNameXA = datasourceNameXA;
	}
	public String getDatasourceNameXA() {
		return datasourceNameXA;
	}

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
