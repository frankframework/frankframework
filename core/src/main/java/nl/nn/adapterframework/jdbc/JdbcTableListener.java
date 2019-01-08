/*
   Copyright 2013 Nationale-Nederlanden

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

import nl.nn.adapterframework.configuration.ConfigurationException;

import nl.nn.adapterframework.doc.IbisDoc;
import org.apache.commons.lang.StringUtils;

/**
 * Database Listener that operates on a table having at least a key and a status field.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.jdbc.JdbcTableListener</td><td>&nbsp;</td></tr>
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



	@IbisDoc({"name of the table to be used", " "})
	public void setTableName(String string) {
		tableName = string;
	}
	public String getTableName() {
		return tableName;
	}

	@IbisDoc({"primary key field of the table, used to identify messages", " "})
	public void setKeyField(String fieldname) {
		super.setKeyField(fieldname);
	}

	@IbisDoc({"(optional) field containing the message data", "<i>same as keyfield</i>"})
	public void setMessageField(String fieldname) {
		super.setMessageField(fieldname);
	}

	@IbisDoc({"field containing the status of the message", " "})
	public void setStatusField(String fieldname) {
		statusField = fieldname;
	}
	public String getStatusField() {
		return statusField;
	}

	@IbisDoc({"(optional) field determining the order in which messages are processed", " "})
	public void setOrderField(String string) {
		orderField = string;
	}
	public String getOrderField() {
		return orderField;
	}

	@IbisDoc({"(optional) field used to store the date and time of the last change of the status field", " "})
	public void setTimestampField(String fieldname) {
		timestampField = fieldname;
	}
	public String getTimestampField() {
		return timestampField;
	}

	@IbisDoc({"(optional) value of status field indicating row is available to be processed. if not specified, any row not having any of the other status values is considered available.", " "})
	public void setStatusValueAvailable(String string) {
		statusValueAvailable = string;
	}
	public String getStatusValueAvailable() {
		return statusValueAvailable;
	}

	@IbisDoc({"value of status field indicating the processing of the row resulted in an error", " "})
	public void setStatusValueError(String string) {
		statusValueError = string;
	}
	public String getStatusValueError() {
		return statusValueError;
	}

	@IbisDoc({"value of status field indicating row is processed ok", " "})
	public void setStatusValueProcessed(String string) {
		statusValueProcessed = string;
	}
	public String getStatusValueProcessed() {
		return statusValueProcessed;
	}

}
