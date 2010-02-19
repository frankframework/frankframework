/*
 * $Log: JdbcSenderBase.java,v $
 * Revision 1.8  2010-02-19 13:45:28  m00f069
 * - Added support for (sender) stubbing by debugger
 * - Added reply listener and reply sender to debugger
 * - Use IbisDebuggerDummy by default
 * - Enabling/disabling debugger handled by debugger instead of log level
 * - Renamed messageId to correlationId in debugger interface
 *
 * Revision 1.7  2009/04/01 08:22:10  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added TimeOutException to SendMessage()
 *
 * Revision 1.6  2007/06/19 12:09:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improve javadoc
 *
 * Revision 1.5  2006/12/12 09:57:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * restore jdbc package
 *
 * Revision 1.3  2005/08/25 15:45:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.debug.IbisDebugger;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.senders.SenderBase;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Base class for building JDBC-senders.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.JdbcSenderBase</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the sender</td><td>&nbsp;</td></tr>

 * <tr><td>{@link #setDatasourceName(String) datasourceName}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDatasourceNameXA(String) datasourceNameXA}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUsername(String) username}</td><td>username used to connect to datasource</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>password used to connect to datasource</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setConnectionsArePooled(boolean) connectionsArePooled}</td><td>when true, it is assumed that an connectionpooling mechanism is present. Before a message is sent, a new connection is obtained, that is closed after the message is sent. When transacted is true, connectionsArePooled is true, too</td><td>true</td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>&nbsp;</td><td>false</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>

 *  * </p>
 * @version Id
 * @author  Gerrit van Brakel
 * @since 	4.2.h
 */
public abstract class JdbcSenderBase extends JdbcFacade implements ISenderWithParameters {
	public static final String version="$RCSfile: JdbcSenderBase.java,v $ $Revision: 1.8 $ $Date: 2010-02-19 13:45:28 $";

	private IbisDebugger ibisDebugger;

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
	
	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
		return sendMessage(correlationID, message, null);
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		message = ibisDebugger.senderInput(this, correlationID, message);
		String result = null;
		try {
			if (!ibisDebugger.stubSender(this, correlationID)) {
				if (isConnectionsArePooled()) {
					Connection c = null;
					try {
						c = getConnection();
						result = sendMessage(c, correlationID, message, prc);
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
						result = sendMessage(connection, correlationID, message, prc);
					}
				}
			}
		} catch(Throwable throwable) {
			throwable = ibisDebugger.senderAbort(this, correlationID, throwable);
			SenderBase.throwSenderOrTimeOutException(this, throwable);
		}
		return ibisDebugger.senderOutput(this, correlationID, result);
	}

	protected abstract String sendMessage(Connection connection, String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException;

	public String toString() {
		String result  = super.toString();
        ToStringBuilder ts=new ToStringBuilder(this);
        ts.append("name", getName() );
        ts.append("version", version);
        result += ts.toString();
        return result;
	}
	
	public void setIbisDebugger(IbisDebugger ibisDebugger) {
		this.ibisDebugger = ibisDebugger;
	}

}
