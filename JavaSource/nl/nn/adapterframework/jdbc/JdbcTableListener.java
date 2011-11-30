/*
 * $Log: JdbcTableListener.java,v $
 * Revision 1.10  2011-11-30 13:51:43  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.8  2011/03/16 16:42:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of DbmsSupport, including support for MS SQL Server
 *
 * Revision 1.7  2009/08/04 11:24:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for messages in CLOBs and BLOBs
 *
 * Revision 1.6  2008/12/10 08:35:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved locking and selection mechanism: now works in multiple threads. 
 * improved disaster recovery: no more specific 'in process' status, rolls back to original state (where apropriate)
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
 * <tr><td>{@link #setMessageFieldType(String) messageFieldType}</td>  <td>type of the field containing the message data: either String, clob or blob</td><td><i>String</i></td></tr>
 * <tr><td>{@link #setBlobCharset(String) blobCharset}</td><td>charset used to read BLOBs</td><td>UTF-8</td></tr>
 * <tr><td>{@link #setBlobsCompressed(boolean) blobsCompressed}</td><td>controls whether blobdata is considered stored compressed in the database</td><td>true</td></tr>
 * <tr><td>{@link #setBlobSmartGet(boolean) blobSmartGet}</td><td>controls automatically whether blobdata is stored compressed and/or serialized in the database</td><td>false</td></tr>

 * <tr><td>{@link #setStatusValueAvailable(String) statusValueAvailable}</td> <td>(optional) value of status field indicating row is available to be processed. If not specified, any row not having any of the other status values is considered available.</td><td>&nbsp;</td></tr>
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
	private String statusValueProcessed;
	private String statusValueError;
	
	public void configure() throws ConfigurationException {
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
		if (StringUtils.isEmpty(getStatusValueProcessed())) {
			throw new ConfigurationException(getLogPrefix()+"must specifiy statusValueProcessed");
		}
		setSelectQuery("SELECT "+getKeyField()+
						(StringUtils.isNotEmpty(getMessageField())?","+getMessageField():"")+
						" FROM "+getTableName()+
						" WHERE "+getStatusField()+
						(StringUtils.isNotEmpty(getStatusValueAvailable())?
						 "='"+getStatusValueAvailable()+"'":
						 " NOT IN ('"+getStatusValueError()+"','"+getStatusValueProcessed()+"')")+
						 (StringUtils.isNotEmpty(getOrderField())?
						 " ORDER BY "+getOrderField():""));
		setUpdateStatusToProcessedQuery(getUpdateStatusQuery(getStatusValueProcessed()));				 
		setUpdateStatusToErrorQuery(getUpdateStatusQuery(getStatusValueError())); 
		super.configure();
	}

	protected String getUpdateStatusQuery(String fieldValue) {
		return "UPDATE "+getTableName()+ 
				" SET "+getStatusField()+"='"+fieldValue+"'"+
				(StringUtils.isNotEmpty(getTimestampField())?","+getTimestampField()+"="+getDbmsSupport().getSysDate():"")+
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

	public void setStatusValueProcessed(String string) {
		statusValueProcessed = string;
	}
	public String getStatusValueProcessed() {
		return statusValueProcessed;
	}

}
