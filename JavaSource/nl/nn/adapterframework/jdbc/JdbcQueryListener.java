/*
 * $Log: JdbcQueryListener.java,v $
 * Revision 1.1  2008-02-28 16:22:45  europe\L190409
 * first version
 *
 */
package nl.nn.adapterframework.jdbc;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;

/**

/**
 * Database Listener that operates on a table having at least a key and a status field.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.JdbcQueryListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeyField(String) keyField}</td>  <td>primary key field of the table, used to identify messages</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageField(String) messageField}</td>  <td>(optional) field containing the message data</td><td><i>same as keyField</i></td></tr>

 * <tr><td>{@link #setSelectQuery(String) selectQuery}</td> <td>query that returns a row to be processed. Must contain a key field and optionally a message field</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUpdateStatusToProcessedQuery(String) updateStatusToProcessedQuery}</td> <td>SQL Statement to the status of a row to 'processed'. Must contain one parameter, that is set to the value of the key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUpdateStatusToErrorQuery(String) updateStatusToErrorQuery}</td> <td>SQL Statement to the status of a row to 'error'. Must contain one parameter, that is set to the value of the key</td><td>same as <code>updateStatusToProcessedQuery</code></td></tr>

 * <tr><td>{@link #setDatasourceName(String) datasourceName}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUsername(String) username}</td><td>username used to connect to datasource</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>password used to connect to datasource</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setConnectionsArePooled(boolean) connectionsArePooled}</td><td>when true, it is assumed that an connectionpooling mechanism is present. Before a message is sent, a new connection is obtained, that is closed after the message is sent. When transacted is true, connectionsArePooled is true, too</td><td>true</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public class JdbcQueryListener extends JdbcListener {

	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getSelectQuery())) {
			throw new ConfigurationException("selectQuery must be specified");
		}
		if (StringUtils.isEmpty(getUpdateStatusToProcessedQuery())) {
			throw new ConfigurationException("updateStatusToProcessedQuery must be specified");
		}
		if (StringUtils.isEmpty(getKeyField())) {
			throw new ConfigurationException("keyField must be specified");
		}
	}
	

	public void setKeyField(String fieldname) {
		super.setKeyField(fieldname);
	}

	public void setMessageField(String fieldname) {
		super.setMessageField(fieldname);
	}

	public void setSelectQuery(String string) {
		super.setSelectQuery(string);
	}

	public void setUpdateStatusToErrorQuery(String string) {
		super.setUpdateStatusToErrorQuery(string);
	}

	public void setUpdateStatusToInProcessQuery(String string) {
		super.setUpdateStatusToInProcessQuery(string);
	}

	public void setUpdateStatusToProcessedQuery(String string) {
		super.setUpdateStatusToProcessedQuery(string);
	}


}
