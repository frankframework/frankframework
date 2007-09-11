/*
 * $Log: JdbcTableListener.java,v $
 * Revision 1.1  2007-09-11 11:53:01  europe\L190409
 * added JdbcListeners
 *
 */
package nl.nn.adapterframework.jdbc;

import nl.nn.adapterframework.configuration.ConfigurationException;

import org.apache.commons.lang.StringUtils;

/**
 * Database Listener that operates on a table having at least a key, message and status field.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.JdbcTableListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTableName(String) tableName}</td>  <td>name of the table to be used</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeyField(String) keyField}</td>  <td>primary key field of the table, used to identify messages</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageField(String) messageField}</td>  <td>field containing the message data</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStatusField(String) statusField}</td>  <td>field containing the status of the message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOrderField(String) orderField}</td>  <td>Field determining the order in which messages are processed</td><td>&nbsp;</td></tr>

 * <tr><td>{@link #setDatasourceName(String) datasourceName}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDatasourceNameXA(String) datasourceNameXA}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUsername(String) username}</td><td>username used to connect to datasource</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>password used to connect to datasource</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setConnectionsArePooled(boolean) connectionsArePooled}</td><td>when true, it is assumed that an connectionpooling mechanism is present. Before a message is sent, a new connection is obtained, that is closed after the message is sent. When transacted is true, connectionsArePooled is true, too</td><td>true</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public class JdbcTableListener extends JdbcListener {
	
	private String tableName;
	private String statusField;
	private String orderField;
//	private String timestampField;
	
	private String statusValueAvailable;
	private String statusValueInProcess;
	private String statusValueError;
	private String statusValueProcessed;
	
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getTableName())) {
			throw new ConfigurationException(getLogPrefix()+"must specifiy tableName");
		}
		if (StringUtils.isEmpty(getKeyField())) {
			throw new ConfigurationException(getLogPrefix()+"must specifiy keyField");
		}
		if (StringUtils.isEmpty(getMessageField())) {
			throw new ConfigurationException(getLogPrefix()+"must specifiy messageField");
		}
		if (StringUtils.isEmpty(getStatusField())) {
			throw new ConfigurationException(getLogPrefix()+"must specifiy statusField");
		}
		if (StringUtils.isEmpty(getStatusValueError())) {
			throw new ConfigurationException(getLogPrefix()+"must specifiy statusValueError");
		}
		if (StringUtils.isEmpty(getStatusValueInProcess())) {
			throw new ConfigurationException(getLogPrefix()+"must specifiy statusValueInProcess");
		}
		if (StringUtils.isEmpty(getStatusValueProcessed())) {
			throw new ConfigurationException(getLogPrefix()+"must specifiy statusValueProcessed");
		}
		setLockQuery("LOCK TABLE "+getTableName());
		setUnlockQuery("UNLOCK TABLE "+getTableName());
		setSelectQuery("SELECT FIRST "+getKeyField()+","+getMessageField()+
						" FROM "+getTableName()+
						" WHERE "+getStatusField()+
						(StringUtils.isNotEmpty(getStatusValueAvailable())?
						 "="+getStatusValueAvailable():
						 "NOT IN ["+getStatusValueError()+","+getStatusValueInProcess()+","+getStatusValueProcessed()+"]")+
						 (StringUtils.isNotEmpty(getOrderField())?
						 " ORDER BY "+getOrderField():""));
		setUpdateStatusToErrorQuery(getUpdateStatusQuery(getStatusValueError())); 
		setUpdateStatusToInProcessQuery(getUpdateStatusQuery(getStatusValueInProcess()));	
		setUpdateStatusToProcessedQuery(getUpdateStatusQuery(getStatusValueProcessed()));				 
	}

	protected String getUpdateStatusQuery(String fieldValue) {
		return "UPDATE "+getTableName()+ 
				" SET "+getStatusField()+"="+fieldValue+
				" WHERE "+getKeyField()+"=?";
	}



	public void setTableName(String string) {
		tableName = string;
	}
	public String getTableName() {
		return tableName;
	}

	public void setKeyField(String fieldname) {
		super.setKeyField(fieldname);
	}

	public void setMessageField(String fieldname) {
		super.setMessageField(fieldname);
	}

	public void setStatusField(String fieldname) {
		statusField = fieldname;
	}
	public String getStatusField() {
		return statusField;
	}

	public void setOrderField(String string) {
		orderField = string;
	}
	public String getOrderField() {
		return orderField;
	}

//	public void setTimestampField(String fieldname) {
//		timestampField = fieldname;
//	}
//	public String getTimestampField() {
//		return timestampField;
//	}


	public void setStatusValueAvailable(String string) {
		statusValueAvailable = string;
	}
	public String getStatusValueAvailable() {
		return statusValueAvailable;
	}

	public void setStatusValueError(String string) {
		statusValueError = string;
	}
	public String getStatusValueError() {
		return statusValueError;
	}

	public void setStatusValueInProcess(String string) {
		statusValueInProcess = string;
	}
	public String getStatusValueInProcess() {
		return statusValueInProcess;
	}

	public void setStatusValueProcessed(String string) {
		statusValueProcessed = string;
	}
	public String getStatusValueProcessed() {
		return statusValueProcessed;
	}


}
