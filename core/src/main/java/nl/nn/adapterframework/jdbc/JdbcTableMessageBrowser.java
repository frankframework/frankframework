/*
   Copyright 2020 WeAreFrank!

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

import java.util.Date;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Misc;

public class JdbcTableMessageBrowser<M> extends JdbcMessageBrowser<M> {

	private String tableName=null;
	private String indexName=null;
	private String selectCondition=null;
	private JdbcFacade parent=null;
	

	private static final String PROPERTY_USE_INDEX_HINT=CONTROL_PROPERTY_PREFIX+"useIndexHint";
	private static final String PROPERTY_USE_FIRST_ROWS_HINT=CONTROL_PROPERTY_PREFIX+"useFirstRowsHint";
	
	protected boolean useIndexHint;
	private boolean useFirstRowsHint;

	public JdbcTableMessageBrowser() {
		super();
	}
	
	public JdbcTableMessageBrowser(JdbcTableListener tableListener, String statusValue, StorageType storageType) {
		this();
		parent=tableListener;
		setKeyField(tableListener.getKeyField());
		setIdField(tableListener.getKeyField());
		setTableName(tableListener.getTableName());
		setMessageField(StringUtils.isNotEmpty(tableListener.getMessageField())?tableListener.getMessageField():tableListener.getKeyField());
		setDateField(tableListener.getTimestampField());
		setType(storageType.getCode());
		selectCondition=Misc.concatStrings(tableListener.getStatusField()+ "='"+statusValue+"'", " AND ", tableListener.getSelectCondition());
	}

	@Override
	protected String getLogPrefix() {
		return "JdbcTableMessageBrowser ["+getName()+"] ";
	}

	@Override
	public void configure() throws ConfigurationException {
		try {
			copyFacadeSettings(parent);
		} catch (JdbcException e) {
			throw new ConfigurationException(e);
		}
		super.configure();
		if (StringUtils.isEmpty(getTableName())) {
			throw new ConfigurationException("Attribute [tableName] is not set");
		}
		if (StringUtils.isEmpty(getKeyField())) {
			throw new ConfigurationException("Attribute [keyField] is not set");
		}
		createQueryTexts(getDbmsSupport());
	}
	
	@Override
	protected void setOperationControls() {
		super.setOperationControls();
		AppConstants ac = AppConstants.getInstance();
		useIndexHint = ac.getBoolean(PROPERTY_USE_INDEX_HINT, false);
		useFirstRowsHint = ac.getBoolean(PROPERTY_USE_FIRST_ROWS_HINT, true);
	}


	
	protected void createQueryTexts(IDbmsSupport dbmsSupport) throws ConfigurationException {
		deleteQuery = "DELETE FROM "+getPrefix()+getTableName()+ getWhereClause(getKeyField()+"=?",true);
		String listClause=getListClause();
		selectContextQuery = "SELECT "+listClause+ getWhereClause(getKeyField()+"=?",true);
		selectDataQuery = "SELECT "+getMessageField()+  " FROM "+getPrefix()+getTableName()+ getWhereClause(getKeyField()+"=?",true);
		checkMessageIdQuery = "SELECT "+provideIndexHintAfterFirstKeyword(dbmsSupport) + getIdField() +" FROM "+getPrefix()+getTableName()+ getWhereClause(getIdField() +"=?",false);
		checkCorrelationIdQuery = "SELECT "+provideIndexHintAfterFirstKeyword(dbmsSupport) + getCorrelationIdField() +" FROM "+getPrefix()+getTableName()+ getWhereClause(getCorrelationIdField() +"=?",false);
		try {
			getMessageCountQuery = dbmsSupport.prepareQueryTextForNonLockingRead("SELECT "+provideIndexHintAfterFirstKeyword(dbmsSupport) + "COUNT(*) FROM "+getPrefix()+getTableName()+ getWhereClause(null,false));
		} catch (JdbcException e) {
			throw new ConfigurationException("Cannot create getMessageCountQuery", e);
		}
	}

	private String getListClause() {
		return getKeyField()+
		(StringUtils.isNotEmpty(getIdField())?","+getIdField():"")+
		(StringUtils.isNotEmpty(getCorrelationIdField())?","+getCorrelationIdField():"")+
		(StringUtils.isNotEmpty(getDateField())?","+getDateField():"")+
		(StringUtils.isNotEmpty(getExpiryDateField())?","+getExpiryDateField():"")+
		(StringUtils.isNotEmpty(getTypeField())?","+getTypeField():"")+
		(StringUtils.isNotEmpty(getHostField())?","+getHostField():"")+
		(StringUtils.isNotEmpty(getLabelField())?","+getLabelField():"")+
		(StringUtils.isNotEmpty(getCommentField())?","+getCommentField():"")+
		" FROM "+getPrefix()+getTableName();
	}
	
	@Override
	protected String getSelectListQuery(IDbmsSupport dbmsSupport, Date startTime, Date endTime, IMessageBrowser.SortOrder order) {
		String whereClause=null;
		if (startTime!=null) {
			whereClause=getDateField()+">=?";
		}
		if (endTime!=null) {
			whereClause=Misc.concatStrings(whereClause, " AND ", getDateField()+"<?");
		}
		if(order.equals(SortOrder.NONE)) { //If no order has been set, use the default (DESC for messages and ASC for errors)
			order = SortOrder.valueOf(getOrder());
		}

		return "SELECT "+provideIndexHintAfterFirstKeyword(dbmsSupport)+provideFirstRowsHintAfterFirstKeyword(dbmsSupport)+ getListClause()+ getWhereClause(whereClause,false)+
		  " ORDER BY "+getDateField()+(" "+order.name()+" ")+provideTrailingFirstRowsHint(dbmsSupport);
	}

	@Override
	protected String createSelector() {
		return Misc.concatStrings(super.createSelector()," AND ",selectCondition);
	}


	protected String provideIndexHintAfterFirstKeyword(IDbmsSupport dbmsSupport) {
		if (useIndexHint && StringUtils.isNotEmpty(getIndexName())) {
			return dbmsSupport.provideIndexHintAfterFirstKeyword(getPrefix()+getTableName(), getPrefix()+getIndexName());
		}
		return "";
	}

	protected String provideFirstRowsHintAfterFirstKeyword(IDbmsSupport dbmsSupport) {
		if (useFirstRowsHint) {
			return dbmsSupport.provideFirstRowsHintAfterFirstKeyword(100);
		}
		return "";
	}
	protected String provideTrailingFirstRowsHint(IDbmsSupport dbmsSupport) {
		if (useFirstRowsHint) {
			return dbmsSupport.provideTrailingFirstRowsHint(100);
		}
		return "";
	}




	@Override
	public String getPhysicalDestinationName() {
		return super.getPhysicalDestinationName()+" in table ["+getTableName()+"]";
	}


	/**
	 * Sets the name of the table messages are stored in.
	 */
	@IbisDoc({"Name of the table messages are stored in", ""})
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public String getTableName() {
		return tableName;
	}


	@IbisDoc({"Name of the index, to be used in hints for query optimizer too (only for oracle)", ""})
	public void setIndexName(String string) {
		indexName = string;
	}
	public String getIndexName() {
		return indexName;
	}



}
