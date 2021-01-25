/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IProvidesMessageBrowsers;
import nl.nn.adapterframework.doc.IbisDoc;
import org.apache.commons.lang.StringUtils;

/**
 * Database Listener that operates on a table having at least a key and a status field.
 *
 * @since   4.7
 */
public class JdbcTableListener extends JdbcListener implements IProvidesMessageBrowsers<Object> {
	
	private String tableName;
	private String statusField;
	private String orderField;
	private String timestampField;
	private String selectCondition;
	
	private String statusValueAvailable;
	private String statusValueProcessed;
	private String statusValueError;
	
	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getTableName())) {
			throw new ConfigurationException(getLogPrefix()+"must specify tableName");
		}
		if (StringUtils.isEmpty(getKeyField())) {
			throw new ConfigurationException(getLogPrefix()+"must specify keyField");
		}
		if (StringUtils.isEmpty(getStatusField())) {
			throw new ConfigurationException(getLogPrefix()+"must specify statusField");
		}
		if (StringUtils.isEmpty(getMessageField())) {
			log.info(getLogPrefix()+"has no messageField specified. Will use keyField as messageField, too");
		}
		if (StringUtils.isEmpty(getStatusValueError())) {
			throw new ConfigurationException(getLogPrefix()+"must specify statusValueError");
		}
		if (StringUtils.isEmpty(getStatusValueProcessed())) {
			throw new ConfigurationException(getLogPrefix()+"must specify statusValueProcessed");
		}
		setSelectQuery("SELECT "+getKeyField()+
						(StringUtils.isNotEmpty(getMessageField())?","+getMessageField():"")+
						" FROM "+getTableName()+
						" WHERE "+getStatusField()+
						(StringUtils.isNotEmpty(getStatusValueAvailable())?
						 "='"+getStatusValueAvailable()+"'":
						 " NOT IN ('"+getStatusValueError()+"','"+getStatusValueProcessed()+"')")+
						(StringUtils.isNotEmpty(getSelectCondition()) ? " AND ("+getSelectCondition()+")": "") +
						 (StringUtils.isNotEmpty(getOrderField())? " ORDER BY "+getOrderField():""));
		setUpdateStatusToProcessedQuery(getUpdateStatusQuery(getStatusValueProcessed(),null));
		setUpdateStatusToErrorQuery(getUpdateStatusQuery(getStatusValueError(),null)); 
		super.configure();
	}

	protected String getUpdateStatusQuery(String fieldValue, String additionalSetClause) {
		return "UPDATE "+getTableName()+ 
				" SET "+getStatusField()+"='"+fieldValue+"'"+
				(StringUtils.isNotEmpty(getTimestampField())?","+getTimestampField()+"="+getDbmsSupport().getSysDate():"")+
				(StringUtils.isNotEmpty(additionalSetClause)?","+additionalSetClause:"")+
				" WHERE "+getKeyField()+"=?";
	}

	@Override
	public IMessageBrowser<Object> getMessageLogBrowser() {
		if (StringUtils.isEmpty(getStatusValueProcessed())) {
			return null;
		}
		return new JdbcTableMessageBrowser<Object>(this,getStatusValueProcessed(), IMessageBrowser.StorageType.MESSAGELOG_RECEIVER);
	}

	@Override
	public IMessageBrowser<Object> getErrorStoreBrowser() {
		if (StringUtils.isEmpty(getStatusValueError())) {
			return null;
		}
		return new JdbcTableMessageBrowser<Object>(this,getStatusValueError(), IMessageBrowser.StorageType.ERRORSTORAGE);
	}


	
	@Override
	public String getPhysicalDestinationName() {
		return super.getPhysicalDestinationName()+" "+getTableName();
	}



	@IbisDoc({"1", "Name of the table to be used", ""})
	public void setTableName(String string) {
		tableName = string;
	}
	public String getTableName() {
		return tableName;
	}

	@Override
	public void setKeyField(String fieldname) {
		super.setKeyField(fieldname);
	}

	@Override
	public void setMessageField(String fieldname) {
		super.setMessageField(fieldname);
	}

	@IbisDoc({"4", "Field containing the status of the message", ""})
	public void setStatusField(String fieldname) {
		statusField = fieldname;
	}
	public String getStatusField() {
		return statusField;
	}

	@IbisDoc({"5", "(optional) Field determining the order in which messages are processed", ""})
	public void setOrderField(String string) {
		orderField = string;
	}
	public String getOrderField() {
		return orderField;
	}

	@IbisDoc({"6", "(optional) Field used to store the date and time of the last change of the statusField", ""})
	public void setTimestampField(String fieldname) {
		timestampField = fieldname;
	}
	public String getTimestampField() {
		return timestampField;
	}

	@IbisDoc({"7", "(optional) Value of statusField indicating row is available to be processed. If not specified, any row not having any of the other status values is considered available.", ""})
	public void setStatusValueAvailable(String string) {
		statusValueAvailable = string;
	}
	public String getStatusValueAvailable() {
		return statusValueAvailable;
	}

	@IbisDoc({"8", "Value of statusField indicating the processing of the row resulted in an error", ""})
	public void setStatusValueError(String string) {
		statusValueError = string;
	}
	public String getStatusValueError() {
		return statusValueError;
	}

	@IbisDoc({"9", "Value of status field indicating row is processed ok", ""})
	public void setStatusValueProcessed(String string) {
		statusValueProcessed = string;
	}
	public String getStatusValueProcessed() {
		return statusValueProcessed;
	}

	@IbisDoc({"10", "Additional condition for a row to belong to this TableListener", ""})
	public void setSelectCondition(String string) {
		selectCondition = string;
	}
	public String getSelectCondition() {
		return selectCondition;
	}

}
