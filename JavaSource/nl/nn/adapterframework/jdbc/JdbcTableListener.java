/*
 * $Log: JdbcTableListener.java,v $
 * Revision 1.4.10.1  2008-04-03 08:12:39  europe\L190409
 * synch from HEAD
 *
 * Revision 1.5  2008/02/28 16:21:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.4  2007/10/02 09:17:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added tablename to physical destination
 *
 * Revision 1.3  2007/09/17 07:44:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * limit query to single row
 *
 * Revision 1.2  2007/09/12 09:26:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first working version
 *
 * Revision 1.1  2007/09/11 11:53:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added JdbcListeners
 *
 */
package nl.nn.adapterframework.jdbc;

import nl.nn.adapterframework.configuration.ConfigurationException;

import org.apache.commons.lang.StringUtils;

/**
 * Database Listener that operates on a table having at least a key and a status field.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.JdbcTableListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTableName(String) tableName}</td>  <td>name of the table to be used</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setKeyField(String) keyField}</td>  <td>primary key field of the table, used to identify messages</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStatusField(String) statusField}</td>  <td>field containing the status of the message</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageField(String) messageField}</td>  <td>(optional) field containing the message data</td><td><i>same as keyField</i></td></tr>
 * <tr><td>{@link #setOrderField(String) orderField}</td>  <td>(optional) field determining the order in which messages are processed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTimestampField(String) timestampField}</td>  <td>(optional) field used to store the date and time of the last change of the status field</td><td>&nbsp;</td></tr>

 * <tr><td>{@link #setStatusValueAvailable(String) statusValueAvailable}</td> <td>(optional) value of status field indicating row is available to be processed. If not specified, any row not having any of the other status values is considered available.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStatusValueInProcess(String) statusValueInProcess}</td> <td>value of status field indicating row is currently being processed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStatusValueProcessed(String) statusValueProcessed}</td> <td>value of status field indicating row is processed OK</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStatusValueError(String) statusValueError}</td>         <td>value of status field indicating the processing of the row resulted in an error</td><td>&nbsp;</td></tr>

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
public class JdbcTableListener extends JdbcListener {
	
	private String tableName;
	private String statusField;
	private String orderField;
	private String timestampField;
	
	private String statusValueAvailable;
	private String statusValueInProcess;
	private String statusValueProcessed;
	private String statusValueError;
	
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getTableName())) {
			throw new ConfigurationException(getLogPrefix()+"must specifiy tableName");
		}
		if (StringUtils.isEmpty(getKeyField())) {
			throw new ConfigurationException(getLogPrefix()+"must specifiy keyField");
		}
		if (StringUtils.isEmpty(getStatusField())) {
			throw new ConfigurationException(getLogPrefix()+"must specifiy statusField");
		}
		if (StringUtils.isEmpty(getMessageField())) {
			log.info(getLogPrefix()+"has no messageField specified. Will use keyField as messageField, too");
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
		setLockQuery("LOCK TABLE "+getTableName()+" IN EXCLUSIVE MODE NOWAIT");
		//setUnlockQuery("UNLOCK TABLE "+getTableName());
		setSelectQuery("SELECT "+getKeyField()+
						(StringUtils.isNotEmpty(getMessageField())?","+getMessageField():"")+
						" FROM "+getTableName()+
						" WHERE "+getStatusField()+
						(StringUtils.isNotEmpty(getStatusValueAvailable())?
						 "='"+getStatusValueAvailable()+"'":
						 " NOT IN ('"+getStatusValueError()+"','"+getStatusValueInProcess()+"','"+getStatusValueProcessed()+"')")+
						 " AND ROWNUM=1"+
						 (StringUtils.isNotEmpty(getOrderField())?
						 " ORDER BY "+getOrderField():""));
		setUpdateStatusToInProcessQuery(getUpdateStatusQuery(getStatusValueInProcess()));	
		setUpdateStatusToProcessedQuery(getUpdateStatusQuery(getStatusValueProcessed()));				 
		setUpdateStatusToErrorQuery(getUpdateStatusQuery(getStatusValueError())); 
	}

	protected String getUpdateStatusQuery(String fieldValue) {
		return "UPDATE "+getTableName()+ 
				" SET "+getStatusField()+"='"+fieldValue+"'"+
				(StringUtils.isNotEmpty(getTimestampField())?","+getTimestampField()+"=SYSDATE":"")+
				" WHERE "+getKeyField()+"=?";
	}

	public String getPhysicalDestinationName() {
		return super.getPhysicalDestinationName()+" "+getTableName();
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

	public void setTimestampField(String fieldname) {
		timestampField = fieldname;
	}
	public String getTimestampField() {
		return timestampField;
	}

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
