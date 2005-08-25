/*
 * $Log: JdbcSenderBase.java,v $
 * Revision 1.3  2005-08-25 15:45:47  europe\L190409
 * close connection in a finally clause
 *
 * Revision 1.2  2005/05/31 09:55:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implemented attribute 'connectionsArePooled'
 *
 * Revision 1.1  2005/04/26 15:20:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced JdbcSenderBase, with non-sql oriented basics
 *
 */
package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Base class for building JDBC-senders.
 *
 * @version Id
 * @author  Gerrit van Brakel
 * @since 	4.2.h
 */
public abstract class JdbcSenderBase extends JdbcFacade implements ISenderWithParameters {
	public static final String version="$RCSfile: JdbcSenderBase.java,v $ $Revision: 1.3 $ $Date: 2005-08-25 15:45:47 $";

	protected Connection connection=null;
	protected ParameterList paramList = null;

	public JdbcSenderBase() {
		super();
	}


	public void addParameter(Parameter p) { 
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

	public void configure(ParameterList parameterList) throws ConfigurationException {
		configure();		
	}

	public void configure() throws ConfigurationException {
		try {
			if (getDatasource()==null) {
				throw new ConfigurationException(getLogPrefix()+"has no datasource");
			}
		} catch (JdbcException e) {
			throw new ConfigurationException(e);
		}
		if (paramList!=null) {
			paramList.configure();
		}
	}

	public void open() throws SenderException {
		if (!isConnectionsArePooled()) {
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
	
	public String sendMessage(String correlationID, String message) throws SenderException {
		return sendMessage(correlationID, message, null);
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException {
		if (isConnectionsArePooled()) {
			Connection c = null;
			try {
				c = getConnection();
				String result = sendMessage(c, correlationID, message, prc);
				return result;
			} catch (JdbcException e) {
				throw new SenderException(e);
			} finally {
				if (c!=null) {
					try {
						c.close();
					} catch (SQLException e) {
						log.warn(new SenderException(getLogPrefix() + "caught exception closing sender after sending message, ID=["+correlationID+"]", e));
					}
				}
			}
			
		} else {
			synchronized (connection) {
				return sendMessage(connection, correlationID, message, prc);
			}
		}
	}

	protected abstract String sendMessage(Connection connection, String correlationID, String message, ParameterResolutionContext prc) throws SenderException;

	public String toString() {
		String result  = super.toString();
        ToStringBuilder ts=new ToStringBuilder(this);
        ts.append("name", getName() );
        ts.append("version", version);
        result += ts.toString();
        return result;
	}
	

}
